"""
YOLO-based detection service for:
  1. Detecting the ID card in a photo.
  2. Detecting fields (firstName, lastName, address, serial, nid) inside the card.
  3. Detecting individual digits of the national ID number.
"""

import cv2
from ultralytics import YOLO

from config.settings import (
    BBOX_HEIGHT_SCALE,
    DETECTION_OUTPUT_PATH,
    FIELD_DETECTION_MODEL_PATH,
    ID_CARD_MODEL_PATH,
    NID_DETECTION_MODEL_PATH,
)

from services.image_processing import expand_bbox_height
from services.ocr_service import extract_text


# ──────────────────────────────────────────────
# ID Card Detection 
# ──────────────────────────────────────────────
def detect_id_card(image_path: str):
    """
    Detect and crop the ID card region from the full photograph.
    Prefers 'front-up' detection over 'back-up' when both are found.

    Returns:
        Tuple[np.ndarray, str]: (cropped image, card_side: 'front'|'back'|'unknown')
    """
    model = YOLO(ID_CARD_MODEL_PATH)
    results = model(image_path)
    image = cv2.imread(image_path)

    front_crop = None
    back_crop  = None
    other_crop = None

    for result in results:
        for box in result.boxes:
            x1, y1, x2, y2 = map(int, box.xyxy[0])
            crop = image[y1:y2, x1:x2]
            cls_name = result.names[int(box.cls[0])].lower()

            if "front" in cls_name:
                front_crop = crop
            elif "back" in cls_name:
                back_crop = crop
            else:
                other_crop = crop

    if front_crop is not None:
        return front_crop, "front"
    if back_crop is not None:
        return back_crop, "back"
    if other_crop is not None:
        return other_crop, "unknown"

    raise ValueError("No ID card detected in the image.")


# ──────────────────────────────────────────────
# National-ID Digit Detection
# ──────────────────────────────────────────────
def detect_national_id_digits(cropped_nid_image) -> str:
    """
    Run digit detection on the cropped national-ID region.

    Returns:
        A string of 14 digits representing the national ID.
    """
    model = YOLO(NID_DETECTION_MODEL_PATH)
    results = model(cropped_nid_image)

    detected_info = []
    for result in results:
        for box in result.boxes:
            cls = int(box.cls)
            x1, y1, x2, y2 = map(int, box.xyxy[0])
            detected_info.append((cls, x1))
            cv2.rectangle(cropped_nid_image, (x1, y1), (x2, y2), (0, 255, 0), 2)
            cv2.putText(
                cropped_nid_image, str(cls),
                (x1, y1 - 10),
                cv2.FONT_HERSHEY_SIMPLEX, 0.9, (36, 255, 12), 2,
            )

    # Sort left→right to form the correct digit sequence
    detected_info.sort(key=lambda x: x[1])
    return "".join(str(cls) for cls, _ in detected_info)


# ──────────────────────────────────────────────
# Field Detection & Extraction
# ──────────────────────────────────────────────
def detect_and_extract_fields(cropped_card_image) -> dict:
    """
    Detect the individual fields on the ID card (name, address, NID, …)
    and extract their text via OCR.

    Returns:
        A dict with keys: first_name, second_name, full_name, national_id,
        address, serial, card_side ('front'|'back'|'unknown').
    """
    model = YOLO(FIELD_DETECTION_MODEL_PATH)
    results = model(cropped_card_image)

    # ── Front-card fields ───────────────────
    first_name = ""
    second_name = ""
    nid = ""
    address = ""
    serial = ""

    # ── Back-card fields ────────────────────
    job    = ""
    expiry = ""
    issue  = ""

    for result in results:
        result.save(DETECTION_OUTPUT_PATH)

        for box in result.boxes:
            bbox = [int(c) for c in box.xyxy[0].tolist()]
            class_id   = int(box.cls[0].item())
            class_name = result.names[class_id]

            # ── Front classes ──
            if class_name == "firstName":
                first_name = extract_text(cropped_card_image, bbox, lang="ara")
            elif class_name == "lastName":
                second_name = extract_text(cropped_card_image, bbox, lang="ara")
            elif class_name == "serial":
                serial = extract_text(cropped_card_image, bbox, lang="eng")
            elif class_name == "address":
                address = extract_text(cropped_card_image, bbox, lang="ara")
            elif class_name == "nid":
                expanded = expand_bbox_height(
                    bbox, scale=BBOX_HEIGHT_SCALE, image_shape=cropped_card_image.shape
                )
                cropped_nid = cropped_card_image[expanded[1]:expanded[3], expanded[0]:expanded[2]]
                nid = detect_national_id_digits(cropped_nid)
            # ── Back classes ──
            elif class_name in ("job", "job_title"):
                job = extract_text(cropped_card_image, bbox, lang="ara")
            elif class_name in ("expiry", "expiry_date"):
                expiry = extract_text(cropped_card_image, bbox, lang="eng")
            elif class_name in ("issue", "issue_date"):
                issue = extract_text(cropped_card_image, bbox, lang="eng")
            # nid_back: الرقم القومي مطبوع على الظهر — نقرأه كـ nid fallback
            elif class_name == "nid_back" and not nid:
                expanded = expand_bbox_height(
                    bbox, scale=BBOX_HEIGHT_SCALE, image_shape=cropped_card_image.shape
                )
                cropped_nid = cropped_card_image[expanded[1]:expanded[3], expanded[0]:expanded[2]]
                nid = detect_national_id_digits(cropped_nid)

    # ── Determine card side ─────────────────
    has_front = bool(first_name or second_name or address)
    has_back  = bool(job or expiry or issue)

    if has_front:
        card_side = "front"
    elif has_back:
        card_side = "back"
    else:
        card_side = "unknown"

    return {
        "first_name":  first_name,
        "second_name": second_name,
        "full_name":   f"{first_name} {second_name}".strip(),
        "national_id": nid,
        "address":     address,
        "serial":      serial,
        "job":         job,
        "expiry":      expiry,
        "issue":       issue,
        "card_side":   card_side,
    }
