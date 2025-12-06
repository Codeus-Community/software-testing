# Load Testing – Shop Service

Short module to practice writing Gatling load tests and reading reports/metrics on a real service.

## 1. Module overview

- `shop-service` – simple Spring Boot service backed by PostgreSQL:
  - `GET /api/products` – list of products;
  - `GET /api/products/{id}` – product details;
  - `POST /api/orders` – create an order;
  - `GET /api/orders/{id}` – get an order.
- `shop-load-tests` – Gatling (Java DSL) tests for this service:
  - `BasicShopScenario` – scenarios/user journeys (starter branch begins with a single `GET /api/products`);
  - `BasicShopSimulation` – simulation and load profile (starter branch begins with a low load baseline).
- Docker infra:
  - Postgres + Prometheus + Grafana (the `Shop Service Overview` dashboard is preconfigured).

This module is provided in two git branches:

- `1-5-load-testing` – starter version (minimal scenario + numbered TODOs to implement).
- `1-5-load-testing-completed` – reference solution (implemented scenario, load profile, and assertions).

In the starter branch (`1-5-load-testing`) the load test is intentionally minimal:

- `BasicShopScenario`:
  - one `GET /api/products` request;
  - no pauses, no feeders, no checkout flow.
- `BasicShopSimulation`:
  - profile: `atOnceUsers(1)` + `rampUsers(5).during(10s)`;
  - no SLA assertions;
  - HTTP protocol is created in `HttpProtocolFactory`.

The Gatling classes contain numbered TODO comments (`TODO-1` .. `TODO-7`) that guide how to evolve the scenario.

## 2. What you need to implement

**Class: `BasicShopScenario`**

1) **Extend the scenario and add think time**
- after `GET /api/products` add:
  - product details request `GET /api/products/{id}`;
  - order creation `POST /api/orders`;
  - fetching the created order `GET /api/orders/{id}`;
- add think time (pauses) between steps: **1–3 seconds**.

2) **Use dynamic data**
- replace hardcoded IDs with a feeder (JSON/CSV) that provides `productId` / `name` / `price` / `quantity` (a starter JSON feeder is already included);
- use feeder data in URLs, request bodies and response validations (checks).

3) **Split user flows**
- create separate scenarios:
  - “browse catalog” (only product listing/details);
  - “checkout order” (view product + create/read order);
- run these scenarios in parallel in the simulation with different load profiles.

**Class: `BasicShopSimulation`**

4) **Improve the load profile**
- replace the simple `atOnce/rampUsers` with a multi‑phase workload (use these values as a baseline):
  - warm‑up (ramp): from **1 user/s** to **6 users/s** during **40s**;
  - steady load: **6 users/s** during **60s**;
  - spike / stress: **14 users/s** during **10s**.
  - note: “users/s” means `rampUsersPerSec` / `constantUsersPerSec`; `stressPeakUsers(n)` injects **n total** users over the duration.

5) **Add basic SLAs**
- add Gatling assertions (baseline targets):
  - global p95 latency **< 900 ms**;
  - global error percentage (KO) **<= 0.5%**;
- so that a run fails if the service performance regresses.

This README focuses on goals + run instructions; in the starter branch (`1-5-load-testing`) the TODOs in `BasicShopScenario` and `BasicShopSimulation` are checkpoints of what to implement next.

## 3. How to start the service

Prereqs: Java 21+, Maven 3.9+, Docker.

1) Start infra (Postgres + Prometheus + Grafana):
```bash
cd 1-5-load-testing/shop-service
docker-compose up -d
```

2) Run the Spring Boot service:
```bash
mvn spring-boot:run
```
- App URL: `http://localhost:8080`
- Metrics URL (Prometheus scrape target): `http://localhost:8081/actuator/prometheus`
- REST API: see `ShopController`.

3) Quick smoke check:
```bash
curl http://localhost:8080/api/products
```

## 4. How to run Gatling

From `1-5-load-testing/shop-load-tests`:
```bash
mvn -q -DskipTests test-compile dependency:build-classpath -Dmdep.outputFile=/tmp/shop-load-tests.cp
java -cp "target/test-classes:$(cat /tmp/shop-load-tests.cp)" \
  -DbaseUrl=http://localhost:8080 \
  org.example.shop.loadtests.LoadTestLauncher
```
- By default `BasicShopSimulation` is executed.
- Results: `target/gatling/<run-id>/index.html`.
- You can override the simulation: `-DsimulationClass=...`.

Recommended flow:
1) Run a baseline on the starter branch (`1-5-load-testing`).
2) Implement the TODOs in Gatling.
3) Run again and compare reports/metrics (or compare against the completed branch).

## 5. What to look at in the Gatling report

- **Global information**
  - Requests/s (throughput);
  - percentage of failed requests (KO).
- **Response time percentiles**
  - p50 / p95 / p99 – how latency changes as the scenario becomes more complex;
  - compare baseline vs updated scenario.
- **Per request stats**
  - separate stats for `GET /api/products`, `GET /api/products/{id}`, `POST /api/orders`, `GET /api/orders/{id}`;
  - where slow requests or errors appear.

## 6. What to look at in Grafana

Dashboard: `Shop Service Overview` (Grafana: `http://localhost:3000`).
Login: `admin` / `admin`.

Key panels:
- HTTP p95 latency – whether you stay within the target threshold under different load profiles.
- Requests/s – how throughput changes as you change the scenario and profile.
- JVM heap + GC – any memory leaks or frequent GC pauses.
- DB connections (HikariCP) – whether you are saturating the connection pool.
- Error rate (5xx) – whether server errors appear under load.
