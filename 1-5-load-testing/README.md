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

Useful Gatling docs (quick lookup):
- Feeders: https://docs.gatling.io/concepts/session/feeders/
- Expression Language (EL): https://docs.gatling.io/reference/script/core/session/el/
- HTTP requests: https://docs.gatling.io/reference/script/http/
- HTTP checks: https://docs.gatling.io/reference/script/http/checks/
- Injection profiles (load model): https://docs.gatling.io/reference/script/core/injection/
- Assertions (SLA): https://docs.gatling.io/concepts/assertions/


Recommended flow:
1) Run a baseline on the starter branch (`1-5-load-testing`).
2) Implement the TODOs in Gatling.
3) Run again and compare reports/metrics (or compare against the completed branch).


## 2. How to start the service

Prereqs: Java 21+, Maven 3.9+, Docker.

Via IDE:

1) Run 1-5-load-testing/shop-service/docker-compose.yml
2) Run 1-5-load-testing/shop-service/src/main/java/org/example/shopservice/ShopServiceApplication.java

Via terminal:

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

## 3. How to run Gatling

Via IDE:
Run 1-5-load-testing/shop-load-tests/src/test/java/org/example/shop/loadtests/LoadTestLauncher.java

Via terminal:
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

## 4. What you need to implement

Follow the 7 steps below to incrementally evolve your load tests from a single request baseline to a full production-like scenario with parallel user flows, dynamic feeders, and SLA validations.

### Class: `BasicShopScenario`

#### 1) Step 1: Extend the Scenario
Extend the scenario by adding a product details request: `GET /api/products/{id}`. For now, use a hardcoded product ID (e.g. `1` or `2`, which are pre-seeded in the database).
<details>
<summary>💡 Hint: How to add another request</summary>

In Gatling, you can chain multiple `.exec()` methods on a scenario builder. Add a new `.exec(http(...))` block targeting the product details endpoint:
```java
    public static ScenarioBuilder build() {
        return scenario(SCENARIO_NAME)
                .exec(
                        http("GET /api/products")
                                .get("/api/products")
                                .check(status().is(200))
                )
                .exec(
                        http("GET /api/products/{id}")
                                .get("/api/products/1") // Hardcoded ID for now
                                .check(status().is(200))
                );
    }
```
</details>

#### 2) Step 2: Add Think Time (Pauses)
Introduce realistic "think time" (pauses of 1 to 3 seconds) between the requests. Real users do not click buttons instantly, and adding think time is crucial for realistic traffic distribution.
<details>
<summary>💡 Hint: Adding pauses in Gatling</summary>

You can use the `.pause(min, max)` method to inject a random delay (in seconds) between two execution steps:
```java
                .exec(
                        http("GET /api/products")
                                .get("/api/products")
                                .check(status().is(200))
                )
                .pause(1, 3) // Pauses randomly between 1 and 3 seconds
                .exec(
                        http("GET /api/products/{id}")
                                .get("/api/products/1")
                                .check(status().is(200))
                );
```
</details>

#### 3) Step 3: Use Dynamic Data (Feeders)
Use the JSON feeder file `feeders/products.json` to replace the hardcoded product ID in your URL and validation checks. Validate that the product ID, name, and price returned in the JSON response match the values provided by the feeder.
<details>
<summary>💡 Hint: Feeders and Expression Language (EL)</summary>

1. **Load the feeder**: Set up the feeder using `jsonFile(...)` and define its strategy (e.g., `.circular()` to loop through the test data):
   ```java
   public static FeederBuilder<?> productFeeder() {
       return jsonFile("feeders/products.json").circular();
   }
   ```
2. **Inject the feeder**: Call `.feed(...)` right at the start of your scenario builder.
3. **Use session attributes in URL/Body**: Reference parameters using Expression Language syntax `#{name}`:
   ```java
   .get("/api/products/#{productId}")
   ```
4. **Validate attributes in checks**: Check response data dynamically:
   ```java
   .check(status().is(200))
   .check(jsonPath("$.id").isEL("#{productId}"))
   .check(jsonPath("$.name").isEL("#{name}"))
   .check(jsonPath("$.price").ofDouble().is(session -> session.getDouble("price")))
   ```
</details>

#### 4) Step 4: Split into Browse and Checkout Flows
Refactor the scenario builder to expose two separate, distinct user journeys:
* **`public static ScenarioBuilder browseCatalog(FeederBuilder<?> productFeeder)`**
  * Lists all products, pauses, and views the details of a specific product (reusing the flow you built in Steps 1–3).
* **`public static ScenarioBuilder checkoutFlow(FeederBuilder<?> productFeeder)`**
  * Views details of a specific product, pauses, places an order (`POST /api/orders`), pauses, and fetches the created order (`GET /api/orders/{id}`).
  * Ensure you correlate the order ID between the creation response and the fetching request using `.saveAs("orderId")`.

> [!TIP]
> **What about the `build()` method?**
> To keep the project compiling at this step, refactor the `build()` method to simply return `browseCatalog(productFeeder())`. 
> Later, in Step 6, the simulation class will call the two new scenario methods directly, making `build()` obsolete (though it can remain in the class as a default fallback).

<details>
<summary>💡 Hint: Correlation and POST requests</summary>

1. **Extracting variables**: Extract values from a response body and save them into session state using `saveAs("key")`:
   ```java
   .exec(
       http("POST /api/orders")
           .post("/api/orders")
           .body(StringBody("{\"productId\": #{productId}, \"quantity\": #{quantity}}"))
           .check(status().is(201))
           .check(jsonPath("$.id").saveAs("orderId"))
   )
   ```
2. **Retrieving by extracted ID**: Retrieve the created order using the saved session variable:
   ```java
   .exec(
       http("GET /api/orders/{id}")
           .get("/api/orders/#{orderId}")
           .check(status().is(200))
   )
   ```
</details>

---

### Class: `BasicShopSimulation`

#### 5) Step 5: Improve the Load Profile
In `BasicShopSimulation.java`, change the default injection profile to a multi-phase profile representing a realistic workload progression. It should consist of:
* A **warm-up** ramp-up phase.
* A **steady load** constant phase.
* A short **spike/stress** peak load phase.
<details>
<summary>💡 Hint: Workload injection DSL</summary>

You can chain multiple injection profiles inside `injectOpen()` using `rampUsersPerSec(...)` and `constantUsersPerSec(...)`:
```java
        setUp(
            browseCatalog.injectOpen(
                rampUsersPerSec(1).to(6).during(Duration.ofSeconds(40)),
                constantUsersPerSec(6).during(Duration.ofSeconds(60)),
                constantUsersPerSec(14).during(Duration.ofSeconds(10))
            )
        )
```
</details>

#### 6) Step 6: Run Scenarios in Parallel
Configure the simulation setup to run both `browseCatalog` and `checkoutFlow` scenarios in parallel. They should execute concurrently with different traffic volumes (mix):
* **`browseCatalog` scenario profile:**
  * Warm-up: ramp from **1 to 6 users/sec** over **40s**
  * Steady load: **6 users/sec** for **60s**
  * Spike load: **14 users/sec** for **10s**
* **`checkoutFlow` scenario profile:**
  * Warm-up: ramp from **1 to 2 users/sec** over **40s**
  * Steady load: **2 users/sec** for **60s**
  * Spike load: **5 users/sec** for **10s**
<details>
<summary>💡 Hint: Concurrency in setUp</summary>

You can pass multiple scenarios to the `setUp(...)` method by comma-separating their scenario-to-injection configs:
```java
        setUp(
            browseCatalog.injectOpen(...),
            checkoutFlow.injectOpen(...)
        ).protocols(httpProtocol)
```
</details>

#### 7) Step 7: Add SLA Assertions
Add Gatling assertions to ensure your load tests act as regression gates. The run must fail if the system metrics do not satisfy the baseline targets:
* **95th percentile response time (latency) < 900 ms**
* **Total failed request percentage (KO) <= 0.5%**
<details>
<summary>💡 Hint: SLA Assertions</summary>

Configure global assertions at the end of the `setUp(...)` chain:
```java
        setUp(...)
            .protocols(httpProtocol)
            .assertions(
                global().responseTime().percentile(95).lt(900),
                global().failedRequests().percent().lte(0.5)
            );
```
</details>

This README focuses on goals + run instructions; in the starter branch (`1-5-load-testing`) the TODOs in `BasicShopScenario` and `BasicShopSimulation` are checkpoints of what to implement next.

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
