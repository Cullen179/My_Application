from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from contextlib import asynccontextmanager
import uvicorn
from app.core.config import settings
from app.routers import auth, protected, deepfake
from app.services.deepfake_detector import deepfake_detector

@asynccontextmanager
async def lifespan(app: FastAPI):
    """Lifespan context manager for startup and shutdown events."""
    try:
        # Start loading the model on startup
        deepfake_detector.initialize()
    except Exception as e:
        print(f"Warning: Failed to load deepfake model on startup: {e}")
        print("The model will be loaded on first request.")
    yield
    # Cleanup code can go here (if needed)

app = FastAPI(
    title="FastAPI Firebase Authentication with Deepfake Detection",
    description="FastAPI project with Firebase authentication and Deepfake detection",
    version="0.1.0",
    lifespan=lifespan,
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

if __name__ == "__main__":
    uvicorn.run("app.main:app", host="0.0.0.0", port=8000, reload=True) 