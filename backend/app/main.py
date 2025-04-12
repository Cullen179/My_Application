from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.core.config import settings
from app.routers import auth, protected, deepfake
from app.services.deepfake_detector import deepfake_detector

app = FastAPI(
    title="FastAPI Firebase Authentication with Deepfake Detection",
    description="FastAPI project with Firebase authentication and Deepfake detection",
    version="0.1.0",
)

# CORS middleware configuration
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # Allows all origins
    allow_credentials=True,
    allow_methods=["*"],  # Allows all methods
    allow_headers=["*"],  # Allows all headers
)

# Health check endpoint (kept as requested)
@app.get("/health")
async def health_check():
    """Health check endpoint to verify API is running."""
    return {"status": "healthy"}

# Include routers with API prefix
app.include_router(auth.router, prefix=settings.API_V1_STR)
app.include_router(protected.router, prefix=settings.API_V1_STR)
app.include_router(deepfake.router, prefix=settings.API_V1_STR)

@app.on_event("startup")
async def startup_event():
    """Initialize the deepfake detection model on startup."""
    try:
        # Start loading the model in the background
        deepfake_detector.initialize()
    except Exception as e:
        print(f"Warning: Failed to load deepfake model on startup: {e}")
        print("The model will be loaded on first request.")

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("app.main:app", host="0.0.0.0", port=8000, reload=True) 