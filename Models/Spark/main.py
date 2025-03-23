from fastapi import FastAPI, UploadFile, File, HTTPException
import uuid, os, subprocess
import json
from typing import List, Optional
from pathlib import Path
import base64


app = FastAPI()

@app.post("/upload-image/{user_id}")
async def upload_image(user_id: str, file: UploadFile = File(...)):
    # Validate file type
    if not file.content_type.startswith('image/'):
        raise HTTPException(status_code=400, detail="File must be an image")

    # Extract file extension from original filename
    file_extension = Path(file.filename).suffix.lower()
    if not file_extension:
        file_extension = '.jpg'  # default extension if none provided

    image_id = str(uuid.uuid4())
    local_path = f"tmp/{image_id}{file_extension}"
    
    # Ensure tmp directory exists
    os.makedirs("tmp", exist_ok=True)
    
    # Save uploaded file
    with open(local_path, "wb") as f:
        f.write(await file.read())

    try:
        # Copy files to container
        container_path = f"/app/tmp/{image_id}{file_extension}"
        subprocess.run(
            ["docker", "cp", local_path, f"clothgan-spark-1:{container_path}"],
            check=True
        )
        subprocess.run(
            ["docker", "cp", "classification_masking.py", "clothgan-spark-1:/app/"],
            check=True
        )

        # Run spark job in Docker container and capture output
        result = subprocess.run(
            ["docker", "exec", "clothgan-spark-1", 
             "spark-submit", 
             "--master", "local[*]",
             "/app/classification_masking.py",
             container_path, 
             image_id, 
             user_id],
            capture_output=True,
            text=True,
            check=True
        )
        
        # Parse the JSON output from Spark job
        spark_result = json.loads(result.stdout)
        
        # Clean up files in container and locally
        subprocess.run(
            ["docker", "exec", "-u", "root", "clothgan-spark-1", "rm", container_path],
            check=True
        )
        os.remove(local_path)
        
        return {
            "status": "completed",
            "image_id": image_id,
            "user_id": user_id,
            "classification": spark_result["classification"],
            "color": spark_result["color"]
        }
        
    except subprocess.CalledProcessError as e:
        # Clean up temporary files in case of error
        if os.path.exists(local_path):
            os.remove(local_path)
        raise HTTPException(status_code=500, detail=f"Processing failed: {e.stderr}")
    except json.JSONDecodeError:
        raise HTTPException(status_code=500, detail="Invalid response from Spark job")
    
@app.get("/images/{user_id}")
async def get_user_images(user_id: str):
    try:
        # Get all metadata for user from Hive
        query = f"SELECT image_id, category, color FROM image_metadata WHERE user_id = '{user_id}'"
        hive_result = subprocess.run(
            ["docker", "exec", "hive-container", "hive", "-e", query],
            capture_output=True,
            text=True,
            check=True
        )

        images = []
        for line in hive_result.stdout.strip().split('\n')[1:]:  # Skip header
            image_id, category, color = line.strip().split('\t')
            
            # Get image from HDFS
            hdfs_path = f"/user/images/{user_id}/{image_id}.jpg"
            temp_path = f"tmp/{image_id}.jpg"
            
            # Copy from HDFS to local through container
            subprocess.run(
                ["docker", "exec", "hadoop-namenode", 
                 "hdfs", "dfs", "-get", hdfs_path, "/tmp/"],
                check=True
            )
            subprocess.run(
                ["docker", "cp", 
                 f"hadoop-namenode:/tmp/{image_id}.jpg",
                 temp_path],
                check=True
            )

            # Read image as bytes and encode
            with open(temp_path, "rb") as img_file:
                image_bytes = img_file.read()
                image_base64 = base64.b64encode(image_bytes).decode()

            # Clean up temp file
            os.remove(temp_path)
            
            images.append({
                "image_id": image_id,
                "category": int(category),
                "color": color,
                "image_data": image_base64
            })

        return {
            "status": "success",
            "images": images
        }

    except subprocess.CalledProcessError as e:
        raise HTTPException(
            status_code=500, 
            detail=f"Failed to retrieve images: {e.stderr}"
        )