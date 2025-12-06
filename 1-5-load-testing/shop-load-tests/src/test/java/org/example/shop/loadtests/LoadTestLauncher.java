package org.example.shop.loadtests;

import io.gatling.app.Gatling;
import io.gatling.core.config.GatlingPropertiesBuilder;
import java.nio.file.Paths;
import org.example.shop.loadtests.simulation.BasicShopSimulation;

public class LoadTestLauncher {

    public static void main(String[] args) {
        // Allow overriding the simulation class via -DsimulationClass=...
        String simulationClass = System.getProperty("simulationClass", BasicShopSimulation.class.getName());

        GatlingPropertiesBuilder props = new GatlingPropertiesBuilder()
            .simulationClass(simulationClass)
            .resourcesDirectory(Paths.get("src", "test", "resources").toString())
            .resultsDirectory(Paths.get("target", "gatling").toString())
            .binariesDirectory(Paths.get("target", "test-classes").toString());

        Gatling.fromMap(props.build());
    }
}
