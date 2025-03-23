import tensorflow as tf

# HDFS paths for TFRecords
train_tfrecord = "hdfs://localhost:9000/user/hadoop/tfrecord/train.tfrecord"
test_tfrecord = "hdfs://localhost:9000/user/hadoop/tfrecord/test.tfrecord"

# Function to parse TFRecord files
def parse_tfrecord(example):
    feature_description = {
        "image": tf.io.FixedLenFeature([], tf.string),
        "label": tf.io.FixedLenFeature([], tf.int64),
    }
    parsed_example = tf.io.parse_single_example(example, feature_description)
    image = tf.io.decode_jpeg(parsed_example["image"], channels=1)
    image = tf.image.convert_image_dtype(image, tf.float32)
    image = tf.image.resize(image, [28, 28])
    label = parsed_example["label"]
    return image, label

# Load datasets using TensorFlow with HDFS support
train_dataset = tf.data.TFRecordDataset(train_tfrecord, compression_type=None).map(parse_tfrecord).batch(32)
test_dataset = tf.data.TFRecordDataset(test_tfrecord, compression_type=None).map(parse_tfrecord).batch(32)

# Define the model
model = tf.keras.Sequential([
    tf.keras.layers.Conv2D(32, (3, 3), activation='relu', input_shape=(28, 28, 1)),
    tf.keras.layers.MaxPooling2D((2, 2)),
    tf.keras.layers.Conv2D(64, (3, 3), activation='relu'),
    tf.keras.layers.MaxPooling2D((2, 2)),
    tf.keras.layers.Conv2D(64, (3, 3), activation='relu'),
    tf.keras.layers.Flatten(),
    tf.keras.layers.Dense(128, activation='relu'),
    tf.keras.layers.Dense(3, activation='softmax')  # 3 classes: dress, pants, shirts
])

model.compile(optimizer='adam',
              loss=tf.keras.losses.SparseCategoricalCrossentropy(from_logits=False),
              metrics=['accuracy'])

# Train the model
model.fit(train_dataset, epochs=30)

# Save the trained model
model.save("model")