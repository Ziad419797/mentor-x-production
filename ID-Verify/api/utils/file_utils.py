import os
import tempfile

from fastapi import UploadFile

ALLOWED_IMAGE_EXTENSIONS = {"jpg", "jpeg", "png"}


def get_file_extension(filename: str) -> str:
    return os.path.splitext(filename)[1].lower().strip(".")


def validate_image_file(filename: str) -> bool:
    if not filename:
        return False
    return get_file_extension(filename) in ALLOWED_IMAGE_EXTENSIONS


def save_upload_to_temp(upload_file: UploadFile) -> str:
    extension = get_file_extension(upload_file.filename or "")
    if extension not in ALLOWED_IMAGE_EXTENSIONS:
        raise ValueError("Unsupported image format")

    suffix = f".{extension}"
    temp_file = tempfile.NamedTemporaryFile(delete=False, suffix=suffix)
    try:
        temp_file.write(upload_file.file.read())
        temp_file.flush()
        return temp_file.name
    finally:
        temp_file.close()
