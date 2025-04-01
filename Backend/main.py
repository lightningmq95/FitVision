from fastapi import FastAPI, UploadFile, File, HTTPException
import uuid, os, subprocess
import json
import tensorflow as tf
import numpy as np
from tensorflow.keras.preprocessing import image
from typing import List, Optional
from pathlib import Path
import base64
import happybase


app = FastAPI()
# Load the saved model
model = tf.keras.models.load_model('model.keras')
CLASS_NAMES = ['dress', 'pants', 'shirts']  # Replace with your actual class names


@app.post("/upload-image/{user_id}")
async def upload_images(user_id: str, files: List[UploadFile] = File(...)):
    # Validate file type
    file = files[0]
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
        # Create user directory in HDFS if it doesn't exist
        subprocess.run(
            ["docker", "exec", "namenode", 
             "hdfs", "dfs", "-mkdir", "-p", f"/user/images/{user_id}"],
            check=True
        )

        # Copy file to HDFS container's tmp directory
        subprocess.run(
            ["docker", "cp", local_path, f"namenode:/tmp/{image_id}{file_extension}"],
            check=True
        )

        # Move file from container's tmp to HDFS
        hdfs_path = f"/user/images/{user_id}/{image_id}{file_extension}"
        subprocess.run(
            ["docker", "exec", "namenode",
             "hdfs", "dfs", "-put", f"/tmp/{image_id}{file_extension}", hdfs_path],
            check=True
        )
        # Copy files to container
        # container_path = f"/app/tmp/{image_id}{file_extension}"
        # subprocess.run(
        #     ["docker", "cp", local_path, f"clothgan-spark-1:{container_path}"],
        #     check=True
        # )
        # subprocess.run(
        #     ["docker", "cp", "classification_masking.py", "clothgan-spark-1:/app/"],
        #     check=True
        # )

        # # Run spark job in Docker container and capture output
        # result = subprocess.run(
        #     ["docker", "exec", "clothgan-spark-1", 
        #      "spark-submit", 
        #      "--master", "local[*]",
        #      "/app/classification_masking.py",
        #      container_path, 
        #      image_id, 
        #      user_id],
        #     capture_output=True,
        #     text=True,
        #     check=True
        # )
        # Classify the image
        img = image.load_img(local_path, target_size=(28, 28), color_mode='grayscale')
        img_array = image.img_to_array(img)
        img_array = np.expand_dims(img_array, axis=0)
        img_array = img_array / 255.0

        # # Make prediction
        predictions = model.predict(img_array)
        predicted_class = np.argmax(predictions[0])
        classification = CLASS_NAMES[predicted_class]
        
        # Parse the JSON output from Spark job
        # spark_result = {"classification": predicted_class, "color": "red"}
        spark_result = {"classification": classification, "color": "red"}
        
        # Clean up files in container and locally
        # subprocess.run(
        #     ["docker", "exec", "-u", "root", "clothgan-spark-1", "rm", container_path],
        #     check=True
        # )
        # os.remove(local_path)
        
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

@app.put("/images/{user_id}/{image_id}/category")
async def update_image_category(
    user_id: str, 
    image_id: str, 
    image_name: str,
    category: int
):
    try:
        # Connect to HBase
        connection = happybase.Connection('localhost')
        # Check if table exists
        table_name = 'image_metadata'
        if table_name.encode('utf-8') not in connection.tables():
            # Define column families
            families = {
                'metadata': dict(max_versions=3)  # Keep last 3 versions
            }
            connection.create_table(table_name, families)
            logger.info(f"Created table: {table_name}")
            
        table = connection.table(table_name)
        
        # Row key format: user_id:image_id
        row_key = f"{user_id}:{image_id}".encode('utf-8')
        
        # Update category in HBase
        table.put(row_key, {
            b'metadata:category': str(category).encode('utf-8'),
            b'metadata:image_name': str(image_name).encode('utf-8')
        })
        
        # Close HBase connection
        connection.close()
        
        return {
            "status": "success",
            "user_id": user_id,
            "image_id": image_id,
            "category": category
        }
        
    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail=f"Failed to update category: {str(e)}"
        )


import logging
from fastapi.responses import JSONResponse

# Set up logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

@app.get("/images/{user_id}")
async def get_user_images(user_id: str):
    connection = None
    temp_files = []
    
    try:
        logger.info(f"Attempting to get images for user: {user_id}")
        
        # Connect to HBase
        connection = happybase.Connection('localhost', timeout=20000)
        table = connection.table('image_metadata')
        
        # Scan for rows with user_id prefix
        row_prefix = f"{user_id}:".encode('utf-8')
        images = []
        
        logger.info("Scanning HBase for images")
        scan_count = 0
        
        for key, data in table.scan(row_prefix=row_prefix):
            try:
                scan_count += 1
                image_id = key.decode('utf-8').split(':')[1]
                logger.info(f"Processing image: {image_id}")
                
                # Get metadata from HBase
                category = int(data[b'metadata:category'].decode('utf-8'))
                image_name = data.get(b'metadata:image_name', b'').decode('utf-8') or 'Untitled'                # color = data[b'metadata:color'].decode('utf-8')
                
                # Get image from HDFS
                hdfs_path = f"/user/images/{user_id}/{image_id}.jpg"
                temp_path = f"tmp/{image_id}.jpg"
                temp_files.append(temp_path)
                
                # Verify HDFS file exists
                check_file = subprocess.run(
                    ["docker", "exec", "namenode", 
                     "hdfs", "dfs", "-test", "-e", hdfs_path],
                    capture_output=True
                )
                
                if check_file.returncode != 0:
                    logger.error(f"Image not found in HDFS: {hdfs_path}")
                    continue
                
                # Copy from HDFS
                subprocess.run(
                    ["docker", "exec", "namenode", 
                     "hdfs", "dfs", "-get", "-f", hdfs_path, "/tmp/"],
                    check=True
                )
                
                subprocess.run(
                    ["docker", "cp", 
                     f"namenode:/tmp/{image_id}.jpg",
                     temp_path],
                    check=True
                )

                # Read and encode image
                with open(temp_path, "rb") as img_file:
                    image_bytes = img_file.read()
                    image_base64 = base64.b64encode(image_bytes).decode()

                images.append({
                    "image_id": image_id,
                    "image_name": image_name,
                    "category": category,
                    # "color": color,
                    "image_data": image_base64
                })
                
            except Exception as img_error:
                logger.error(f"Error processing image {image_id}: {str(img_error)}")
                continue

        logger.info(f"Processed {scan_count} images, returning {len(images)} valid results")
        
        # return JSONResponse(content={
        #     "status": "success",
        #     "count": len(images),
        #     "images": images
        # })
        return images

    except Exception as e:
        logger.error(f"Error in get_user_images: {str(e)}")
        raise HTTPException(
            status_code=500,
            detail=f"Failed to retrieve images: {str(e)}"
        )
        
    finally:
        # Clean up
        if connection:
            connection.close()
        for temp_file in temp_files:
            if os.path.exists(temp_file):
                os.remove(temp_file)
                logger.info(f"Cleaned up temp file: {temp_file}")


