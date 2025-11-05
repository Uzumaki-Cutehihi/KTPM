# ⚡ Cách Chạy Nhanh - BookVault

## 🚀 3 Cách Chạy Nhanh Nhất

### 1️⃣ Script (NHANH NHẤT - 1 lệnh)

**Windows:**
```cmd
build-and-start.bat
```

**Linux/Mac:**
```bash
./build-and-start.sh
```

### 2️⃣ Docker Compose (Đầy đủ)

```bash
docker compose build && docker compose up -d
```

### 3️⃣ Core Services Only (NHANH - bỏ Elasticsearch, MinIO, Kafka)

```bash
docker compose -f docker-compose.minimal.yml build && docker compose -f docker-compose.minimal.yml up -d
```

---

## 📊 Kiểm Tra

```bash
# Xem status
docker compose ps

# Xem logs
docker compose logs -f

# Health check
curl http://localhost:8080/actuator/health
```

---

## 🛑 Dừng

```bash
docker compose down
```

---

## 📝 Ports

- **Gateway**: http://localhost:8080
- **Catalog**: http://localhost:8081
- **IAM**: http://localhost:8082
- **Borrowing**: http://localhost:8083
- **Admin**: http://localhost:8087

