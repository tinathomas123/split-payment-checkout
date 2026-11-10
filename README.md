# Split-Payment Checkout (Day 1)

Two Spring Boot services in one repository, each with its own PostgreSQL database.

| Service | Port | Database (Docker) |
|---|---|---|
| order-service | 8081 | `orders` on localhost:5433 |
| payment-service | 8082 | `payments` on localhost:5434 |

## Run

1. Start the databases: `docker compose up -d`
2. Start each service (in IntelliJ, or in a terminal):
   - `cd order-service && mvn spring-boot:run`
   - `cd payment-service && mvn spring-boot:run`
3. Run the tests: `mvn test` inside each service folder.

## Try it

Create an order ($100 total, split $60 / $40):

```bash
curl -X POST http://localhost:8081/orders -H "Content-Type: application/json" -d '{
  "items": [
    {"productName": "Headphones", "quantity": 2, "unitPrice": 25.00},
    {"productName": "Backpack", "quantity": 1, "unitPrice": 50.00}
  ],
  "cardAAmount": 60.00,
  "cardBAmount": 40.00
}'
```

Get its status: `curl http://localhost:8081/orders/1`

A split that doesn't add up (60 + 30) returns `400 Bad Request` with a clear message.

Record the split payment (Day 1 manual test; from Day 2 this will come from a Kafka event):

```bash
curl -X POST http://localhost:8082/payments -H "Content-Type: application/json" -d '{
  "orderId": 1, "orderTotal": 100.00,
  "cardALast4": "4242", "cardAAmount": 60.00,
  "cardBLast4": "1881", "cardBAmount": 40.00
}'
```

See the payment rows: `curl "http://localhost:8082/payments?orderId=1"`

## Design notes

- Money uses `BigDecimal`, compared with `compareTo` (not `equals`, which treats 10.0 and 10.00 as different).
- Only the last 4 digits of each card are stored, never the full number.
- payment-service refuses to record payments twice for the same order (idempotency), which matters once Kafka can redeliver events.
- `ddl-auto: update` creates the tables automatically; a production system would use migrations (Flyway or Liquibase).
