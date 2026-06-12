import logging
import os

from fastapi import APIRouter, File, HTTPException, UploadFile, status

from api.schemas.responses import DecodedNID, ExtractionResponse, HealthResponse
from api.utils.file_utils import save_upload_to_temp, validate_image_file
from services.pipeline import process_id_card

logger = logging.getLogger("egypt_id_api.router")
router = APIRouter(tags=["ID Card"])


@router.get("/health", response_model=HealthResponse)
def health_check():
    """Health endpoint for API status monitoring."""
    logger.info("Health check requested")
    return HealthResponse(status="UP", service="Egyptian-ID-Card-AI", version="1.0.0")


@router.post(
    "/extract-id",
    response_model=ExtractionResponse,
    status_code=status.HTTP_200_OK,
    summary="Extract structured ID data from an uploaded image",
)
async def extract_id_card(file: UploadFile = File(...)):
    """Upload an Egyptian National ID image and return extracted structured data."""
    filename = file.filename or ""
    logger.info("Received file upload: %s", filename)

    if not validate_image_file(filename):
        allowed = ", ".join(sorted(["jpg", "jpeg", "png"]))
        logger.warning("Rejected unsupported file type: %s", filename)
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Unsupported file type. Allowed extensions: {allowed}",
        )

    temp_file_path = None
    try:
        temp_file_path = save_upload_to_temp(file)
        logger.info("Saved temporary upload to %s", temp_file_path)

        pipeline_result = process_id_card(temp_file_path)
        extracted  = pipeline_result.extracted
        decoded    = pipeline_result.decoded
        card_side  = pipeline_result.card_side   # 'front' | 'back' | 'unknown'
        is_front   = card_side == "front"

        # لو ظهر بطاقة → success=False + رسالة واضحة للمعلم
        success = is_front
        message = None if is_front else "تم رفع ظهر البطاقة — يرجى رفع وجه البطاقة الذي يحتوي على الاسم والرقم القومي"

        response_payload = ExtractionResponse(
            success=success,
            card_side=card_side,
            message=message,
            data={
                "first_name":  extracted.get("first_name", ""),
                "last_name":   extracted.get("second_name", ""),
                "full_name":   extracted.get("full_name", ""),
                "address":     extracted.get("address", ""),
                "national_id": extracted.get("national_id", ""),
                "serial":      extracted.get("serial", ""),
                "job":         extracted.get("job", ""),
                "expiry":      extracted.get("expiry", ""),
                "issue":       extracted.get("issue", ""),
            },
            decoded_nid=DecodedNID(
                birth_date=decoded.get("birth_date", "N/A"),
                governorate=decoded.get("governorate", "N/A"),
                gender=decoded.get("gender", "N/A"),
            ),
        )
        logger.info("Extraction completed for %s — card_side=%s success=%s", filename, card_side, success)
        return response_payload

    except ValueError as exc:
        logger.warning("Pipeline error for %s: %s", filename, exc)
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            detail=str(exc),
        )
    except Exception as exc:
        logger.exception("Unexpected error during extraction for %s", filename)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Internal server error",
        )
    finally:
        if temp_file_path and os.path.exists(temp_file_path):
            try:
                os.remove(temp_file_path)
                logger.info("Removed temporary file %s", temp_file_path)
            except Exception:
                logger.warning("Failed to remove temporary file %s", temp_file_path)
