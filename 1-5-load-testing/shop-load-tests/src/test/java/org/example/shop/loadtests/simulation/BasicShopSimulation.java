package org.example.shop.loadtests.simulation;

import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;
import org.example.shop.loadtests.config.HttpProtocolFactory;
import org.example.shop.loadtests.scenario.BasicShopScenario;

import java.time.Duration;

import static io.gatling.javaapi.core.CoreDsl.*;

/**
 * Basic Gatling simulation for the lesson.
 *
 * For now:
 *  - very simple scenario (GET /api/products)
 *  - low load profile: 1 user at once + ramp to 5 users during 10s
 *
 * This class should be extended with:
 *  - TODO-5: a more realistic load profile:
 *      - warm-up (ramp): 1 → 6 users/s during 40s;
 *      - steady load: 6 users/s during 60s;
 *      - spike / stress: 14 users/s during 10s.
 *  - TODO-6: multiple scenarios (browse vs checkout) running in parallel with different traffic mix and profiles.
 *  - TODO-7: SLA assertions (latency + error rate) so that the run fails on regressions.
 *      - global p95 latency < 900 ms;
 *      - global error percentage (KO) <= 0.5%.
 */
public class BasicShopSimulation extends Simulation {

    {
        // You can override baseUrl via JVM property: -DbaseUrl=http://host:port
        String baseUrl = System.getProperty("baseUrl", "http://localhost:8080");

        HttpProtocolBuilder httpProtocol = HttpProtocolFactory.create(baseUrl);

        ScenarioBuilder scn = BasicShopScenario.build();

        setUp(
            scn.injectOpen(
                atOnceUsers(1),
                rampUsers(5).during(Duration.ofSeconds(10))
            )
        ).protocols(httpProtocol);
    }
}
