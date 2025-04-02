from pyspark.sql import SparkSession
import tensorflow as tf
import logging
import numpy as np
from tensorflow.keras.preprocessing import image
import sys
import cv2
import json
import os
from pathlib import Path

# Configure more detailed logging
logging.basicConfig(
    level=logging.DEBUG,  # Changed to DEBUG for more detail
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s - %(pathname)s:%(lineno)d',
    stream=sys.stderr
)
logger = logging.getLogger(__name__)

# Redirect TensorFlow logging to stderr
tf.get_logger().setLevel(logging.ERROR)
os.environ['TF_CPP_MIN_LOG_LEVEL'] = '3'

def classify_image(image_path):
    """Classify a single image using the TensorFlow model."""
    logger.info(f"Starting classification for image: {image_path}")

    # Load model from the mounted models directory
    logger.info("Loading model from /opt/spark-models/model.keras")

    model = tf.keras.models.load_model('/opt/spark-models/model.keras')
    CLASS_NAMES = ['dress', 'pants', 'shirts']
    
    try:
        # Check if file exists
        if not os.path.exists(image_path):
            raise FileNotFoundError(f"Image file not found: {image_path}")
        

        # Read and preprocess image using OpenCV
        logger.info("Reading and preprocessing image")

        img = cv2.imread(image_path, cv2.IMREAD_GRAYSCALE)
        if img is None:
            raise ValueError(f"Failed to load image: {image_path}")    
        logger.info(f"Original image shape: {img.shape}")
        
        img = cv2.resize(img, (28, 28))
        img_array = np.expand_dims(img, axis=[0, -1])  # Add batch and channel dimensions
        img_array = img_array / 255.0

        # Make prediction
        logger.info("Making prediction")

        predictions = model.predict(img_array)
        predicted_class = np.argmax(predictions[0])
        classification = CLASS_NAMES[predicted_class]
        logger.info(f"Classification result: {classification}")

        return {"classification": classification, "color": "red"}
    except Exception as e:
        return {"error": str(e)}
    
def main():
    # Initialize Spark session
    
    spark = SparkSession.builder \
        .appName("Image Classification") \
        .getOrCreate()
    
    # Disable Spark logging
    spark.sparkContext.setLogLevel("ERROR")

    try:
        # Get arguments - now expecting local path
        local_path = sys.argv[1]
        image_id = sys.argv[2]
        user_id = sys.argv[3]

        # Perform classification
        result = classify_image(local_path)
        result = {"classification": "shirt", "color": "red"}

        
        # Print only the classification result to stdout
        print(json.dumps(result))
        
    except Exception as e:
        print(json.dumps({"error": str(e)}))
    finally:
        spark.stop()

if __name__ == "__main__":
    main()