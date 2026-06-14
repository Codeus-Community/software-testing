package org.codeus.localstackdemo.service;

import org.codeus.localstackdemo.config.DemoAppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.SsmException;

@Service
@Profile("local")
public class SsmFlowSettingsProvider implements FlowSettingsProvider {

    private static final Logger log = LoggerFactory.getLogger(SsmFlowSettingsProvider.class);

    private final SsmClient ssmClient;
    private final DemoAppProperties properties;

    public SsmFlowSettingsProvider(SsmClient ssmClient, DemoAppProperties properties) {
        this.ssmClient = ssmClient;
        this.properties = properties;
    }

    @Override
    public int getBatchSize() {
        String value = readParameter(properties.getSsm().getBatchSizeParameter());
        if (value == null) {
            return properties.getDefaults().getBatchSize();
        }

        try {
            int parsed = Integer.parseInt(value);
            return parsed > 0 ? parsed : properties.getDefaults().getBatchSize();
        } catch (NumberFormatException exception) {
            log.warn("Invalid batch size value '{}' in SSM, falling back to default", value);
            return properties.getDefaults().getBatchSize();
        }
    }

    @Override
    public boolean isUseLambdaEnabled() {
        String value = readParameter(properties.getSsm().getUseLambdaParameter());
        if (value == null) {
            return properties.getDefaults().isUseLambda();
        }
        return Boolean.parseBoolean(value);
    }

    private String readParameter(String parameterName) {
        try {
            return ssmClient.getParameter(GetParameterRequest.builder().name(parameterName).build())
                    .parameter()
                    .value();
        } catch (SsmException exception) {
            log.warn("Unable to read SSM parameter '{}', using defaults. {}", parameterName, exception.getMessage());
            return null;
        }
    }
}
