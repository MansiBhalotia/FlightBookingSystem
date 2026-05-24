# Flight Booking System

A REST API for flight ticket booking built with **Spring Boot**, featuring Optimistic Concurrency Control (OCC), fair FCFS locking, and atomic status transitions to prevent double-booking under concurrent load.

---

## Tech Stack

| | |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.2.5 |
| Build | Maven |
| Storage | In-memory (`ConcurrentHashMap`) |
| Concurrency | `ReentrantLock(fair=true)`, `AtomicReference`, OCC versioning |

---

## Local Setup

### Prerequisites

- Java 17 or higher (`java -version`)
- Maven 3.8 or higher (`mvn -version`)

### Steps

**1. Clone the repository**
```bash
git clone <your-repo-url>
cd FlightBookingSystem
```

**2. Build the project**
```bash
mvn clean install
```

**3. Run the application**
```bash
mvn spring-boot:run
```

Server starts at **`http://localhost:8080`**

**4. Run tests**
```bash
mvn test
```

---

## Seeded Data

Three flights are available immediately on startup — no setup required:

| Flight | Origin | Destination | Seats |
|--------|--------|-------------|-------|
| `AA101` | New York (JFK) | Los Angeles (LAX) | 150 |
| `BA202` | London (LHR) | Paris (CDG) | 80 |
| `LH303` | Frankfurt (FRA) | Tokyo (NRT) | 200 |

---

## API Reference

### Flights

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/flights` | Register a new flight |
| `GET` | `/api/flights` | List all flights |
| `GET` | `/api/flights/{flightNumber}` | Get a specific flight (includes current `version`) |

### Bookings

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/bookings` | Book a flight — returns `CONFIRMED` in one step |
| `DELETE` | `/api/bookings/{bookingId}` | Cancel a booking and release seats |

---

## Curl Examples

### 1. Get All Flights
```bash
curl -X GET http://localhost:8080/api/flights
```

---

### 2. Get a Specific Flight
```bash
curl -X GET http://localhost:8080/api/flights/AA101
```

**Response `200 OK`:**
```json
{
  "flightNumber": "AA101",
  "origin": "New York (JFK)",
  "destination": "Los Angeles (LAX)",
  "departureTime": "2026-05-25T10:00:00",
  "totalSeats": 150,
  "availableSeats": 150,
  "version": 0
}
```

> ⚠️ Note the `version` value — you **must** pass it in the booking request.

---

### 3. Register a New Flight
```bash
curl -X POST http://localhost:8080/api/flights \
  -H "Content-Type: application/json" \
  -d '{
    "flightNumber": "EK500",
    "origin": "Dubai (DXB)",
    "destination": "Sydney (SYD)",
    "departureTime": "2026-06-15T22:00:00",
    "totalSeats": 300
  }'
```

**Response `201 Created`:**
```json
{
  "flightNumber": "EK500",
  "origin": "Dubai (DXB)",
  "destination": "Sydney (SYD)",
  "departureTime": "2026-06-15T22:00:00",
  "totalSeats": 300,
  "availableSeats": 300,
  "version": 0
}
```

---

### 4. Book a Flight (Single Passenger)

**Step 1** — Read the current flight version:
```bash
curl -X GET http://localhost:8080/api/flights/AA101
# note the "version" field from the response
```

**Step 2** — Submit the booking with that version:
```bash
curl -X POST http://localhost:8080/api/bookings \
  -H "Content-Type: application/json" \
  -d '{
    "flightNumber": "AA101",
    "flightVersion": 0,
    "passengers": [
      {
        "firstName": "John",
        "lastName": "Doe",
        "passportNumber": "P1234567"
      }
    ]
  }'
```

**Response `201 Created`:**
```json
{
  "bookingId": "3f2a1b4c-8e9d-4f7a-b2c1-1a2b3c4d5e6f",
  "flightNumber": "AA101",
  "origin": "New York (JFK)",
  "destination": "Los Angeles (LAX)",
  "departureTime": "2026-05-25T10:00:00",
  "status": "CONFIRMED",
  "bookedAt": "2026-05-24T14:00:00",
  "passengers": [
    {
      "firstName": "John",
      "lastName": "Doe",
      "passportNumber": "P1234567"
    }
  ]
}
```

---

### 5. Book a Flight (Multiple Passengers)
```bash
curl -X POST http://localhost:8080/api/bookings \
  -H "Content-Type: application/json" \
  -d '{
    "flightNumber": "AA101",
    "flightVersion": 1,
    "passengers": [
      {
        "firstName": "Jane",
        "lastName": "Smith",
        "passportNumber": "P9876543"
      },
      {
        "firstName": "Bob",
        "lastName": "Jones",
        "passportNumber": "P1112223"
      }
    ]
  }'
```

---

### 6. Cancel a Booking
```bash
curl -X DELETE http://localhost:8080/api/bookings/3f2a1b4c-8e9d-4f7a-b2c1-1a2b3c4d5e6f
```

**Response `200 OK`:**
```json
{
  "bookingId": "3f2a1b4c-8e9d-4f7a-b2c1-1a2b3c4d5e6f",
  "flightNumber": "AA101",
  "origin": "New York (JFK)",
  "destination": "Los Angeles (LAX)",
  "departureTime": "2026-05-25T10:00:00",
  "status": "CANCELLED",
  "bookedAt": "2026-05-24T14:00:00",
  "passengers": [...]
}
```

---

## Error Responses

All errors follow this structure:
```json
{
  "status": 409,
  "error": "Conflict",
  "message": "...",
  "timestamp": "2026-05-24T14:00:00"
}
```

| HTTP Status | Scenario |
|-------------|----------|
| `400 Bad Request` | Missing or invalid fields (e.g. no `flightVersion`, empty passengers) |
| `404 Not Found` | Flight or booking does not exist |
| `409 Conflict` | Duplicate flight, no seats available, stale version, already cancelled |

---

## Internal Booking Flow

```
User clicks "Book"  →  POST /api/bookings
         │
         ├─ 1. Validate request fields
         │
         ├─ 2. Look up flight (404 if not found)
         │
         ├─ 3. reserveSeats(count, expectedVersion)
         │       ├─ Acquire fair ReentrantLock  (FCFS ordering)
         │       ├─ OCC check: version matches? (409 if stale)
         │       ├─ Seats available?             (409 if full)
         │       └─ Decrement seats, bump version, release lock
         │
         ├─ 4. Save Booking (status = PENDING)
         │       └─ On failure → releaseSeats() + rethrow
         │
         ├─ 5. CAS: PENDING → CONFIRMED
         │       └─ On failure → releaseSeats() + cancel booking + rethrow
         │
         └─ 6. Return 201 { "status": "CONFIRMED" }
```

---

## Concurrency Design

| Mechanism | Purpose |
|-----------|---------|
| `ReentrantLock(fair=true)` | Threads served in arrival order — guarantees FCFS, no starvation |
| OCC `version` field on `Flight` | Rejects bookings made on stale flight data (browsing user scenario) |
| `AtomicReference<Status>` + CAS | Atomic status transitions — prevents double-confirm or double-cancel races |
| Compensation on failure | Any mid-flow exception releases reserved seats and removes orphan bookings |

---

## Booking Status Lifecycle

```
  [Created]
      │
      ▼
  PENDING  ──── CAS ────▶  CONFIRMED
      │                        │
      └──────── CAS ───────────┘
                    │
                    ▼
               CANCELLED
             (seats released)
```

