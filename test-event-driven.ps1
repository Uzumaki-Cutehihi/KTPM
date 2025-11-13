# Test script cho event-driven architecture

Write-Host "=== BookVault Event-Driven Architecture Test ===" -ForegroundColor Green

# Function to test API endpoints
function Test-Endpoint {
    param($Url, $Method = "GET", $Body = $null, $Headers = @{})
    
    try {
        $params = @{
            Uri = $Url
            Method = $Method
            Headers = $Headers
            TimeoutSec = 10
        }
        
        if ($Body) {
            $params.Body = $Body
            $params.ContentType = "application/json"
        }
        
        $response = Invoke-RestMethod @params
        return @{ Success = $true; Data = $response }
    }
    catch {
        return @{ Success = $false; Error = $_.Exception.Message }
    }
}

# 1. Test Catalog Service
Write-Host "`n1. Testing Catalog Service..." -ForegroundColor Yellow
$catalogTest = Test-Endpoint -Url "http://localhost:8081/api/catalog/v1/books"
if ($catalogTest.Success) {
    Write-Host "✅ Catalog Service is running" -ForegroundColor Green
} else {
    Write-Host "❌ Catalog Service error: $($catalogTest.Error)" -ForegroundColor Red
}

# 2. Test Search Service
Write-Host "`n2. Testing Search Service..." -ForegroundColor Yellow
$searchTest = Test-Endpoint -Url "http://localhost:8084/api/search/v1/books/search?query=test"
if ($searchTest.Success) {
    Write-Host "✅ Search Service is running" -ForegroundColor Green
} else {
    Write-Host "❌ Search Service error: $($searchTest.Error)" -ForegroundColor Red
}

# 3. Test Borrowing Service
Write-Host "`n3. Testing Borrowing Service..." -ForegroundColor Yellow
$borrowingTest = Test-Endpoint -Url "http://localhost:8083/api/borrowing/v1/loans"
if ($borrowingTest.Success) {
    Write-Host "✅ Borrowing Service is running" -ForegroundColor Green
} else {
    Write-Host "❌ Borrowing Service error: $($borrowingTest.Error)" -ForegroundColor Red
}

# 4. Test Elasticsearch
Write-Host "`n4. Testing Elasticsearch..." -ForegroundColor Yellow
$elasticTest = Test-Endpoint -Url "http://localhost:9200/_cluster/health"
if ($elasticTest.Success) {
    Write-Host "✅ Elasticsearch is running" -ForegroundColor Green
    Write-Host "Cluster status: $($elasticTest.Data.status)" -ForegroundColor Cyan
} else {
    Write-Host "❌ Elasticsearch error: $($elasticTest.Error)" -ForegroundColor Red
}

# 5. Test Kafka (via logs)
Write-Host "`n5. Testing Event Flow..." -ForegroundColor Yellow

# Create a test book
$bookData = @{
    title = "Test Event-Driven Book"
    author = "Test Author"
    isbn = "978-TEST-1234"
    quantity = 5
    description = "Testing event-driven architecture"
} | ConvertTo-Json

Write-Host "Creating test book..." -ForegroundColor Cyan
$createBook = Test-Endpoint -Url "http://localhost:8081/api/catalog/v1/books" -Method "POST" -Body $bookData

if ($createBook.Success) {
    Write-Host "✅ Book created successfully" -ForegroundColor Green
    $bookId = $createBook.Data.id
    Write-Host "Book ID: $bookId" -ForegroundColor Cyan
    
    # Wait for event processing
    Start-Sleep -Seconds 3
    
    # Search for the book
    Write-Host "Searching for the book in Elasticsearch..." -ForegroundColor Cyan
    $searchResult = Test-Endpoint -Url "http://localhost:8084/api/search/v1/books/search?query=TEST"
    
    if ($searchResult.Success -and $searchResult.Data.Count -gt 0) {
        Write-Host "✅ Book found in search index!" -ForegroundColor Green
        Write-Host "Search results: $($searchResult.Data.Count) books found" -ForegroundColor Cyan
    } else {
        Write-Host "⚠️  Book not found in search index (event may still be processing)" -ForegroundColor Yellow
    }
} else {
    Write-Host "❌ Failed to create book: $($createBook.Error)" -ForegroundColor Red
}

Write-Host "`n=== Test Summary ===" -ForegroundColor Green
Write-Host "Event-driven architecture test completed!" -ForegroundColor White
Write-Host "Check the logs above for detailed information." -ForegroundColor Gray
Write-Host "`nTo monitor real-time events, run:" -ForegroundColor Cyan
Write-Host "docker logs -f ktpm-catalog-1 | Select-String 'published'" -ForegroundColor Gray
Write-Host "docker logs -f ktpm-search-1 | Select-String 'indexed'" -ForegroundColor Gray