# FitVision

This project implements a **virtual try-on system** that maps standalone clothing images onto images of people using **CycleGAN**. Alongside the deep learning pipeline, we’ve integrated **Apache Spark** for data processing, **Apache Cassandra** for fast, scalable storage, and **HDFS** for distributed file management.

---

## 🚀 Features

- 🔄 **CycleGAN for unpaired image translation** between clothes and human models.
- ⚡ **Apache Spark** for large-scale image preprocessing and transformation.
- 📁 **HDFS** for distributed, fault-tolerant storage of datasets and model outputs.
- 🗃️ **Apache Cassandra** to manage image metadata and intermediate results efficiently.

---

## 🖥️ Hardware Requirements

- **GPU**:

  - Minimum: 4 GB VRAM (e.g., NVIDIA GTX 1050 Ti)
  - Recommended: 4-core GPU for faster processing

- **CPU**: Quad-core processor (Intel i5 / AMD Ryzen 5 or better)

- **RAM**:

  - Minimum: 8 GB
  - Recommended: 16 GB

- **Storage**: At least 50 GB free (SSD preferred)

---

## 🐳 Installation & Docker Setup

This project is containerized using Docker and Docker Compose.

### 🔧 Prerequisites

- [Docker](https://desktop.docker.com/win/main/amd64/Docker%20Desktop%20Installer.exe?utm_source=docker&utm_medium=webreferral&utm_campaign=dd-smartbutton&utm_location=module)
- [Docker Compose](https://docs.docker.com/compose/)

### 📦 Setup Steps

1. **Clone the repository**

   ```bash
   git clone https://github.com/lightningmq95/FitVision.git
   cd FitVision
   ```

2. **Naviagate to the Docker Setup Folder**

   ```bash
    cd Docker
   ```

3. **Check environment files Ensure the .env and hadoop.env files are present and configured. These are already included in the repo, but feel free to modify as needed.**

4. **Build and start the containers**
   ```bash
    docker-compose up --build
   ```
