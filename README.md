# Auto Trading Bot

## Requirements
- Java 21+
- Node.js 18+
- PostgreSQL 13+ (or compatible relational database)
- Access to a public crypto market data API (Binance public REST endpoints, no API key required)

## Setup

### Database
1. Create a PostgreSQL database (example name: `backend`)
2. Run the schema script:
   ```bash
   psql -U postgres -d backend -f backend/db/init.sql
   ```

### Backend (Spring Boot + Gradle)
1. Configure database credentials in:
   backend/src/main/resources/application.yml

   Example:
   ```yaml
   spring:
     datasource:
       url: jdbc:postgresql://localhost:5432/backend
       username: postgres
       password: postgres
   ```

2. Build the backend:
   ```bash
   cd backend
   ./gradlew build
   ```

### Frontend (React)
1. Install dependencies:
   ```bash
   cd frontend
   npm install
   ```
   
## Run Instructions

### Start Backend
```bash
cd backend
./gradlew bootRun
```

Backend runs at: http://localhost:8080

### Start Frontend
```bash
cd frontend
npm run dev
```

Frontend runs at: http://localhost:5173