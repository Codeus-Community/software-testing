package org.example.shop.loadtests.scenario;

import io.gatling.javaapi.core.FeederBuilder;
import io.gatling.javaapi.core.ScenarioBuilder;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

public class BasicShopScenario {

    public static final String BROWSE_SCENARIO_NAME = "Browse catalog";
    public static final String CHECKOUT_SCENARIO_NAME = "Checkout order";

    /**
     * Step 1 (already implemented):
     *  - single GET /api/products request that must return 200.
     *
     * TODO roadmap for the workshop:
     *  - TODO-1: add realistic "think time" between user actions (pause 1–3 seconds; avoid back-to-back requests).
     *  - TODO-2: extend the flow with product details (GET /api/products/{id}) and validate response structure.
     *  - TODO-3: remove hardcoded values by introducing external test data (feeder) and using it consistently in the flow.
     *  - TODO-4: split into at least two user journeys:
     *      - browse (catalog + details),
     *      - checkout (includes order creation + reading the created order, with proper correlation).
     */
    public static ScenarioBuilder build() {
        return browseCatalog(productFeeder());
    }

    public static FeederBuilder<?> productFeeder() {
        return jsonFile("feeders/products.json").circular();
    }

    public static ScenarioBuilder browseCatalog(FeederBuilder<?> productFeeder) {
        return scenario(BROWSE_SCENARIO_NAME)
            .feed(productFeeder)
            .exec(
                http("GET /api/products")
                    .get("/api/products")
                    .check(status().is(200))
                    .check(jsonPath("$[0].id").exists())
            )
            .pause(1, 3)
            .exec(
                http("GET /api/products/{id}")
                    .get("/api/products/#{productId}")
                    .check(status().is(200))
                    .check(jsonPath("$.id").isEL("#{productId}"))
                    .check(jsonPath("$.name").isEL("#{name}"))
                    .check(jsonPath("$.price").ofDouble().is(session -> session.getDouble("price")))
            )
            .pause(1, 3);
    }

    public static ScenarioBuilder checkoutFlow(FeederBuilder<?> productFeeder) {
        return scenario(CHECKOUT_SCENARIO_NAME)
            .feed(productFeeder)
            .exec(
                http("GET /api/products/{id}")
                    .get("/api/products/#{productId}")
                    .check(status().is(200))
                    .check(jsonPath("$.id").isEL("#{productId}"))
            )
            .pause(1, 3)
            .exec(
                http("POST /api/orders")
                    .post("/api/orders")
                    .body(StringBody("{\"productId\": #{productId}, \"quantity\": #{quantity}}"))
                    .check(status().is(201))
                    .check(jsonPath("$.id").saveAs("orderId"))
                    .check(jsonPath("$.productId").isEL("#{productId}"))
                    .check(jsonPath("$.quantity").isEL("#{quantity}"))
                    .check(jsonPath("$.createdAt").exists())
            )
            .pause(1, 3)
            .exec(
                http("GET /api/orders/{id}")
                    .get("/api/orders/#{orderId}")
                    .check(status().is(200))
                    .check(jsonPath("$.id").isEL("#{orderId}"))
                    .check(jsonPath("$.productId").isEL("#{productId}"))
                    .check(jsonPath("$.quantity").isEL("#{quantity}"))
            )
            .pause(1, 3);
    }
}
