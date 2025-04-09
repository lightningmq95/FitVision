import os
import subprocess

def main(cloth_name, img_name):
    # Set the environment variable for CUDA
    os.environ["CUDA_VISIBLE_DEVICES"] = "0"

    # Command to run the test script
    command = ["python", "test.py", "--name", "res",  "--cloth_name", cloth_name, "--img_name", img_name]

    # Run the command
    subprocess.run(command)

if __name__ == "__main__":
    main("57.jpg", "23.jpg")
    # left - cloth, right - image