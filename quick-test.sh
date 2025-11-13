#!/bin/bash

echo "=== BookVault Quick Test ==="
echo ""

# Function to test API
test_api() {
    local url=$1
    local name=$2
    echo -n "Testing $name... "
    if curl -s -f "$url" > /dev/null; then
        echo "✓ OK"
        return 0
    else
        echo "✗ FAILED"
        return 1
    fi
}

# Wait for services to be ready
echo "Waiting for services to start..."
sleep 30

# Test basic connectivity
echo "1. Testing service connectivity:"
test_api "http://localhost:8081/api/catalog/v1/books" "Catalog Service"
test_api "http://localhost:8083/api/borrowing/v1/loans" "Borrowing Service"
test_api "http://localhost:8084/api/search/v1/books?query=test" "Search Service"
test_api "http://localhost:8085/api/notification/v1/send" "Notification Service"

# Test book creation
echo ""
echo "2. Testing book creation and search:"
echo "Creating test book..."
BOOK_ID=$(curl -s -X POST http://localhost:8081/api/catalog/v1/books \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Clean Code",
    "author": "Robert C. Martin",
    "isbn": "978-0132350884",
    "description": "A Handbook of Agile Software Craftsmanship",
    "quantity": 10,
    "category": "Programming"
  }' | grep -o '"id":[0-9]*' | cut -d':' -f2)

if [ ! -z "$BOOK_ID" ]; then
    echo "Book created with ID: $BOOK_ID"
    echo "Waiting for indexing..."
    sleep 5
    echo "Searching for book..."
    curl -s "http://localhost:8084/api/search/v1/books?query=Clean%%20Code" | head -c 200
    echo ""
else
    echo "Failed to create book"
fi

# Test loan creation
echo ""
echo "3. Testing loan creation:"
LOAN_RESPONSE=$(curl -s -X POST http://localhost:8083/api/borrowing/v1/loans \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "bookId": 1,
    "quantity": 1,
    "dueDays": 14
  }')

if [[ "$LOAN_RESPONSE" == *"id"* ]]; then
    echo "Loan created successfully"
    echo "Response: $LOAN_RESPONSE"
else
    echo "Failed to create loan"
fi

echo ""
echo "=== Test completed! ==="
echo "Check logs: docker-compose logs [service-name]"
echo "Stop services: docker-compose down"