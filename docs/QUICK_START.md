# WA Shop - Quick Start Guide

## Prerequisites

- JDK 17+
- Docker & Docker Compose
- Flutter 3.x (for mobile app)
- PostgreSQL client (optional)

## 1. Start Database Services

```bash
# Start PostgreSQL and Redis
docker-compose up -d

# Verify services are running
docker-compose ps
```

## 2. Run Backend

```bash
cd backend

# Install Gradle wrapper (first time only)
gradle wrapper

# Run the server
./gradlew run
```

Backend will start at `http://localhost:8080`

### Test the API

```bash
# Health check
curl http://localhost:8080/health

# Get products
curl http://localhost:8080/api/products?shopId=SHOP_001

# Test WhatsApp bot (simulated)
curl -X POST http://localhost:8080/webhook/test \
  -H "Content-Type: application/json" \
  -d '{"from": "9876543210", "message": "hi"}'
```

## 3. Run Flutter App

```bash
cd flutter_app

# Get dependencies
flutter pub get

# Run the app
flutter run
```

## 4. Testing the WhatsApp Flow

### Simulated Bot Test

```bash
# Say hi
curl -X POST http://localhost:8080/webhook/test \
  -H "Content-Type: application/json" \
  -d '{"from": "9988776655", "message": "hi"}'

# View products
curl -X POST http://localhost:8080/webhook/test \
  -H "Content-Type: application/json" \
  -d '{"from": "9988776655", "message": "products"}'

# Add item 1 to cart (1 piece)
curl -X POST http://localhost:8080/webhook/test \
  -H "Content-Type: application/json" \
  -d '{"from": "9988776655", "message": "1"}'

# Add 2 pieces of item 2
curl -X POST http://localhost:8080/webhook/test \
  -H "Content-Type: application/json" \
  -d '{"from": "9988776655", "message": "2 2"}'

# Add 500g of item 11 (Tomato - priced per kg)
curl -X POST http://localhost:8080/webhook/test \
  -H "Content-Type: application/json" \
  -d '{"from": "9988776655", "message": "11 500g"}'

# Add 1kg of item 12 (Onion)
curl -X POST http://localhost:8080/webhook/test \
  -H "Content-Type: application/json" \
  -d '{"from": "9988776655", "message": "12 1kg"}'

# Add 250 grams of item 15 (Capsicum)
curl -X POST http://localhost:8080/webhook/test \
  -H "Content-Type: application/json" \
  -d '{"from": "9988776655", "message": "15 250g"}'

# View cart
curl -X POST http://localhost:8080/webhook/test \
  -H "Content-Type: application/json" \
  -d '{"from": "9988776655", "message": "cart"}'

# Clear cart
curl -X POST http://localhost:8080/webhook/test \
  -H "Content-Type: application/json" \
  -d '{"from": "9988776655", "message": "clear"}'

# Checkout
curl -X POST http://localhost:8080/webhook/test \
  -H "Content-Type: application/json" \
  -d '{"from": "9988776655", "message": "checkout"}'
```

## 5. API Documentation

### Authentication

```bash
# Login (creates user if not exists)
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"phone": "9876543210"}'

# Register new shop
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "phone": "9876543210",
    "shopName": "My Shop",
    "ownerName": "Owner Name"
  }'
```

### Products

```bash
# List products
curl "http://localhost:8080/api/products?shopId=SHOP_001"

# Add product (requires auth)
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "shopId": "SHOP_001",
    "name": "New Product",
    "price": 100,
    "unit": "piece",
    "category": "General"
  }'
```

### Orders

```bash
# List orders for shop
curl "http://localhost:8080/api/orders?shopId=SHOP_001"

# Get order stats
curl "http://localhost:8080/api/orders/stats?shopId=SHOP_001"

# Update order status
curl -X PUT http://localhost:8080/api/orders/ORDER_ID/status \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{"status": "ACCEPTED"}'
```

## 6. Connecting Real WhatsApp

1. Create a Meta Business account
2. Set up WhatsApp Business API
3. Configure webhook URL: `https://your-domain.com/webhook`
4. Set environment variables:

```bash
export WHATSAPP_TOKEN=your_token
export WHATSAPP_VERIFY_TOKEN=your_verify_token
export WHATSAPP_PHONE_ID=your_phone_id
```

## 7. Deployment Checklist

- [ ] Set up production database
- [ ] Configure Redis for caching
- [ ] Set strong JWT_SECRET
- [ ] Configure HTTPS
- [ ] Set up WhatsApp Business API
- [ ] Configure Razorpay for payments
- [ ] Set up monitoring (logs, metrics)

## Troubleshooting

### Database Connection Error

```bash
# Check if PostgreSQL is running
docker-compose ps

# View logs
docker-compose logs postgres
```

### Port Already in Use

```bash
# Find process using port 8080
lsof -i :8080

# Kill if needed
kill -9 <PID>
```

### Flutter Build Issues

```bash
# Clean and rebuild
flutter clean
flutter pub get
flutter run
```
