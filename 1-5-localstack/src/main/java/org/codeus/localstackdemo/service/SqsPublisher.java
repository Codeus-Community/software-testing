package org.codeus.localstackdemo.service;

import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.codeus.localstackdemo.config.DemoAppProperties;
import org.codeus.localstackdemo.domain.DemoPayload;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@Service
public class SqsPublisher {

    private final SqsClient sqsClient;
    private final AwsResourceResolver resourceResolver;
    private final DemoAppProperties properties;
    private final ObjectMapper objectMapper;

    public SqsPublisher(
            SqsClient sqsClient,
            AwsResourceResolver resourceResolver,
            DemoAppProperties properties,
            ObjectMapper objectMapper
    ) {
        this.sqsClient = sqsClient;
        this.resourceResolver = resourceResolver;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public int publish(List<DemoPayload> payloads) {
        String queueUrl = resourceResolver.resolveQueueUrl(properties.getSqs().getQueueName());
        for (DemoPayload payload : payloads) {
            sqsClient.sendMessage(SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(toJson(payload))
                    .build());
        }
        return payloads.size();
    }

    private String toJson(DemoPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize payload for SQS publishing", exception);
        }
    }
}
