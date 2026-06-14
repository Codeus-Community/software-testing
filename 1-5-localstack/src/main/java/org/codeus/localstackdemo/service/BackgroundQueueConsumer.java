package org.codeus.localstackdemo.service;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.codeus.localstackdemo.config.DemoAppProperties;
import org.codeus.localstackdemo.domain.DemoPayload;
import org.codeus.localstackdemo.domain.SourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

@Component
@ConditionalOnProperty(name = "app.demo.consumer.enabled", havingValue = "true")
public class BackgroundQueueConsumer {

    private static final Logger log = LoggerFactory.getLogger(BackgroundQueueConsumer.class);

    private final SqsClient sqsClient;
    private final AwsResourceResolver resourceResolver;
    private final DemoAppProperties properties;
    private final ObjectMapper objectMapper;
    private final PayloadPersistenceService payloadPersistenceService;

    public BackgroundQueueConsumer(
            SqsClient sqsClient,
            AwsResourceResolver resourceResolver,
            DemoAppProperties properties,
            ObjectMapper objectMapper,
            PayloadPersistenceService payloadPersistenceService
    ) {
        this.sqsClient = sqsClient;
        this.resourceResolver = resourceResolver;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.payloadPersistenceService = payloadPersistenceService;
    }

    @Scheduled(fixedDelayString = "${app.demo.consumer.poll-delay-ms:3000}")
    public void pollQueues() {
        consumeQueue(properties.getSqs().getQueueName(), SourceType.sqs);
        consumeQueue(properties.getSns().getSubscriptionQueueName(), SourceType.sns);
    }

    private void consumeQueue(String queueName, SourceType sourceType) {
        try {
            String queueUrl = resourceResolver.resolveQueueUrl(queueName);
            List<Message> messages = sqsClient.receiveMessage(ReceiveMessageRequest.builder()
                            .queueUrl(queueUrl)
                            .maxNumberOfMessages(properties.getConsumer().getMaxMessages())
                            .waitTimeSeconds(1)
                            .build())
                    .messages();

            if (messages.isEmpty()) {
                return;
            }

            List<DemoPayload> payloads = new ArrayList<>();
            List<Message> processedMessages = new ArrayList<>();
            for (Message message : messages) {
                try {
                    payloads.add(objectMapper.readValue(message.body(), DemoPayload.class));
                    processedMessages.add(message);
                } catch (JsonProcessingException exception) {
                    log.warn("Skipping unreadable message from queue '{}': {}", queueName, message.body());
                }
            }

            if (payloads.isEmpty()) {
                return;
            }

            payloadPersistenceService.savePayloads(payloads, sourceType);
            for (Message message : processedMessages) {
                sqsClient.deleteMessage(DeleteMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .receiptHandle(message.receiptHandle())
                        .build());
            }
        } catch (RuntimeException exception) {
            log.warn("Background consumption from queue '{}' failed: {}", queueName, exception.getMessage());
        }
    }
}
