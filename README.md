# Split-Payment Checkout

A customer pays for one order with two cards. Two Spring Boot services, each with its own PostgreSQL database, communicate through Kafka events. A React + TypeScript front end lets the customer shop, split the total across two cards and watch the order status. Everything can run locally or in a Kubernetes cluster (kind).

| Service | Port | Database (Docker) |
|---|---|---|
| order-service | 8081 | `orders` on localhost:5433 |
| payment-service | 8082 | `payments` on localhost:5434 |
| Kafka broker | 9092 | |
| frontend (Vite dev server) | 5173 | |

## How it works

1. `POST /orders` saves the order as `PENDING_PAYMENT` and, once committed, publishes **OrderPlaced** to the `orders.placed` topic.
2. payment-service consumes it and charges card A, then card B (simulated provider).
   - Both approved: publishes **COMPLETED** to `payment.results`.
   - Card B declined: refunds card A (compensating action) and publishes **FAILED**.
   - Card A declined: card B is never charged; publishes **FAILED**.
3. order-service consumes the result and sets the order to `PAID` or `PAYMENT_FAILED`.

**Test cards (last 4 digits):** `0002` is declined; `9999` simulates the payment provider being down; anything else is approved.

**Retries and dead-letter topic:** if processing throws a technical error (for example, provider down), payment-service retries the message 3 times, 2 seconds apart, then publishes it to `orders.placed.DLT` so it doesn't block later messages. A declined card is a normal business outcome, not an error, so it isn't retried.

**Idempotency:** payment-service never processes the same order twice, and order-service ignores repeated results, because Kafka can deliver a message more than once.

## Run

1. Start Postgres and Kafka: `docker compose up -d`
2. Start both services: `mvn spring-boot:run` in `order-service` and in `payment-service` (separate terminals).
3. Run the tests: `mvn test` in each service folder (no Docker needed; they use Mockito).

## Try it (from the project folder)

```
curl -X POST http://localhost:8081/orders -H "Content-Type: application/json" -d @requests/order-success.json
curl http://localhost:8081/orders/1
curl "http://localhost:8082/payments?orderId=1"
```

The order should change from `PENDING_PAYMENT` to `PAID` within a second or two.

Other cases:
- `requests/order-card-b-declined.json`: order becomes `PAYMENT_FAILED`; payments show card A `REFUNDED`, card B `FAILED`.
- `requests/order-provider-down.json`: watch payment-service's log show 3 retries, then the message goes to the dead-letter topic; the order stays `PENDING_PAYMENT`.
- `requests/order-bad-split.json`: rejected straight away with `400 Bad Request`.

See what's in the dead-letter topic:

```
docker compose exec kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic orders.placed.DLT --from-beginning
```

(Press Ctrl+C to stop.)

## Front end (React + TypeScript)

Pages: product list, cart, checkout (card A amount is entered; card B pays the rest) and an order status page that checks the order every 2 seconds until it is `PAID` or `PAYMENT_FAILED`.

With Postgres, Kafka and both services running (see **Run**):

```
cd frontend
npm install
npm run dev
```

Open http://localhost:5173. The Vite dev server forwards `/api/orders` to port 8081 and `/api/payments` to port 8082, so the browser only talks to one address and the services need no CORS settings. Use test card `0002` or `9999` at checkout to see a declined payment or the provider-down case.

## Run in Kubernetes (kind)

Needs Docker Desktop (give it at least 6 GB of memory), [kind](https://kind.sigs.k8s.io/) and kubectl. Run from the project folder:

```
kind create cluster --name splitpay --config kind-config.yaml
docker build -t splitpay/order-service:local order-service
docker build -t splitpay/payment-service:local payment-service
docker build -t splitpay/frontend:local frontend
kind load docker-image splitpay/order-service:local splitpay/payment-service:local splitpay/frontend:local --name splitpay
kubectl apply -f k8s/
kubectl get pods -n splitpay -w
```

When every pod shows `1/1 Running` (the services may restart once or twice while Postgres and Kafka start), open http://localhost:8080.

| File | What it creates |
|---|---|
| `kind-config.yaml` | A one-node cluster; maps the front end's NodePort 30080 to localhost:8080 |
| `k8s/01-config.yaml` | ConfigMap (Kafka and database addresses) and Secret (database credentials) |
| `k8s/10-databases.yaml` | A Deployment and Service for each PostgreSQL database |
| `k8s/11-kafka.yaml` | Single-node Kafka (KRaft) |
| `k8s/20-…`, `k8s/21-…` | Deployment and Service for each Spring Boot service |
| `k8s/30-frontend.yaml` | nginx serving the React app and forwarding `/api/...` to the services |

Useful commands:

```
kubectl logs -n splitpay deploy/payment-service -f
kubectl exec -n splitpay deploy/kafka -- /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic orders.placed.DLT --from-beginning
kind delete cluster --name splitpay
```

After changing code, rebuild the image, run `kind load docker-image ...` again, then `kubectl rollout restart -n splitpay deploy/<name>`.

## Design notes

- Money uses `BigDecimal`, compared with `compareTo` (not `equals`, which treats 10.0 and 10.00 as different).
- Only the last 4 digits of each card are stored, never the full number.
- Each service owns its own copy of the event classes and sends plain JSON (no Java type headers), so the services stay independent.
- Events are keyed by order ID, so all events for one order stay in order on the same partition.
- OrderPlaced is sent after the database commit. A production system would use the transactional outbox pattern so an event can't be lost if the app crashes between the commit and the send.
- `ddl-auto: update` creates the tables automatically; a production system would use migrations (Flyway or Liquibase).
- Settings that differ between environments (database and Kafka addresses) come from environment variables, which Spring Boot maps onto `application.yml` properties; in Kubernetes they come from a ConfigMap and a Secret.
- The front end works in whole cents, so splitting a total never produces rounding errors.
- Images are multi-stage builds: Maven or Node builds the app, and only the runtime (JRE or nginx) goes into the final image, which runs as a non-root user where possible.
