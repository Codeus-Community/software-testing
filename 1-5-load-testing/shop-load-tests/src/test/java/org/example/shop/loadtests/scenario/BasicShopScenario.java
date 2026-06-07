package org.example.shop.loadtests.scenario;

import io.gatling.javaapi.core.ScenarioBuilder;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

public class BasicShopScenario {

    public static final String SCENARIO_NAME = "Basic shop scenario";

    /**
     * User journeys and scenarios for the shop service load tests.
     *
     * Implement the exercise steps (TODO-1 to TODO-4) in this class.
     * Refer to the module's README.md for detailed instructions and hints.
     */
    public static ScenarioBuilder build() {
        // TODO-3: Feed the scenario with the product feeder once implemented.
        return scenario(SCENARIO_NAME)
                .exec(
                        http("GET /api/products")
                                .get("/api/products")
                                .check(status().is(200))
                );
                // TODO-1: Extend this scenario by chaining a GET request for product details: "/api/products/{id}".
                //         Use a hardcoded product ID (e.g. 1 or 2) and verify the response status is 200.
                //
                // TODO-2: Add realistic think time (pause between 1 and 3 seconds) between the requests.
    }

    // TODO-3: Define a circular JSON feeder helper method loading data from "feeders/products.json".
    //         Then:
    //         - Feed it into the main scenario.
    //         - Replace the hardcoded product details URL with a dynamic variable from the feeder.
    //         - Add checks to validate that the returned product details (id, name, price) match the feeder values.

    // TODO-4: Split the single scenario setup into two distinct scenario methods:
    //         - public static ScenarioBuilder browseCatalog(FeederBuilder<?> productFeeder)
    //           (flow already built in previous steps: lists all products -> pause(1, 3) -> retrieves details of a specific product)
    //         - public static ScenarioBuilder checkoutFlow(FeederBuilder<?> productFeeder)
    //           (views product details -> pause(1, 3) -> creates order and saves orderId -> pause(1, 3) -> retrieves the created order)
    //
    //         Note on build():
    //         To keep the project compiling at this step, refactor the build() method above to return browseCatalog(productFeeder()).
    //         Later in Step 6, the simulation class will call these new scenario methods directly, making build() obsolete
    //         (though it can remain as a default fallback).
}
