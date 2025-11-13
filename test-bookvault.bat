@echo off
echo === BookVault Docker Test Script ===
echo.

REM Kiểm tra Docker đang chạy
echo 1. Kiểm tra Docker status...
docker info >nul 2>&1
if %errorlevel% neq 0 (
    echo Lỗi: Docker không chạy. Vui lòng khởi động Docker Desktop!
    pause
    exit /b 1
)
echo Docker đang chạy OK!
echo.

REM Build services
echo 2. Building services...
echo Đang build, vui lòng đợi...
docker-compose build > build.log 2>&1
if %errorlevel% neq 0 (
    echo Lỗi build! Xem build.log để biết chi tiết
    pause
    exit /b 1
)
echo Build hoàn thành!
echo.

REM Start services
echo 3. Starting services...
docker-compose up -d
echo Đang khởi động services, vui lòng đợi 30 giây...
timeout /t 30 /nobreak >nul
echo.

REM Kiểm tra services
echo 4. Kiểm tra services...
docker-compose ps
echo.

REM Test API endpoints
echo 5. Testing API endpoints...
echo Testing Catalog Service...
curl -s http://localhost:8081/api/catalog/v1/books >nul
if %errorlevel% equ 0 (
    echo ✓ Catalog Service OK
) else (
    echo ✗ Catalog Service FAILED
)

echo Testing Search Service...
curl -s http://localhost:8084/api/search/v1/books?query=test >nul
if %errorlevel% equ 0 (
    echo ✓ Search Service OK
) else (
    echo ✗ Search Service FAILED
)

echo Testing Borrowing Service...
curl -s http://localhost:8083/api/borrowing/v1/loans >nul
if %errorlevel% equ 0 (
    echo ✓ Borrowing Service OK
) else (
    echo ✗ Borrowing Service FAILED
)

echo Testing Notification Service...
curl -s http://localhost:8085/api/notification/v1/send >nul
if %errorlevel% equ 0 (
    echo ✓ Notification Service OK
) else (
    echo ✗ Notification Service FAILED
)
echo.

REM Create test book
echo 6. Creating test book...
echo Tạo test book...
curl -s -X POST http://localhost:8081/api/catalog/v1/books -H "Content-Type: application/json" -d "{\"title\":\"Test Book\",\"author\":\"Test Author\",\"isbn\":\"123-456\",\"description\":\"Test Description\",\"quantity\":5,\"category\":\"Test\"}" >nul
if %errorlevel% equ 0 (
    echo ✓ Test book created
) else (
    echo ✗ Test book creation failed
)
echo.

REM Search test
echo 7. Testing search functionality...
timeout /t 5 /nobreak >nul
echo Search for "Test Book":
curl -s "http://localhost:8084/api/search/v1/books?query=Test%%20Book"
echo.
echo.

echo === Test hoàn thành! ===
echo.
echo Để xem logs: docker-compose logs [service-name]
echo Để dừng services: docker-compose down
echo.
pause