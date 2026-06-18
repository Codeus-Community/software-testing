package org.codeus.localstackdemo.web;

import java.util.List;

import org.codeus.localstackdemo.config.DemoAppProperties;
import org.codeus.localstackdemo.persistence.ProcessedPayloadRepository;
import org.codeus.localstackdemo.util.PayloadFileGenerator;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.PutParameterRequest;

final class TestAwsSupport {

    private final S3Client s3Client;
    private final SqsClient sqsClient;
    private final SsmClient ssmClient;
    private final DemoAppProperties properties;
    private final ProcessedPayloadRepository processedPayloadRepository;

    TestAwsSupport(
            S3Client s3Client,
            SqsClient sqsClient,
            SsmClient ssmClient,
            DemoAppProperties properties,
            ProcessedPayloadRepository processedPayloadRepository
    ) {
        this.s3Client = s3Client;
        this.sqsClient = sqsClient;
        this.ssmClient = ssmClient;
        this.properties = properties;
        this.processedPayloadRepository = processedPayloadRepository;
    }

    void reset(int payloadCount, int batchSize, boolean useLambda) {
        clearPersistedRecords();
        uploadPayload(payloadCount);
        drainQueue(properties.getSqs().getQueueName());
        drainQueue(properties.getSns().getSubscriptionQueueName());
        setBatchSize(batchSize);
        setUseLambda(useLambda);
    }

    void clearPersistedRecords() {
        processedPayloadRepository.deleteAllInBatch();
    }

    void setBatchSize(int batchSize) {
        putParameter(properties.getSsm().getBatchSizeParameter(), Integer.toString(batchSize));
    }

    void setUseLambda(boolean useLambda) {
        putParameter(properties.getSsm().getUseLambdaParameter(), Boolean.toString(useLambda));
    }

    int getQueueDepth(String queueName) {
        String queueUrl = queueUrl(queueName);
        return Integer.parseInt(sqsClient.getQueueAttributes(GetQueueAttributesRequest.builder()
                        .queueUrl(queueUrl)
                        .attributeNames(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES)
                        .build())
                .attributes()
                .getOrDefault(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES, "0"));
    }

    List<String> peekMessages(String queueName, int maxMessages) {
        String queueUrl = queueUrl(queueName);
        return sqsClient.receiveMessage(ReceiveMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .maxNumberOfMessages(maxMessages)
                        .waitTimeSeconds(1)
                        .visibilityTimeout(0)
                        .build())
                .messages()
                .stream()
                .map(Message::body)
                .toList();
    }

    private void uploadPayload(int payloadCount) {
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(properties.getS3().getBucket())
                        .key(properties.getS3().getKey())
                        .build(),
                RequestBody.fromString(PayloadFileGenerator.generatePayloadJson(payloadCount))
        );
    }

    private void drainQueue(String queueName) {
        String queueUrl = queueUrl(queueName);
        while (true) {
            List<Message> messages = sqsClient.receiveMessage(ReceiveMessageRequest.builder()
                            .queueUrl(queueUrl)
                            .maxNumberOfMessages(10)
                            .waitTimeSeconds(1)
                            .build())
                    .messages();

            if (messages.isEmpty()) {
                return;
            }

            for (Message message : messages) {
                sqsClient.deleteMessage(DeleteMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .receiptHandle(message.receiptHandle())
                        .build());
            }
        }
    }

    private void putParameter(String parameterName, String value) {
        ssmClient.putParameter(PutParameterRequest.builder()
                .name(parameterName)
                .value(value)
                .type("String")
                .overwrite(true)
                .build());
    }

    private String queueUrl(String queueName) {
        return sqsClient.getQueueUrl(GetQueueUrlRequest.builder().queueName(queueName).build()).queueUrl();
    }
}
