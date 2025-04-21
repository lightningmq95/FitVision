from fastapi import FastAPI, UploadFile, File, Form, HTTPException
import uuid, os, subprocess
from ultralytics import YOLO
import cv2
import json
import tensorflow as tf
import numpy as np
from tensorflow.keras.preprocessing import image
from typing import List, Optional
from pathlib import Path
import base64


app = FastAPI()
# Load the saved model
model = tf.keras.models.load_model('model.keras')
# Load a pretrained YOLO model
yolo_model = YOLO("yolo11n.pt")
vton_model = yolo_model
CLASS_NAMES = ['dress', 'pants', 'shirts']  # Replace with your actual class names

@app.post("/upload-image/{userId}")
async def upload_image(
    userId: str,
    image_name: str = Form(...),
    file: UploadFile = File(...)
):
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
        # Create user directory in HDFS if it doesn't exist
        subprocess.run(
            ["docker", "exec", "namenode", 
             "hdfs", "dfs", "-mkdir", "-p", f"/user/images/{userId}"],
            check=True
        )

        # Copy file to HDFS container's tmp directory
        subprocess.run(
            ["docker", "cp", local_path, f"namenode:/tmp/{image_id}{file_extension}"],
            check=True
        )

        # Move file from container's tmp to HDFS
        hdfs_path = f"/user/images/{userId}/{image_id}{file_extension}"
        subprocess.run(
            ["docker", "exec", "namenode",
             "hdfs", "dfs", "-put", f"/tmp/{image_id}{file_extension}", hdfs_path],
            check=True
        )

        # Classify the image
        img = image.load_img(local_path, target_size=(28, 28), color_mode='grayscale')
        img_array = image.img_to_array(img)
        img_array = np.expand_dims(img_array, axis=0)
        img_array = img_array / 255.0

        # Make prediction
        predictions = model.predict(img_array)
        predicted_class = np.argmax(predictions[0])
        classification = CLASS_NAMES[predicted_class]

        # Connect to Cassandra and save metadata
        auth_provider = PlainTextAuthProvider(username='cassandra', password='cassandra')
        cluster = Cluster(['localhost'], port=9042, auth_provider=auth_provider)
        session = cluster.connect('fitvision')

        # Insert into Cassandra
        query = """
        INSERT INTO image_metadata (
            user_id,
            image_id,
            image_extension,
            image_name,
            classification
        ) VALUES (%s, %s, %s, %s, %s)
        """
        
        session.execute(
            query, 
            (
                userId,
                image_id,
                file_extension,
                image_name,
                classification
            )
        )

        # Close Cassandra connection
        cluster.shutdown()

        # Clean up local temporary file
        os.remove(local_path)
        
        return {
            "status": "completed",
            "image_id": image_id,
            "user_id": userId,
            "image_name": image_name,
            "category": classification,
            "color": "red",
            "image_extension": file_extension
        }
        
    except subprocess.CalledProcessError as e:
        if os.path.exists(local_path):
            os.remove(local_path)
        raise HTTPException(status_code=500, detail=f"Processing failed: {e.stderr}")
    except Exception as e:
        if os.path.exists(local_path):
            os.remove(local_path)
        raise HTTPException(status_code=500, detail=f"Error: {str(e)}")
    

# @app.post("/upload-image/{user_id}")
# async def upload_images(user_id: str, files: List[UploadFile] = File(...)):
#     # Validate file type
#     file = files[0]
#     if not file.content_type.startswith('image/'):
#         raise HTTPException(status_code=400, detail="File must be an image")

#     # Extract file extension from original filename
#     file_extension = Path(file.filename).suffix.lower()
#     if not file_extension:
#         file_extension = '.jpg'  # default extension if none provided

#     image_id = str(uuid.uuid4())
#     local_path = f"tmp/{image_id}{file_extension}"
    
#     # Ensure tmp directory exists
#     os.makedirs("tmp", exist_ok=True)
    
#     # Save uploaded file
#     with open(local_path, "wb") as f:
#         f.write(await file.read())

#     try:
#         # Create user directory in HDFS if it doesn't exist
#         subprocess.run(
#             ["docker", "exec", "namenode", 
#              "hdfs", "dfs", "-mkdir", "-p", f"/user/images/{user_id}"],
#             check=True
#         )

#         # Copy file to HDFS container's tmp directory
#         subprocess.run(
#             ["docker", "cp", local_path, f"namenode:/tmp/{image_id}{file_extension}"],
#             check=True
#         )

#         # Move file from container's tmp to HDFS
#         hdfs_path = f"/user/images/{user_id}/{image_id}{file_extension}"
#         subprocess.run(
#             ["docker", "exec", "namenode",
#              "hdfs", "dfs", "-put", f"/tmp/{image_id}{file_extension}", hdfs_path],
#             check=True
#         )
#         # Copy files to container
#         container_path = f"/app/tmp/{image_id}{file_extension}"
#         subprocess.run(
#             ["docker", "cp", local_path, f"clothgan-spark-1:{container_path}"],
#             check=True
#         )
#         # subprocess.run(
#         #     ["docker", "cp", "classification_masking.py", "clothgan-spark-1:/app/"],
#         #     check=True
#         # )

#         # # Run spark job in Docker container and capture output
#         # result = subprocess.run(
#         #     ["docker", "exec", "clothgan-spark-1", 
#         #      "spark-submit", 
#         #      "--master", "local[*]",
#         #      "/app/classification_masking.py",
#         #      container_path, 
#         #      image_id, 
#         #      user_id],
#         #     capture_output=True,
#         #     text=True,
#         #     check=True
#         # )
#         # Classify the image
#         img = image.load_img(local_path, target_size=(28, 28), color_mode='grayscale')
#         img_array = image.img_to_array(img)
#         img_array = np.expand_dims(img_array, axis=0)
#         img_array = img_array / 255.0

#         # # Make prediction
#         predictions = model.predict(img_array)
#         predicted_class = np.argmax(predictions[0])
#         classification = CLASS_NAMES[predicted_class]
        
#         # Parse the JSON output from Spark job
#         # spark_result = {"classification": predicted_class, "color": "red"}
#         spark_result = {"classification": classification, "color": "red"}
        
#         # Clean up files in container and locally
#         # subprocess.run(
#         #     ["docker", "exec", "-u", "root", "clothgan-spark-1", "rm", container_path],
#         #     check=True
#         # )
#         # os.remove(local_path)
        
#         return {
#             "status": "completed",
#             "image_id": image_id,
#             "user_id": user_id,
#             "classification": spark_result["classification"],
#             "color": spark_result["color"]
#         }
        
#     except subprocess.CalledProcessError as e:
#         # Clean up temporary files in case of error
#         if os.path.exists(local_path):
#             os.remove(local_path)
#         raise HTTPException(status_code=500, detail=f"Processing failed: {e.stderr}")
#     except json.JSONDecodeError:
#         raise HTTPException(status_code=500, detail="Invalid response from Spark job")
from cassandra.cluster import Cluster
from cassandra.auth import PlainTextAuthProvider

# @app.put("/images/{user_id}/{image_id}/category")
# async def update_image_category(
#     user_id: str, 
#     image_id: str, 
#     image_name: str,
#     category: int
# ):
#     try:
#         # Connect to HBase
#         connection = happybase.Connection('localhost')
#         # Check if table exists
#         table_name = 'image_metadata'
#         if table_name.encode('utf-8') not in connection.tables():
#             # Define column families
#             families = {
#                 'metadata': dict(max_versions=3)  # Keep last 3 versions
#             }
#             connection.create_table(table_name, families)
#             logger.info(f"Created table: {table_name}")
            
#         table = connection.table(table_name)
        
#         # Row key format: user_id:image_id
#         row_key = f"{user_id}:{image_id}".encode('utf-8')
        
#         # Update category in HBase
#         table.put(row_key, {
#             b'metadata:category': str(category).encode('utf-8'),
#             b'metadata:image_name': str(image_name).encode('utf-8')
#         })
        
#         # Close HBase connection
#         connection.close()
        
#         return {
#             "status": "success",
#             "user_id": user_id,
#             "image_id": image_id,
#             "category": category
#         }
        
#     except Exception as e:
#         raise HTTPException(
#             status_code=500,
#             detail=f"Failed to update category: {str(e)}"
#         )

@app.put("/images/{user_id}/{image_id}/category")
async def update_image_category(
    user_id: str, 
    image_id: str, 
    image_extension: str,
    image_name: str,
    category: int
):
    try:
        # Connect to Cassandra cluster with authentication
        auth_provider = PlainTextAuthProvider(username='cassandra', password='cassandra')
        cluster = Cluster(
            ['localhost'], 
            port=9042,
            auth_provider=auth_provider
        )
        session = cluster.connect('fitvision')  # using the keyspace we created

        # Prepare the insert statement
        query = """
        INSERT INTO image_metadata (
            user_id,
            image_id,
            image_extension,
            image_name,
            classification
        ) VALUES (%s, %s, %s, %s, %s)
        """
        
        # Convert category number to classification string
        CLASS_NAMES = ['dress', 'pants', 'shirts']
        classification = CLASS_NAMES[category]
        
        # Execute the insert
        session.execute(
            query, 
            (
                user_id,
                image_id,
                image_extension,
                image_name,
                classification,
            )
        )
        
        # Close Cassandra connection
        cluster.shutdown()
        
        return {
            "status": "success",
            "user_id": user_id,
            "image_id": image_id,
            "category": category,
            "classification": classification
        }
        
    except Exception as e:
        logger.error(f"Cassandra error: {str(e)}")
        raise HTTPException(
            status_code=500,
            detail=f"Failed to update category: {str(e)}"
        )

import logging
from fastapi.responses import JSONResponse

# Set up logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# @app.get("/images/{user_id}")
# async def get_user_images(user_id: str):
#     connection = None
#     temp_files = []
    
#     try:
#         logger.info(f"Attempting to get images for user: {user_id}")
        
#         # Connect to HBase
#         connection = happybase.Connection('localhost', timeout=20000)
#         table = connection.table('image_metadata')
        
#         # Scan for rows with user_id prefix
#         row_prefix = f"{user_id}:".encode('utf-8')
#         images = []
        
#         logger.info("Scanning HBase for images")
#         scan_count = 0
        
#         for key, data in table.scan(row_prefix=row_prefix):
#             try:
#                 scan_count += 1
#                 image_id = key.decode('utf-8').split(':')[1]
#                 logger.info(f"Processing image: {image_id}")
                
#                 # Get metadata from HBase
#                 category = int(data[b'metadata:category'].decode('utf-8'))
#                 image_name = data.get(b'metadata:image_name', b'').decode('utf-8') or 'Untitled'                # color = data[b'metadata:color'].decode('utf-8')
                
#                 # Get image from HDFS
#                 hdfs_path = f"/user/images/{user_id}/{image_id}.jpg"
#                 temp_path = f"tmp/{image_id}.jpg"
#                 temp_files.append(temp_path)
                
#                 # Verify HDFS file exists
#                 check_file = subprocess.run(
#                     ["docker", "exec", "namenode", 
#                      "hdfs", "dfs", "-test", "-e", hdfs_path],
#                     capture_output=True
#                 )
                
#                 if check_file.returncode != 0:
#                     logger.error(f"Image not found in HDFS: {hdfs_path}")
#                     continue
                
#                 # Copy from HDFS
#                 subprocess.run(
#                     ["docker", "exec", "namenode", 
#                      "hdfs", "dfs", "-get", "-f", hdfs_path, "/tmp/"],
#                     check=True
#                 )
                
#                 subprocess.run(
#                     ["docker", "cp", 
#                      f"namenode:/tmp/{image_id}.jpg",
#                      temp_path],
#                     check=True
#                 )

#                 # Read and encode image
#                 with open(temp_path, "rb") as img_file:
#                     image_bytes = img_file.read()
#                     image_base64 = base64.b64encode(image_bytes).decode()

#                 images.append({
#                     "image_id": image_id,
#                     "image_name": image_name,
#                     "category": category,
#                     # "color": color,
#                     "image_data": image_base64
#                 })
                
#             except Exception as img_error:
#                 logger.error(f"Error processing image {image_id}: {str(img_error)}")
#                 continue

#         logger.info(f"Processed {scan_count} images, returning {len(images)} valid results")
        
#         # return JSONResponse(content={
#         #     "status": "success",
#         #     "count": len(images),
#         #     "images": images
#         # })
#         return images

#     except Exception as e:
#         logger.error(f"Error in get_user_images: {str(e)}")
#         raise HTTPException(
#             status_code=500,
#             detail=f"Failed to retrieve images: {str(e)}"
#         )
        
#     finally:
#         # Clean up
#         if connection:
#             connection.close()
#         for temp_file in temp_files:
#             if os.path.exists(temp_file):
#                 os.remove(temp_file)
#                 logger.info(f"Cleaned up temp file: {temp_file}")


@app.get("/images/{user_id}")
async def get_user_images(user_id: str):
    temp_files = []
    
    try:
        logger.info(f"Attempting to get images for user: {user_id}")
        
        # Connect to Cassandra cluster with authentication
        auth_provider = PlainTextAuthProvider(username='cassandra', password='cassandra')
        cluster = Cluster(
            ['localhost'], 
            port=9042,
            auth_provider=auth_provider
        )
        session = cluster.connect('fitvision')
        
        # Get all images for user
        query = """
        SELECT image_id, image_name, classification, image_extension
        FROM image_metadata
        WHERE user_id = %s
        """
        rows = session.execute(query, [user_id])
        
        images = []
        scan_count = 0
        
        for row in rows:
            try:
                scan_count += 1
                logger.info(f"Processing image: {row.image_id}")
                
                # Convert classification back to category number
                CLASS_NAMES = ['dress', 'pants', 'shirts']
                category = CLASS_NAMES.index(row.classification)
                
                # Get image from HDFS
                hdfs_path = f"/user/images/{user_id}/{row.image_id}{row.image_extension}"
                temp_path = f"tmp/{row.image_id}{row.image_extension}"
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
                     f"namenode:/tmp/{row.image_id}{row.image_extension}",
                     temp_path],
                    check=True
                )

                # Read and encode image
                with open(temp_path, "rb") as img_file:
                    image_bytes = img_file.read()
                    image_base64 = base64.b64encode(image_bytes).decode()

                images.append({
                    "image_id": row.image_id,
                    "image_name": row.image_name,
                    "category": category,
                    "classification": row.classification,
                    "image_extension": row.image_extension,
                    "image_data": image_base64
                })
                
            except Exception as img_error:
                logger.error(f"Error processing image {row.image_id}: {str(img_error)}")
                continue

        logger.info(f"Processed {scan_count} images, returning {len(images)} valid results")
        
        # Close Cassandra connection
        cluster.shutdown()
        return images

    except Exception as e:
        logger.error(f"Error in get_user_images: {str(e)}")
        raise HTTPException(
            status_code=500,
            detail=f"Failed to retrieve images: {str(e)}"
        )
        
    finally:
        # Clean up temp files
        for temp_file in temp_files:
            if os.path.exists(temp_file):
                os.remove(temp_file)
                logger.info(f"Cleaned up temp file: {temp_file}")


@app.get("/combos/{user_id}")
async def get_user_combos(user_id: str):
    try:
        # Connect to Cassandra cluster with authentication
        auth_provider = PlainTextAuthProvider(username='cassandra', password='cassandra')
        cluster = Cluster(
            ['localhost'], 
            port=9042,
            auth_provider=auth_provider
        )
        session = cluster.connect('fitvision')

        # Get all combo names for user
        query = """
        SELECT combo_name FROM clothing_combos
        WHERE user_id = %s
        """
        rows = session.execute(query, [user_id])
        
        combo_names = [row.combo_name for row in rows]

        # Close Cassandra connection
        cluster.shutdown()

        return combo_names

    except Exception as e:
        logger.error(f"Cassandra error: {str(e)}")
        raise HTTPException(
            status_code=500,
            detail=f"Failed to retrieve combos: {str(e)}"
        )
    


@app.put("/combos/{user_id}/{combo_name}")
async def update_clothing_combo(
    user_id: str,
    combo_name: str,
    clothing_id: str,
    is_top: bool  # True for top (shirt/dress), False for bottom (pants)
):
    try:
        # Connect to Cassandra cluster with authentication
        auth_provider = PlainTextAuthProvider(username='cassandra', password='cassandra')
        cluster = Cluster(
            ['localhost'], 
            port=9042,
            auth_provider=auth_provider
        )
        session = cluster.connect('fitvision')

        # First, verify the clothing item exists and get its classification
        verify_query = """
        SELECT classification FROM image_metadata
        WHERE user_id = %s AND image_id = %s
        """
        clothing_row = session.execute(verify_query, [user_id, clothing_id]).one()
        if not clothing_row:
            raise HTTPException(status_code=404, detail="Clothing item not found")

        classification = clothing_row.classification
        if is_top and classification not in ['dress', 'shirts']:
            raise HTTPException(status_code=400, detail="Selected item is not a top wear")
        elif not is_top and classification != 'pants':
            raise HTTPException(status_code=400, detail="Selected item is not a bottom wear")

        # Get existing combo if any
        get_query = """
        SELECT * FROM clothing_combos
        WHERE user_id = %s AND combo_name = %s
        """
        existing_combo = session.execute(get_query, [user_id, combo_name]).one()

        if existing_combo:
            # Update existing combo
            update_query = """
            UPDATE clothing_combos
            SET {} = %s
            WHERE user_id = %s AND combo_name = %s
            """.format('top_clothing_id' if is_top else 'bottom_clothing_id')
            session.execute(update_query, [clothing_id, user_id, combo_name])
        else:
            # Create new combo
            insert_query = """
            INSERT INTO clothing_combos (
                user_id,
                combo_name,
                top_clothing_id,
                bottom_clothing_id
            ) VALUES (%s, %s, %s, %s)
            """
            session.execute(
                insert_query,
                [
                    user_id,
                    combo_name,
                    clothing_id if is_top else None,
                    None if is_top else clothing_id
                ]
            )

        # Get updated combo
        result = session.execute(get_query, [user_id, combo_name]).one()
        
        # Close Cassandra connection
        cluster.shutdown()

        return {
            "status": "success",
            "user_id": user_id,
            "combo_name": combo_name,
            "top_clothing_id": result.top_clothing_id,
            "bottom_clothing_id": result.bottom_clothing_id
        }

    except Exception as e:
        logger.error(f"Cassandra error: {str(e)}")
        raise HTTPException(
            status_code=500,
            detail=f"Failed to update combo: {str(e)}"
        )

# @app.get("/combos/{user_id}")
# async def get_user_combos(user_id: str):
#     try:
#         # Connect to Cassandra cluster with authentication
#         auth_provider = PlainTextAuthProvider(username='cassandra', password='cassandra')
#         cluster = Cluster(
#             ['localhost'], 
#             port=9042,
#             auth_provider=auth_provider
#         )
#         session = cluster.connect('fitvision')

#         # Get all combos for user
#         query = """
#         SELECT combo_name, top_clothing_id, bottom_clothing_id
#         FROM clothing_combos
#         WHERE user_id = %s
#         """
#         rows = session.execute(query, [user_id])
        
#         # Filter for complete combos in Python
#         combo_names = [
#             row.combo_name 
#             for row in rows 
#             if row.top_clothing_id is not None and row.bottom_clothing_id is not None
#         ]

#         # Close Cassandra connection
#         cluster.shutdown()

#         return combo_names

#     except Exception as e:
#         logger.error(f"Cassandra error: {str(e)}")
#         raise HTTPException(
#             status_code=500,
#             detail=f"Failed to retrieve combos: {str(e)}"
#         )

@app.get("/combos/{user_id}/{combo_name}")
async def get_combo_details(user_id: str, combo_name: str):
    temp_files = []
    try:
        # Connect to Cassandra
        auth_provider = PlainTextAuthProvider(username='cassandra', password='cassandra')
        cluster = Cluster(['localhost'], port=9042, auth_provider=auth_provider)
        session = cluster.connect('fitvision')

        # Get combo clothing IDs
        combo_query = """
        SELECT top_clothing_id, bottom_clothing_id FROM clothing_combos
        WHERE user_id = %s AND combo_name = %s
        """
        combo = session.execute(combo_query, [user_id, combo_name]).one()
        if not combo:
            raise HTTPException(status_code=404, detail="Combo not found")

        result = {"combo_name": combo_name, "top": None, "bottom": None}

        # Get clothing details for top and bottom
        if combo.top_clothing_id or combo.bottom_clothing_id:
            clothing_query = """
            SELECT image_id, image_name, classification, image_extension
            FROM image_metadata
            WHERE user_id = %s AND image_id = %s
            """

            for clothing_type, clothing_id in [("top", combo.top_clothing_id), ("bottom", combo.bottom_clothing_id)]:
                if clothing_id:
                    # Get metadata
                    clothing = session.execute(clothing_query, [user_id, clothing_id]).one()
                    
                    # Get image from HDFS
                    hdfs_path = f"/user/images/{user_id}/{clothing_id}{clothing.image_extension}"
                    temp_path = f"tmp/{clothing_id}{clothing.image_extension}"
                    temp_files.append(temp_path)

                    # Copy from HDFS
                    subprocess.run(
                        ["docker", "exec", "namenode", 
                         "hdfs", "dfs", "-get", "-f", hdfs_path, "/tmp/"],
                        check=True
                    )
                    subprocess.run(
                        ["docker", "cp", 
                         f"namenode:/tmp/{clothing_id}{clothing.image_extension}",
                         temp_path],
                        check=True
                    )

                    # Read and encode image
                    with open(temp_path, "rb") as img_file:
                        image_base64 = base64.b64encode(img_file.read()).decode()

                    result[clothing_type] = {
                        "image_id": clothing_id,
                        "image_name": clothing.image_name,
                        "classification": clothing.classification,
                        "image_extension": clothing.image_extension,
                        "image_data": image_base64
                    }

        cluster.shutdown()
        return result

    except Exception as e:
        logger.error(f"Error in get_combo_details: {str(e)}")
        raise HTTPException(
            status_code=500,
            detail=f"Failed to retrieve combo details: {str(e)}"
        )
    finally:
        # Clean up temp files
        for temp_file in temp_files:
            if os.path.exists(temp_file):
                os.remove(temp_file)
                logger.info(f"Cleaned up temp file: {temp_file}")

# For path /generate, take 2 file images as input (cloth, person) and return generated 'worn' image 
# (for now return 2nd image as it is)

@app.post("/generate")
async def generate_image(
    userId: str,
    file: UploadFile = File(...),
    file2: UploadFile = File(...)
):
    # Validate file type
    if not file.content_type.startswith('image/'):
        raise HTTPException(status_code=400, detail="File must be an image")
    
    if not file2.content_type.startswith('image/'):
        raise HTTPException(status_code=400, detail="File must be an image")

    # Extract file extension from original filename
    file_extension = Path(file.filename).suffix.lower()
    if not file_extension:
        file_extension = '.jpg'  # default extension if none provided
    file_extension2 = Path(file2.filename).suffix.lower()
    if not file_extension2:
        file_extension2 = '.jpg'

    image_id = str(uuid.uuid4())
    local_path = f"tmp/{image_id}{file_extension}"
    local_path2 = f"tmp/{image_id}{file_extension2}"
    # Ensure tmp directory exists
    os.makedirs("tmp", exist_ok=True)
    # Save uploaded file
    with open(local_path, "wb") as f:
        f.write(await file.read())
    with open(local_path2, "wb") as f:
        f.write(await file2.read())
    try:
        print('hi')

        
        return {
            "status": "completed",
            
        }
    except subprocess.CalledProcessError as e:
        if os.path.exists(local_path):
            os.remove(local_path)
        if os.path.exists(local_path2):
            os.remove(local_path2)
        raise HTTPException(status_code=500, detail=f"Processing failed: {e.stderr}")

# Input an image uploaded by user, and then return a list of base64 encoded images
@app.post("/generate/extract")
async def generate_image_extract(
    file: UploadFile = File(...),
):
    # Validate file type
    if not file.content_type.startswith('image/'):
        raise HTTPException(status_code=400, detail="File must be an image")

    # Extract file extension from original filename
    file_extension = Path(file.filename).suffix.lower()
    if not file_extension:
        file_extension = '.jpg'

    # Read the uploaded file into a NumPy array
    file_bytes = await file.read()
    np_img = np.frombuffer(file_bytes, np.uint8)
    image = cv2.imdecode(np_img, cv2.IMREAD_COLOR)

    # Zero padding on image
    padded_img = zero_padding(image, 100)

    persons = extract_people(padded_img, file_extension)
    
    return {
        "status": "completed",
        "persons": persons
    }


# Input 2 images uploaded by user (cloth and person) and return a single base64 encoded image
@app.post("/generate/tryon")
async def generate_image_tryon(
    file: UploadFile = File(...),
    file2: UploadFile = File(...)
):
    # Validate file type
    if not file.content_type.startswith('image/'):
        raise HTTPException(status_code=400, detail="File must be an image")
    
    if not file2.content_type.startswith('image/'):
        raise HTTPException(status_code=400, detail="File must be an image")

    # Extract file extension from original filename
    file_extension = Path(file.filename).suffix.lower()
    if not file_extension:
        file_extension = '.jpg'
    file_extension2 = Path(file2.filename).suffix.lower()   
    if not file_extension2:
        file_extension2 = '.jpg'

    result_img = vton_model(file, file2)
    # Convert the result image to base64
    _, buffer = cv2.imencode(file_extension, result_img)
    result_image_base64 = base64.b64encode(buffer).decode()
    return {
        "status": "completed",
        "result_image": result_image_base64
    }


def zero_padding(img: np.ndarray, padding: int) -> np.ndarray:
    """
    Apply zero padding to the image.
    
    Args:
        img (numpy.ndarray): Input image.
        padding (int): Amount of padding to add to each side.
        
    Returns:
        numpy.ndarray: Padded image.
    """
    # Get the shape of the original image
    h, w, c = img.shape

    # Calculate the new dimensions with padding
    new_h = h + 2 * padding
    new_w = w + 2 * padding

    # Create a new image with the new dimensions
    padded_img = cv2.resize(img, (new_w, new_h))
    # Fill the new image with zeros (black)
    padded_img.fill(0)

    # Copy the original image into the center of the new image
    padded_img[padding:padding + h, padding:padding + w] = img
    return padded_img

def extract_people(img: np.ndarray, file_extension: str) -> list:
    """
    Extract Individual people from an image usign YOLO model

    Args:
        img (numpy.ndarray): Input image.
        file_extension (str): File extension for the output images.
    
    Returns:
        list: List of base64 encoded images of individual persons.
    """

        # Perform inference on the image
    results = yolo_model.predict(source=img, conf=0.5)
    # Filter results to only include persons (class ID 0)
    person_detections = [box for box in results[0].boxes if box.cls == 0]

    offset = 50
    # Display individual extracted people
    persons = []
    for box in person_detections:
        x1, y1, x2, y2 = map(int, box.xyxy[0])
        x1 -= offset
        y1 -= offset
        x2 += offset
        y2 += offset
        person_image = img[y1:y2, x1:x2]
        person_image = cv2.cvtColor(person_image, cv2.COLOR_BGR2RGB)
        _, buffer = cv2.imencode(file_extension, person_image)
        person_image_base64 = base64.b64encode(buffer).decode()
        persons.append(person_image_base64)
    
    return persons

# Lighting adjustment
def generate_cloth_mask(image):
    img = image.copy()
    Z = img.reshape((-1, 3)).astype(np.float32)

    criteria = (cv2.TERM_CRITERIA_EPS + cv2.TERM_CRITERIA_MAX_ITER, 10, 1.0)
    K = 3
    _, labels, centers = cv2.kmeans(Z, K, None, criteria, 10, cv2.KMEANS_RANDOM_CENTERS)

    centers = np.uint8(centers)
    segmented = centers[labels.flatten()].reshape(image.shape)

    counts = np.bincount(labels.flatten())
    main_label = np.argmax(counts[1:]) + 1 if len(counts) > 1 else 0
    mask = (labels.flatten() == main_label).astype(np.uint8) * 255
    mask = mask.reshape(image.shape[:2])

    kernel = np.ones((5, 5), np.uint8)
    mask = cv2.morphologyEx(mask, cv2.MORPH_OPEN, kernel)
    mask = cv2.morphologyEx(mask, cv2.MORPH_CLOSE, kernel)

    num_labels, labels, stats, _ = cv2.connectedComponentsWithStats(mask, connectivity=8)
    cleaned_mask = np.zeros_like(mask)
    for i in range(1, num_labels):
        if stats[i, cv2.CC_STAT_AREA] >= 500:
            cleaned_mask[labels == i] = 255

    return cleaned_mask


def apply_effect_on_shirt(image, mask, condition="sunny", intensity=1.0):
    shirt_region = cv2.bitwise_and(image, image, mask=mask)
    modified = shirt_region.copy()
    h, w = mask.shape[:2]

    if condition == "sunny":
        modified = cv2.convertScaleAbs(shirt_region, alpha=1.35 * intensity, beta=50)
        hsv = cv2.cvtColor(modified, cv2.COLOR_BGR2HSV)
        hsv[:, :, 1] = np.clip(hsv[:, :, 1] + 45 * intensity, 0, 255)
        modified = cv2.cvtColor(hsv, cv2.COLOR_HSV2BGR)

    elif condition == "cloudy":
        modified = cv2.convertScaleAbs(shirt_region, alpha=0.95 * intensity, beta=-10)
        overlay = np.full(shirt_region.shape, (130, 130, 150), dtype=np.uint8)
        modified = cv2.addWeighted(modified, 0.85, overlay, 0.15, 0)

    elif condition == "rainy":
        modified = cv2.GaussianBlur(shirt_region, (5, 5), 1)
        wet_effect = cv2.addWeighted(modified, 0.9, np.full(shirt_region.shape, 30, dtype=np.uint8), 0.1, 0)
        reflection = cv2.GaussianBlur(wet_effect, (7, 7), 2)
        modified = cv2.addWeighted(wet_effect, 0.92, reflection, 0.08, 0)
        
    elif condition == "bright":
        modified = cv2.convertScaleAbs(shirt_region, alpha=0.7 * 1.7, beta=20)
        modified = cv2.GaussianBlur(modified, (3, 3), 0)

    elif condition == "dim":
        modified = cv2.convertScaleAbs(shirt_region, alpha=1 * intensity, beta=-2)
        dark_overlay = np.full(shirt_region.shape, (20, 20, 20), dtype=np.uint8)
        modified = cv2.addWeighted(modified, 0.8, dark_overlay, 0.2, 0)

    elif condition == "warm":
        warm_overlay = np.full(shirt_region.shape, (0, 50, 100), dtype=np.uint8)
        modified = cv2.addWeighted(shirt_region, 0.8, warm_overlay, 0.5, 0)


    elif condition == "night":
        modified = cv2.convertScaleAbs(shirt_region, alpha=0.9 * intensity, beta=-40)
        overlay = np.full(shirt_region.shape, (40, 40, 100), dtype=np.uint8)
        modified = cv2.addWeighted(modified, 0.85, overlay, 0.15, 0)

        center = (w // 2, h // 2)
        radius = max(w, h) // 4
        light_effect = np.zeros_like(shirt_region, dtype=np.uint8)
        cv2.circle(light_effect, center, radius, (40, 40, 120), -1)
        light_effect = cv2.GaussianBlur(light_effect, (61, 61), 30)
        modified = cv2.addWeighted(modified, 0.92, light_effect, 0.08, 0)

    blurred_mask = cv2.GaussianBlur(mask, (9, 9), 3)
    blurred_mask = blurred_mask.astype(float) / 255.0
    blurred_mask = cv2.merge([blurred_mask] * 3)

    modified = modified.astype(float)
    shirt_region = shirt_region.astype(float)
    smooth_blend = modified * blurred_mask + shirt_region * (1 - blurred_mask)
    smooth_blend = np.clip(smooth_blend, 0, 255).astype(np.uint8)

    modified_masked = cv2.bitwise_and(smooth_blend, smooth_blend, mask=mask)
    background = cv2.bitwise_and(image, image, mask=cv2.bitwise_not(mask))
    final_image = cv2.add(background, modified_masked)

    return final_image
    

@app.post("/adjust_lighting")
async def adjust_lighting(file: UploadFile = File(...), lighting_type: str = "sunny"):
    try:
        # Read the uploaded file into a NumPy array
        file_bytes = await file.read()
        np_arr = np.frombuffer(file_bytes, np.uint8)
        image = cv2.imdecode(np_arr, cv2.IMREAD_COLOR)

        if image is None:
            raise HTTPException(status_code=400, detail="Invalid image")

        # Generate the cloth mask
        mask = generate_cloth_mask(image)

        # Optional: Invert if needed (based on brightness)
        if np.mean(image[mask == 255]) > np.mean(image[mask == 0]):
            mask = cv2.bitwise_not(mask)

        # Apply the lighting effect
        result = apply_effect_on_shirt(image, mask, lighting_type.lower(), intensity=1.0)

        # Encode the result to JPEG
        _, buffer = cv2.imencode('.jpg', result)
        return StreamingResponse(io.BytesIO(buffer.tobytes()), media_type="image/jpeg")

    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error: {str(e)}")





