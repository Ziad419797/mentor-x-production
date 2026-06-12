"""
OCR service – EasyOCR for both Arabic and English/Serial.
"""

import cv2

from config.settings import OCR_LANGUAGES, USE_GPU
from services.image_processing import preprocess_image


# ── EasyOCR (Arabic + English) ───────────────────────────

_easyocr_reader = None
_easyocr_en_reader = None


def _get_easyocr_reader():
    """Lazily initialize the EasyOCR reader for Arabic."""
    global _easyocr_reader
    if _easyocr_reader is None:
        import easyocr
        _easyocr_reader = easyocr.Reader(OCR_LANGUAGES, gpu=USE_GPU)
    return _easyocr_reader


def _get_easyocr_en_reader():
    """Lazily initialize the EasyOCR reader for English only."""
    global _easyocr_en_reader
    if _easyocr_en_reader is None:
        import easyocr
        _easyocr_en_reader = easyocr.Reader(['en'], gpu=USE_GPU)
    return _easyocr_en_reader


def _extract_arabic(image, bbox) -> str:
    """Extract Arabic text using EasyOCR."""
    x1, y1, x2, y2 = bbox
    cropped = image[y1:y2, x1:x2]
    preprocessed = preprocess_image(cropped)

    results = _get_easyocr_reader().readtext(
        preprocessed,
        detail=0,
        paragraph=True
    )

    return " ".join(results).strip()


def _extract_english(image, bbox) -> str:
    """Extract English/Serial text using EasyOCR."""
    x1, y1, x2, y2 = bbox
    cropped = image[y1:y2, x1:x2]

    try:
        results = _get_easyocr_en_reader().readtext(cropped, detail=0)
        return " ".join(results).strip()
    except Exception as e:
        print("EasyOCR English parsing error:", e)
        return ""


# ── Public API (routes to the right engine) ──────────────

def _pad_bbox(bbox, image_shape, pad: int = 10):
    """
    Expand bbox by `pad` pixels on each side,
    clamped to image bounds.

    Prevents YOLO's tight bounding boxes
    from cutting off edge characters.
    """
    h, w = image_shape[:2]

    x1, y1, x2, y2 = bbox

    return [
        max(x1 - pad, 0),
        max(y1 - pad, 0),
        min(x2 + pad, w),
        min(y2 + pad, h),
    ]


def extract_text(image, bbox, lang: str = "ara") -> str:
    """
    Crop a region from image using bbox
    and route to the correct OCR engine.

    Args:
        image: Full card image (numpy BGR)
        bbox: [x1, y1, x2, y2]
        lang:
            ara -> EasyOCR
            eng -> PaddleOCR

    Returns:
        Recognized text string.
    """

    padded = _pad_bbox(bbox, image.shape)

    if lang == "eng":
        return _extract_english(image, padded)

    return _extract_arabic(image, padded)