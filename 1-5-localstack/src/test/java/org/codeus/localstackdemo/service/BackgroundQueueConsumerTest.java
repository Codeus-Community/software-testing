package org.codeus.localstackdemo.service;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.codeus.localstackdemo.config.DemoAppProperties;
import org.codeus.localstackdemo.domain.DemoPayload;
import org.codeus.localstackdemo.domain.SourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BackgroundQueueConsumerTest {

    @Mock
    private SqsClient sqsClient;

    @Mock
    private AwsResourceResolver resourceResolver;

    @Mock
    private PayloadPersistenceService payloadPersistenceService;

    private BackgroundQueueConsumer consumer;

    @BeforeEach
    void setUp() {
        DemoAppProperties properties = new DemoAppProperties();
        properties.getSqs().setQueueName("main-queue");
        properties.getSns().setSubscriptionQueueName("sns-subscription-queue");
        properties.getConsumer().setMaxMessages(10);

        consumer = new BackgroundQueueConsumer(
                sqsClient,
                resourceResolver,
                properties,
                new ObjectMapper(),
                payloadPersistenceService
        );
    }

    @Test
    void persistsMessagesFromBothQueuesWithMatchingSourceTypes() {
        when(resourceResolver.resolveQueueUrl("main-queue")).thenReturn("queue-url-1");
        when(resourceResolver.resolveQueueUrl("sns-subscription-queue")).thenReturn("queue-url-2");
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(
                ReceiveMessageResponse.builder()
                        .messages(Message.builder().body("{\"id\":1,\"message\":\"message_1\"}").receiptHandle("rh-1").build())
                        .build(),
                ReceiveMessageResponse.builder()
                        .messages(Message.builder().body("{\"id\":2,\"message\":\"message_2\"}").receiptHandle("rh-2").build())
                        .build()
        );

        consumer.pollQueues();

        ArgumentCaptor<List<DemoPayload>> payloadCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<SourceType> sourceTypeCaptor = ArgumentCaptor.forClass(SourceType.class);
        verify(payloadPersistenceService, times(2)).savePayloads(payloadCaptor.capture(), sourceTypeCaptor.capture());

        assertThat(sourceTypeCaptor.getAllValues()).containsExactly(SourceType.sqs, SourceType.sns);
        assertThat(payloadCaptor.getAllValues().get(0)).containsExactly(new DemoPayload(1, "message_1"));
        assertThat(payloadCaptor.getAllValues().get(1)).containsExactly(new DemoPayload(2, "message_2"));
        verify(sqsClient, times(2)).deleteMessage(any(DeleteMessageRequest.class));
    }
}
