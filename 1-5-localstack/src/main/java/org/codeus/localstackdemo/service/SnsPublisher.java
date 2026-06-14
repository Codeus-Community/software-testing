package org.codeus.localstackdemo.service;

import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.codeus.localstackdemo.config.DemoAppProperties;
import org.codeus.localstackdemo.domain.DemoPayload;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

@Service
public class SnsPublisher {

    private final SnsClient snsClient;
    private final AwsResourceResolver resourceResolver;
    private final DemoAppProperties properties;
    private final ObjectMapper objectMapper;

    public SnsPublisher(
            SnsClient snsClient,
            AwsResourceResolver resourceResolver,
            DemoAppProperties properties,
            ObjectMapper objectMapper
    ) {
        this.snsClient = snsClient;
        this.resourceResolver = resourceResolver;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public int publish(List<DemoPayload> payloads) {
        String topicArn = resourceResolver.resolveTopicArn(properties.getSns().getTopicName());
        for (DemoPayload payload : payloads) {
            snsClient.publish(PublishRequest.builder()
                    .topicArn(topicArn)
                    .message(toJson(payload))
                    .build());
        }
        return payloads.size();
    }

    private String toJson(DemoPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize payload for SNS publishing", exception);
        }
    }
}
