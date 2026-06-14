package org.codeus.localstackdemo.service;

import org.codeus.localstackdemo.config.DemoAppProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("prod-like")
public class StaticFlowSettingsProvider implements FlowSettingsProvider {

    private final DemoAppProperties properties;

    public StaticFlowSettingsProvider(DemoAppProperties properties) {
        this.properties = properties;
    }

    @Override
    public int getBatchSize() {
        return properties.getDefaults().getBatchSize();
    }

    @Override
    public boolean isUseLambdaEnabled() {
        return properties.getDefaults().isUseLambda();
    }
}
