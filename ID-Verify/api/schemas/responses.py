from pydantic import BaseModel, Field
from typing import Optional


class IDCardData(BaseModel):
    # وجه البطاقة
    first_name: str = Field("", example="محمد")
    last_name: str = Field("", example="أحمد علي")
    full_name: str = Field("", example="محمد أحمد علي")
    address: str = Field("", example="12 شارع التحرير القاهرة")
    national_id: str = Field("", example="29901011234567")
    serial: str = Field("", example="ABC123")
    # ظهر البطاقة
    job: str = Field("", example="مهندس")
    expiry: str = Field("", example="2030-01-01")
    issue: str = Field("", example="2020-01-01")


class DecodedNID(BaseModel):
    birth_date: str = Field("", example="1999-01-01")
    governorate: str = Field("", example="Cairo")
    gender: str = Field("", example="Male")


class ExtractionResponse(BaseModel):
    success: bool = Field(..., example=True)
    card_side: str = Field("front", example="front")  # 'front' | 'back' | 'unknown'
    message: Optional[str] = Field(None, example=None)
    data: IDCardData
    decoded_nid: DecodedNID


class HealthResponse(BaseModel):
    status: str = Field(..., example="UP")
    service: str = Field(..., example="Egyptian-ID-Card-AI")
    version: str = Field(..., example="1.0.0")
