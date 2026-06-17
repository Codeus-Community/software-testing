package org.codeus.localstackdemo.web;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.codeus.localstackdemo.config.DemoAppProperties;
import org.codeus.localstackdemo.domain.DemoPayload;
import org.codeus.localstackdemo.persistence.ProcessedPayloadRepository;
import org.codeus.localstackdemo.util.PayloadFileGenerator;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.CreateFunctionRequest;
import software.amazon.awssdk.services.lambda.model.FunctionCode;
import software.amazon.awssdk.services.lambda.model.GetFunctionConfigurationRequest;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.Runtime;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.CreateTopicRequest;
import software.amazon.awssdk.services.sns.model.SubscribeRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.PutParameterRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

final class TestAwsSupport {

    private static final String ACCOUNT_ID = "000000000000";
    private static final String BATCH_SIZE_PARAMETER = "/localstack-demo/batchsize";
    private static final String USE_LAMBDA_PARAMETER = "/localstack-demo/useLambda";
    private static final int DEFAULT_PAYLOAD_COUNT = 5;

    private final S3Client s3Client;
    private final SqsClient sqsClient;
    private final SnsClient snsClient;
    private final SsmClient ssmClient;
    private final LambdaClient lambdaClient;
    private final DemoAppProperties properties;
    private final ProcessedPayloadRepository processedPayloadRepository;

    TestAwsSupport(
            S3Client s3Client,
            SqsClient sqsClient,
            SnsClient snsClient,
            SsmClient ssmClient,
            LambdaClient lambdaClient,
            DemoAppProperties properties,
            ProcessedPayloadRepository processedPayloadRepository
    ) {
        this.s3Client = s3Client;
        this.sqsClient = sqsClient;
        this.snsClient = snsClient;
        this.ssmClient = ssmClient;
        this.lambdaClient = lambdaClient;
        this.properties = properties;
        this.processedPayloadRepository = processedPayloadRepository;
    }

    void provisionInfrastructure() {
        createBucketIfNeeded();
        createQueue(properties.getSqs().getQueueName());
        String topicArn = snsClient.createTopic(CreateTopicRequest.builder()
                        .name(properties.getSns().getTopicName())
                        .build())
                .topicArn();
        String snsQueueUrl = createQueue(properties.getSns().getSubscriptionQueueName());
        String snsQueueArn = queueArn(snsQueueUrl);

        sqsClient.setQueueAttributes(builder -> builder
                .queueUrl(snsQueueUrl)
                .attributes(Map.of(
                        QueueAttributeName.POLICY,
                        snsQueuePolicy(topicArn, snsQueueArn)
                )));

        snsClient.subscribe(SubscribeRequest.builder()
                .topicArn(topicArn)
                .protocol("sqs")
                .endpoint(snsQueueArn)
                .attributes(Map.of("RawMessageDelivery", "true"))
                .build());

        setBatchSize(3);
        setUseLambda(false);
        uploadPayload(DEFAULT_PAYLOAD_COUNT);
        createLambdaFunction();
        waitUntilLambdaIsReady();
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
        putParameter(BATCH_SIZE_PARAMETER, Integer.toString(batchSize));
    }

    void setUseLambda(boolean useLambda) {
        putParameter(USE_LAMBDA_PARAMETER, Boolean.toString(useLambda));
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

    private void createBucketIfNeeded() {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(properties.getS3().getBucket()).build());
        } catch (S3Exception exception) {
            s3Client.createBucket(CreateBucketRequest.builder().bucket(properties.getS3().getBucket()).build());
        }
    }

    private String createQueue(String queueName) {
        sqsClient.createQueue(builder -> builder.queueName(queueName));
        return queueUrl(queueName);
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

    private String queueArn(String queueUrl) {
        return sqsClient.getQueueAttributes(GetQueueAttributesRequest.builder()
                        .queueUrl(queueUrl)
                        .attributeNames(QueueAttributeName.QUEUE_ARN)
                        .build())
                .attributes()
                .get(QueueAttributeName.QUEUE_ARN);
    }

    private String snsQueuePolicy(String topicArn, String queueArn) {
        return """
                {
                  "Version": "2012-10-17",
                  "Statement": [
                    {
                      "Sid": "AllowLocalstackDemoTopic",
                      "Effect": "Allow",
                      "Principal": "*",
                      "Action": "sqs:SendMessage",
                      "Resource": "%s",
                      "Condition": {
                        "ArnEquals": {
                          "aws:SourceArn": "%s"
                        }
                      }
                    }
                  ]
                }
                """.formatted(queueArn, topicArn).replace(System.lineSeparator(), "");
    }

    private void createLambdaFunction() {
        byte[] lambdaArchive;
        try {
            lambdaArchive = Files.readAllBytes(resolveLambdaArchivePath());
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read the Lambda archive for integration tests", exception);
        }

        lambdaClient.createFunction(CreateFunctionRequest.builder()
                .functionName(properties.getLambda().getFunctionName())
                .runtime(Runtime.NODEJS18_X)
                .handler("index.handler")
                .role("arn:aws:iam::" + ACCOUNT_ID + ":role/localstack-demo-lambda-role")
                .code(FunctionCode.builder().zipFile(SdkBytes.fromByteArray(lambdaArchive)).build())
                .build());
    }

    private void waitUntilLambdaIsReady() {
        await().atMost(Duration.ofSeconds(60))
                .ignoreExceptions()
                .until(() -> "Active".equals(lambdaClient.getFunctionConfiguration(GetFunctionConfigurationRequest.builder()
                                .functionName(properties.getLambda().getFunctionName())
                                .build())
                        .stateAsString()));

        await().atMost(Duration.ofSeconds(60))
                .ignoreExceptions()
                .untilAsserted(() -> {
                    String payload = lambdaClient.invoke(InvokeRequest.builder()
                                    .functionName(properties.getLambda().getFunctionName())
                                    .payload(SdkBytes.fromUtf8String("{}"))
                                    .build())
                            .payload()
                            .asUtf8String()
                            .replace("\"", "")
                            .trim();
                    assertThat(payload).isEqualTo("42");
                });
    }

    private Path resolveLambdaArchivePath() {
        Path workingDirectory = Path.of(System.getProperty("user.dir"));
        Path moduleRelative = workingDirectory.resolve("localstack/lambda/function.zip");
        if (Files.exists(moduleRelative)) {
            return moduleRelative;
        }

        Path repoRelative = workingDirectory.resolve("1-5-localstack/localstack/lambda/function.zip");
        if (Files.exists(repoRelative)) {
            return repoRelative;
        }

        throw new IllegalStateException("Unable to locate localstack/lambda/function.zip from " + workingDirectory);
    }
}
