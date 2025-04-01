import tensorflow as tf
import numpy as np
import cv2
import sys
from pyspark.sql import SparkSession

# Initialize Spark
spark = SparkSession.builder.appName("ImageInference").getOrCreate()

# Load the trained model
model = tf.keras.models.load_model('model')

# Function to preprocess image
def preprocess_image(img_path):
    image = cv2.imread(img_path, cv2.IMREAD_GRAYSCALE)
    image = cv2.resize(image, (28, 28))
    image = image / 255.0  # Normalize
    return np.expand_dims(image, axis=[0, -1])  # Add batch and channel dimension

# Image path
img_path = sys.argv[1]  # Pass image path as a command-line argument
img_array = preprocess_image(img_path)

# Make prediction
predictions = model.predict(img_array)
predicted_class = np.argmax(predictions[0])

class_names = ['dress', 'pants', 'shirts']
print(f'Predicted class: {class_names[predicted_class]}')