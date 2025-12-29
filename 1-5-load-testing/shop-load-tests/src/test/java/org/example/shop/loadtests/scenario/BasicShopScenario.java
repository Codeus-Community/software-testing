package org.example.shop.loadtests.scenario;

import io.gatling.javaapi.core.ScenarioBuilder;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

public class BasicShopScenario {

    public static final String SCENARIO_NAME = "Basic shop scenario";

    /**
     * Step 1 (already implemented):
     *  - single GET /api/products request that must return 200.
     *
     * TODO roadmap for the workshop:
     *  - TODO-1: add realistic "think time" between user actions (pause 1–3 seconds; avoid back-to-back requests).
     *  - TODO-2: extend the flow with product details (GET /api/products/{id}) and validate response structure.
     *  - TODO-3: use external test data (feeder) for productId/quantity/name/price (see src/test/resources/feeders/products.json)
     *      and use it consistently in URLs, bodies and checks.
     *  - TODO-4: split into at least two user journeys:
     *      - browse (catalog + details),
     *      - checkout (includes order creation + reading the created order, with proper correlation).
     */
    public static ScenarioBuilder build() {
        return scenario(SCENARIO_NAME)
            .exec(
                http("GET /api/products")
                    .get("/api/products")
                    .check(status().is(200))
            );
    }
}
