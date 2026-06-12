import os
os.environ["FLAGS_use_mkldnn"] = "0"
os.environ["PADDLE_PDX_DISABLE_ONEDNN"] = "1"
os.environ["FLAGS_enable_pir_in_executor"] = "0"

import logging

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from api.routers.id_card import router as id_card_router

logger = logging.getLogger("egypt_id_api")
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)s %(name)s %(message)s",
)
logger.info("Initializing Egyptian National ID Card AI API")

app = FastAPI(
    title="Egyptian National ID Card AI API",
    version="1.0.0",
    description="AI service for extracting Egyptian National ID information",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["GET", "POST", "OPTIONS"],
    allow_headers=["*"],
)

app.include_router(id_card_router, prefix="/api/v1")
