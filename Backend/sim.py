# from flask import Flask, request, jsonify
# import uuid
# import os
# import tensorflow as tf
# import numpy as np
# from tensorflow.keras.preprocessing import image
# from werkzeug.utils import secure_filename

# app = Flask(__name__)

# # Load the model
# model = tf.keras.models.load_model('model.keras')
# CLASS_NAMES = ['dress', 'pants', 'shirts']  # Replace with your actual class names

# # Set the upload folder and allowed extensions
# UPLOAD_FOLDER = 'uploads'
# ALLOWED_EXTENSIONS = {'jpg', 'jpeg', 'png'}

# app.config['UPLOAD_FOLDER'] = UPLOAD_FOLDER

# # Helper function to check allowed extensions
# def allowed_file(filename):
#     return '.' in filename and filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS

# @app.route("/upload-image/<userId>", methods=["POST"])
# def upload_image(userId):
#     if 'file' not in request.files:
#         return jsonify({"error": "No file part"}), 400
    
#     file = request.files['file']
    
#     if file.filename == '':
#         return jsonify({"error": "No selected file"}), 400
    
#     if file and allowed_file(file.filename):
#         filename = secure_filename(file.filename)
#         file_extension = filename.rsplit('.', 1)[1].lower()
#         image_id = str(uuid.uuid4())  # Generate a unique image ID
#         local_path = os.path.join(app.config['UPLOAD_FOLDER'], f"{image_id}.{file_extension}")
        
#         # Ensure the upload folder exists
#         os.makedirs(app.config['UPLOAD_FOLDER'], exist_ok=True)

#         # Save the file locally
#         file.save(local_path)

#         # Process the image for classification
#         try:
#             # Load the image and preprocess it
#             img = image.load_img(local_path, target_size=(28, 28), color_mode='grayscale')
#             img_array = image.img_to_array(img)
#             img_array = np.expand_dims(img_array, axis=0)
#             img_array = img_array / 255.0  # Normalize image

#             # Make the prediction
#             predictions = model.predict(img_array)
#             predicted_class = np.argmax(predictions[0])
#             classification = CLASS_NAMES[predicted_class]

#             # Clean up the local file after processing
#             os.remove(local_path)

#             return jsonify({
#                 "status": "completed",
#                 "image_id": image_id,
#                 "user_id": userId,
#                 "category": classification,
#                 "image_extension": file_extension
#             })
#         except Exception as e:
#             # Clean up the local file if an error occurs
#             if os.path.exists(local_path):
#                 os.remove(local_path)
#             return jsonify({"error": f"Image processing failed: {str(e)}"}), 500
#     else:
#         return jsonify({"error": "Invalid file type"}), 400

# if __name__ == "__main__":
#     app.run(debug=True)


#------------------------------------------
# from flask import Flask, request, jsonify
# import os
# import subprocess

# app = Flask(__name__)

# def run_tryon(cloth_name, img_name):
#     os.environ["CUDA_VISIBLE_DEVICES"] = "0"
#     command = [
#         "python", "test.py",
#         "--name", "res",
#         "--cloth_name", cloth_name,
#         "--img_name", img_name
#     ]
#     subprocess.run(command)

# @app.route('/tryon', methods=['POST'])
# def try_on():
#     data = request.get_json()
    
#     # Validate input
#     cloth_name = data.get("cloth_name")
#     img_name = data.get("img_name")

#     if not cloth_name or not img_name:
#         return jsonify({"error": "Missing 'cloth_name' or 'img_name'"}), 400

#     try:
#         run_tryon(cloth_name, img_name)
#         # Assuming your script saves output to a specific location, e.g., 'results/res/'
#         output_path = f"results/res/{img_name}"
#         if not os.path.exists(output_path):
#             return jsonify({"error": "Try-on failed, result not found."}), 500

#         return jsonify({"message": "Try-on successful", "result_image": output_path})
#     except Exception as e:
#         return jsonify({"error": str(e)}), 500

# if __name__ == "__main__":
#     app.run(debug=True)



from flask import Flask, request, jsonify, send_file
import base64
import os
import uuid
import numpy as np
import cv2
import io
from PIL import Image
from tensorflow.keras.preprocessing import image
from ultralytics import YOLO

import tensorflow as tf
from werkzeug.utils import secure_filename

# Initialize Flask app
app = Flask(__name__)

# --------------- Image Upload and Classification ---------------------
# Load the model for classification
model = tf.keras.models.load_model('model.keras')
yolo_model = YOLO("yolo11n.pt")
CLASS_NAMES = ['dress', 'pants', 'shirts']  

# Set the upload folder and allowed extensions
UPLOAD_FOLDER = 'uploads'
ALLOWED_EXTENSIONS = {'jpg', 'jpeg', 'png'}
app.config['UPLOAD_FOLDER'] = UPLOAD_FOLDER

# Helper function to check allowed extensions
def allowed_file(filename):
    return '.' in filename and filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS

@app.route("/upload-image/<userId>", methods=["POST"])
def upload_image(userId):
    if 'file' not in request.files:
        return jsonify({"error": "No file part"}), 400
    
    file = request.files['file']
    
    if file.filename == '':
        return jsonify({"error": "No selected file"}), 400
    
    if file and allowed_file(file.filename):
        filename = secure_filename(file.filename)
        file_extension = filename.rsplit('.', 1)[1].lower()
        image_id = str(uuid.uuid4())  # Generate a unique image ID
        local_path = os.path.join(app.config['UPLOAD_FOLDER'], f"{image_id}.{file_extension}")
        
        # Ensure the upload folder exists
        os.makedirs(app.config['UPLOAD_FOLDER'], exist_ok=True)

        # Save the file locally
        file.save(local_path)

        # Process the image for classification
        try:
            # Load the image and preprocess it
            img = image.load_img(local_path, target_size=(28, 28), color_mode='grayscale')
            img_array = image.img_to_array(img)
            img_array = np.expand_dims(img_array, axis=0)
            img_array = img_array / 255.0  # Normalize image

            # Make the prediction
            predictions = model.predict(img_array)
            predicted_class = np.argmax(predictions[0])
            classification = CLASS_NAMES[predicted_class]

            # Clean up the local file after processing
            os.remove(local_path)

            return jsonify({
                "status": "completed",
                "image_id": image_id,
                "user_id": userId,
                "category": classification,
                "image_extension": file_extension
            })
        except Exception as e:
            # Clean up the local file if an error occurs
            if os.path.exists(local_path):
                os.remove(local_path)
            return jsonify({"error": f"Image processing failed: {str(e)}"}), 500
    else:
        return jsonify({"error": "Invalid file type"}), 400

# --------------- Lighting Adjustment ---------------------------
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

@app.route('/adjust_lighting', methods=['POST'])
def adjust_lighting():
    try:
        data = request.get_json()
        image_data = data['image']
        lighting_type = data['lighting_type']

        decoded_bytes = base64.b64decode(image_data)
        np_arr = np.frombuffer(decoded_bytes, np.uint8)
        image = cv2.imdecode(np_arr, cv2.IMREAD_COLOR)

        if image is None:
            return jsonify({'error': 'Invalid image'}), 400

        mask = generate_cloth_mask(image)

        # Optional: Invert if needed (based on brightness)
        if np.mean(image[mask == 255]) > np.mean(image[mask == 0]):
            mask = cv2.bitwise_not(mask)

        result = apply_effect_on_shirt(image, mask, lighting_type.lower(), intensity=1.0)

        # Encode to JPEG to send response
        _, buffer = cv2.imencode('.jpg', result)
        return send_file(
            io.BytesIO(buffer),
            mimetype='image/jpeg',
            as_attachment=False
        )

    except Exception as e:
        return jsonify({'error': str(e)}), 500

# --------------- Combo Management --------------------------
# Dummy data: Combos for each user
COMBOS_DB = {
    "test_user": ["Casual Outfit", "Party Wear", "Formal Suit"],
    "user123": ["Summer Vibes", "Winter Warmth"]
}

def encode_image(image_path):
    if not os.path.exists(image_path):
        print(f"ERROR: File '{image_path}' not found!")
        return None # File does not exist

    try:
        with open(image_path, "rb") as img_file:
            encoded = base64.b64encode(img_file.read()).decode("utf-8")
            return encoded
    except Exception as e:
        print(f"ERROR: Failed to encode '{image_path}': {e}")
        return None  # If encoding fails

# Dummy data(Using mix of Base64 & URLs)
COMBO_IMAGES_DB = {
    "Casual Outfit": [
        {"imageName": "shirt.jpg", "imageData": encode_image("shirt.jpg"), "category": "Shirt"},
        {"imageName": "jeans.png", "imageData": encode_image("jeans.png") or "https://via.placeholder.com/150", "category": "Jeans"}
    ],
    "Party Wear": [
        {"imageName": "dress.png", "imageData": "https://th.bing.com/th/id/OIP.FZ-JPD_X3JhkSI8r239KhQAAAA?w=193&h=322&c=7&r=0&o=5&dpr=1.3&pid=1.7", "category": "Dress"},
        {"imageName": "heels.png", "imageData": "https://th.bing.com/th/id/OIP.oHemjt3FdHv2avsbkXwBBwHaHa?w=208&h=208&c=7&r=0&o=5&dpr=1.3&pid=1.7", "category": "Shoes"}
    ]
}

@app.route("/getCombos/<user_id>", methods=["GET"])
def get_combos(user_id):
    combos = COMBOS_DB.get(user_id, [])
    return jsonify(combos)

@app.route("/getComboDetails/<user_id>/<combo_name>", methods=["GET"])
def get_combo_details(user_id, combo_name):
    images = COMBO_IMAGES_DB.get(combo_name, [])
    return jsonify(images)

# @app.route("/extract-people", methods=["POST"])
# def extract_people():
#     if 'file' not in request.files:
#         return jsonify({"error": "No file part"}), 400
    
#     file = request.files['file']
    
#     if file.filename == '':
#         return jsonify({"error": "No selected file"}), 400
    
#     if file and allowed_file(file.filename):
#         filename = secure_filename(file.filename)
#         file_extension = filename.rsplit('.', 1)[1].lower()

#         file_bytes = file.read()
#         np_img = np.frombuffer(file_bytes, np.uint8)
#         image = cv2.imdecode(np_img, cv2.IMREAD_COLOR)

#         if image is None:
#             return jsonify({"error": "Invalid image file"}), 400

#         padded_img = zero_padding(image, 50)
#         persons = extract_people(padded_img, file_extension)
        
#         return jsonify({
#             "status": "completed",
#             "persons": persons,
#             "image_extension": file_extension
#         })

@app.route("/detect-faces", methods=["POST"])
def extract_people():
    try:
        data = request.get_json()
        base64_image = data.get("base64Image")

        if not base64_image:
            return jsonify({"error": "No image data provided"}), 400

        # Decode the base64 image
        file_bytes = base64.b64decode(base64_image)
        np_img = np.frombuffer(file_bytes, np.uint8)
        image = cv2.imdecode(np_img, cv2.IMREAD_COLOR)

        if image is None:
            return jsonify({"error": "Invalid image data"}), 400

        # Process the image
        padded_img = zero_padding(image, 50)
        persons = extract_people(padded_img, ".jpg")

        return jsonify(persons)
    except Exception as e:
        return jsonify({"error": str(e)}), 500
        


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

if __name__ == "__main__":
    app.run(debug=True)
