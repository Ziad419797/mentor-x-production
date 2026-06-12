from pydantic import BaseModel


class IDCardExtractionRequest(BaseModel):
    file: bytes

    class Config:
        schema_extra = {
            "example": {
                "file": "<binary image file>"
            }
        }
