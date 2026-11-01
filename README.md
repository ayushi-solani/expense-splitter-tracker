# Expense Splitter & Tracker

Group expense splitting (itemized bills, multi-currency, debt simplification)
combined with personal expense tracking (category breakdown, spending
patterns, ML-based forecasting).

## Structure

- `backend/` — Spring Boot REST API (auth, groups, expenses, settlements)
- `ml-service/` — Flask + scikit-learn microservice for spend forecasting
- `frontend/` — React + Vite client

## Running locally

**1. MySQL**
Create a database named `expense_tracker` (see `backend/src/main/resources/application.properties`).

**2. Backend**
```
cd backend
./mvnw spring-boot:run
```
Runs on `http://localhost:8080`.

**3. ML service**
```
cd ml-service
python -m venv venv
source venv/bin/activate
pip install -r requirements.txt
python app.py
```
Runs on `http://localhost:5000`.

**4. Frontend**
```
cd frontend
npm install
npm run dev
```
Runs on `http://localhost:5173`, proxies `/api` to the backend.

## Key pieces to point to in a demo

- `backend/.../service/DebtSimplificationService.java` — greedy max-heap
  algorithm that reduces group balances to the minimum number of settlement
  transactions.
- `ml-service/model.py` — linear regression forecast with explicit
  cold-start handling for users with under 3 months of data.
