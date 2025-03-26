from pyspark.sql import SparkSession
import tensorflow as tf
import numpy as np
import os

# Initialize Spark
spark = SparkSession.builder.appName("ImagePreprocessing").getOrCreate()

# HDFS paths
train_path = "/opt/bitnami/spark/Dataset/train"
test_path = "/opt/bitnami/spark/Dataset/test"
output_path = "/opt/bitnami/spark/tfrecords/"

# Function to read, resize, and normalize images using TensorFlow
def preprocess_image(file_path, label, encode_as_png=False):
    image = tf.io.read_file(file_path)
    image = tf.image.decode_jpeg(image, channels=1)  # Decode JPEG as grayscale
    image = tf.image.resize(image, [28, 28])  # Resize to 28x28
    image = image / 255.0  # Normalize
    
    # Option 1: Store PNG (Lossless)
    if encode_as_png:
        image_encoded = tf.io.encode_png(tf.cast(image * 255, tf.uint8)).numpy()
    
    # Option 2: Store JPEG with lower compression
    else:
        image_encoded = tf.io.encode_jpeg(tf.cast(image * 255, tf.uint8), quality=95).numpy()

    return image_encoded, label

# Get class labels
class_labels = os.listdir(train_path)
label_dict = {name: idx for idx, name in enumerate(class_labels)}

# Load images and labels
def load_images(directory):
    data = []
    for class_name in os.listdir(directory):
        class_dir = os.path.join(directory, class_name)
        if os.path.isdir(class_dir):
            for img_file in os.listdir(class_dir):
                img_path = os.path.join(class_dir, img_file)
                label = label_dict[class_name]
                data.append((img_path, label))
    return data

train_data = load_images(train_path)
test_data = load_images(test_path)

# Convert dataset to TFRecord format
def write_tfrecord(data, output_filename, encode_as_png=False):
    with tf.io.TFRecordWriter(output_filename) as writer:
        for img_path, label in data:
            img_encoded, label = preprocess_image(img_path, label, encode_as_png=encode_as_png)
            feature = {
                "image": tf.train.Feature(bytes_list=tf.train.BytesList(value=[img_encoded])),
                "label": tf.train.Feature(int64_list=tf.train.Int64List(value=[label])),
            }
            example = tf.train.Example(features=tf.train.Features(feature=feature))
            writer.write(example.SerializeToString())

# Use PNG (lossless) or JPEG (lower compression)
use_png = True  # Change to False if you prefer JPEG with lower compression

write_tfrecord(train_data, output_path + "train.tfrecord", encode_as_png=use_png)
write_tfrecord(test_data, output_path + "test.tfrecord", encode_as_png=use_png)

print("TFRecord files saved to HDFS with lower compression.")