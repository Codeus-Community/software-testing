package org.example.shop.loadtests.simulation;

import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;
import org.example.shop.loadtests.config.HttpProtocolFactory;
import org.example.shop.loadtests.scenario.BasicShopScenario;

import java.time.Duration;

import static io.gatling.javaapi.core.CoreDsl.*;

/**
 * Gatling simulation setting up workload injection profiles and assertions.
 *
 * Implement the exercise steps (TODO-5 to TODO-7) in this class.
 * Refer to the module's README.md for detailed instructions and hints.
 */
public class BasicShopSimulation extends Simulation {

    {
        // You can override baseUrl via JVM property: -DbaseUrl=http://host:port
        String baseUrl = System.getProperty("baseUrl", "http://localhost:8080");

        HttpProtocolBuilder httpProtocol = HttpProtocolFactory.create(baseUrl);

        // TODO-6: Instead of using the single scenario build() method, update this simulation
        //         to obtain the feeder and run both browseCatalog and checkoutFlow scenarios in parallel.
        ScenarioBuilder scn = BasicShopScenario.build();

        setUp(
            // TODO-5: Update the injection profile to represent a multi-phase workload model:
            //         - Warm-up (ramp-up): 1 to 6 users/sec over 40 seconds.
            //         - Steady load: constant 6 users/sec for 60 seconds.
            //         - Spike load: constant 14 users/sec for 10 seconds.
            //
            // TODO-6: Inject traffic to both browseCatalog and checkoutFlow scenarios in parallel:
            //         - browseCatalog: Use the workload profile from TODO-5.
            //         - checkoutFlow: Use a lower workload profile:
            //             - Warm-up: 1 to 2 users/sec over 40 seconds.
            //             - Steady load: constant 2 users/sec for 60 seconds.
            //             - Spike load: constant 5 users/sec for 10 seconds.
            scn.injectOpen(
                atOnceUsers(1),
                rampUsers(5).during(Duration.ofSeconds(10))
            )
        )
        .protocols(httpProtocol);
        // TODO-7: Add SLA assertions to check that global 95th percentile response time is less than 900 ms,
        //         and total failed requests percentage is at most 0.5%.
    }
}
