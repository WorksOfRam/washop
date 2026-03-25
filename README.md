# WA Shop - WhatsApp Commerce SaaS

A platform enabling small businesses to sell products directly through WhatsApp.

## Architecture

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│  WhatsApp Bot   │     │   Flutter App   │     │  Admin Panel    │
│  (Customers)    │     │  (Shop Owners)  │     │   (Future)      │
└────────┬────────┘     └────────┬────────┘     └────────┬────────┘
         │                       │                       │
         └───────────────────────┼───────────────────────┘
                                 │
                    ┌────────────▼────────────┐
                    │   Ktor Backend (REST)   │
                    └────────────┬────────────┘
                                 │
              ┌──────────────────┼──────────────────┐
              │                  │                  │
     ┌────────▼────────┐ ┌──────▼──────┐ ┌────────▼────────┐
     │   PostgreSQL    │ │    Redis    │ │    Razorpay     │
     └─────────────────┘ └─────────────┘ └─────────────────┘
```

## Project Structure

```
WhatsappShop/
├── backend/           # Ktor REST API
├── flutter_app/       # Shop Owner Dashboard
├── database/          # SQL schemas
├── docs/              # Documentation
└── docker-compose.yml # Local development
```

## Quick Start

### Prerequisites
- JDK 17+
- Docker & Docker Compose
- Flutter 3.x
- PostgreSQL 15+

### Backend Setup

```bash
cd backend
./gradlew run
```

### Flutter App Setup

```bash
cd flutter_app
flutter pub get
flutter run
```

### Docker (Full Stack)

```bash
docker-compose up -d
```

## API Endpoints

| Module | Endpoint | Method | Description |
|--------|----------|--------|-------------|
| Auth | `/api/auth/login` | POST | Phone-based login |
| Auth | `/api/auth/verify` | POST | Verify OTP |
| Products | `/api/products` | GET | List products |
| Products | `/api/products` | POST | Add product |
| Products | `/api/products/{id}` | PUT | Update product |
| Products | `/api/products/{id}` | DELETE | Delete product |
| Cart | `/api/cart` | GET | Get cart |
| Cart | `/api/cart/add` | POST | Add to cart |
| Cart | `/api/cart/remove` | POST | Remove from cart |
| Orders | `/api/orders` | GET | List orders |
| Orders | `/api/orders` | POST | Create order |
| Orders | `/api/orders/{id}` | GET | Order detail |
| Orders | `/api/orders/{id}/status` | PUT | Update status |
| WhatsApp | `/webhook` | POST | WhatsApp webhook |

## Environment Variables

```env
# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=washop
DB_USER=postgres
DB_PASSWORD=postgres

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# WhatsApp
WHATSAPP_TOKEN=your_token
WHATSAPP_VERIFY_TOKEN=your_verify_token
WHATSAPP_PHONE_ID=your_phone_id

# JWT
JWT_SECRET=your_secret_key

# Razorpay
RAZORPAY_KEY_ID=your_key
RAZORPAY_KEY_SECRET=your_secret
```

## License

MIT
