package org.codeus.localstackdemo.web;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Delayed;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.codeus.localstackdemo.config.DemoAppProperties;
import org.codeus.localstackdemo.domain.DemoPayload;
import org.codeus.localstackdemo.domain.SourceType;
import org.codeus.localstackdemo.persistence.ProcessedPayloadEntity;
import org.codeus.localstackdemo.persistence.ProcessedPayloadRepository;
import org.codeus.localstackdemo.service.BackgroundQueueConsumer;
import org.codeus.localstackdemo.util.PayloadFileGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.scheduling.TaskScheduler;
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
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("local")
class LocalstackDemoControllerIntegrationTest {

    private static final DockerImageName LOCALSTACK_IMAGE = DockerImageName.parse("localstack/localstack:3.0.2");
    private static final String MAIN_QUEUE_NAME = "localstack-demo-queue";
    private static final String SNS_QUEUE_NAME = "localstack-demo-sns-subscription-queue";
    private static final String SNS_TOPIC_NAME = "localstack-demo-topic";
    private static final String PROCESSED_PAYLOAD_TABLE = "processed_payload";
    private static final int DEFAULT_PAYLOAD_COUNT = 5;

    @Container
    @SuppressWarnings("resource")
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("localstack_demo")
            .withUsername("localstack")
            .withPassword("localstack");

    @Container
    private static final LocalStackContainer LOCALSTACK = new LocalStackContainer(LOCALSTACK_IMAGE)
            .withServices(
                    LocalStackContainer.Service.S3,
                    LocalStackContainer.Service.SQS,
                    LocalStackContainer.Service.SNS,
                    LocalStackContainer.Service.SSM,
                    LocalStackContainer.Service.LAMBDA
            );

    static {
        Startables.deepStart(POSTGRES, LOCALSTACK).join();
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("app.demo.region", LOCALSTACK::getRegion);
        registry.add("app.demo.aws.access-key", LOCALSTACK::getAccessKey);
        registry.add("app.demo.aws.secret-key", LOCALSTACK::getSecretKey);
        registry.add(
                "app.demo.aws.endpoint-url",
                () -> LOCALSTACK.getEndpointOverride(LocalStackContainer.Service.S3).toString()
        );
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProcessedPayloadRepository processedPayloadRepository;

    @Autowired
    private BackgroundQueueConsumer backgroundQueueConsumer;

    @Autowired
    private TestAwsSupport testAwsSupport;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void resetState() {
        testAwsSupport.reset(DEFAULT_PAYLOAD_COUNT, 3, false);
    }

    @Nested
    class S3OnlyFlowEndpoint {

        @Test
        void persists_only_current_batch_size_from_ssm() throws Exception {
            testAwsSupport.reset(DEFAULT_PAYLOAD_COUNT, 2, false);

            mockMvc.perform(post("/s3-only-flow"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.flow").value("s3-only-flow"))
                    .andExpect(jsonPath("$.recordsRead").value(DEFAULT_PAYLOAD_COUNT))
                    .andExpect(jsonPath("$.recordsPublished").value(0))
                    .andExpect(jsonPath("$.recordsPersisted").value(2))
                    .andExpect(jsonPath("$.sourceType").value("s3_only"))
                    .andExpect(jsonPath("$.lambdaInvoked").value(false))
                    .andExpect(jsonPath("$.targetResource").value(PROCESSED_PAYLOAD_TABLE));

            assertPersistedPayloads(SourceType.s3_only, expectedPayloads(2), true);
        }

        @Test
        void re_reads_batch_size_after_ssm_change_without_restart() throws Exception {
            testAwsSupport.reset(DEFAULT_PAYLOAD_COUNT, 2, false);

            mockMvc.perform(post("/s3-only-flow"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.recordsPersisted").value(2));

            assertPersistedPayloads(SourceType.s3_only, expectedPayloads(2), true);

            testAwsSupport.clearPersistedRecords();
            testAwsSupport.setBatchSize(4);

            mockMvc.perform(post("/s3-only-flow"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.recordsPersisted").value(4));

            assertPersistedPayloads(SourceType.s3_only, expectedPayloads(4), true);
        }
    }

    @Nested
    class SqsFlowEndpoint {

        @Test
        void publishes_to_sqs_and_persists_messages_after_manual_poll() throws Exception {
            mockMvc.perform(post("/sqs-flow"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.flow").value("sqs-flow"))
                    .andExpect(jsonPath("$.recordsRead").value(DEFAULT_PAYLOAD_COUNT))
                    .andExpect(jsonPath("$.recordsPublished").value(DEFAULT_PAYLOAD_COUNT))
                    .andExpect(jsonPath("$.recordsPersisted").value(0))
                    .andExpect(jsonPath("$.sourceType").value("sqs"))
                    .andExpect(jsonPath("$.lambdaInvoked").value(false))
                    .andExpect(jsonPath("$.targetResource").value(MAIN_QUEUE_NAME));

            await().atMost(Duration.ofSeconds(10))
                    .until(() -> testAwsSupport.getQueueDepth(MAIN_QUEUE_NAME) == DEFAULT_PAYLOAD_COUNT);

            assertThat(testAwsSupport.peekMessages(MAIN_QUEUE_NAME, DEFAULT_PAYLOAD_COUNT))
                    .hasSize(DEFAULT_PAYLOAD_COUNT)
                    .anySatisfy(message -> assertThat(message).contains("\"message\":\"message_1\""));

            backgroundQueueConsumer.pollQueues();

            assertPersistedPayloads(SourceType.sqs, expectedPayloads(DEFAULT_PAYLOAD_COUNT), false);
            await().atMost(Duration.ofSeconds(10))
                    .until(() -> testAwsSupport.getQueueDepth(MAIN_QUEUE_NAME) == 0);
        }
    }

    @Nested
    class SnsFlowEndpoint {

        @Test
        void publishes_to_sns_subscription_queue_and_persists_messages_after_manual_poll() throws Exception {
            mockMvc.perform(post("/sns-flow"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.flow").value("sns-flow"))
                    .andExpect(jsonPath("$.recordsRead").value(DEFAULT_PAYLOAD_COUNT))
                    .andExpect(jsonPath("$.recordsPublished").value(DEFAULT_PAYLOAD_COUNT))
                    .andExpect(jsonPath("$.recordsPersisted").value(0))
                    .andExpect(jsonPath("$.sourceType").value("sns"))
                    .andExpect(jsonPath("$.lambdaInvoked").value(false))
                    .andExpect(jsonPath("$.targetResource").value(SNS_TOPIC_NAME));

            await().atMost(Duration.ofSeconds(10))
                    .until(() -> testAwsSupport.getQueueDepth(SNS_QUEUE_NAME) == DEFAULT_PAYLOAD_COUNT);

            assertThat(testAwsSupport.peekMessages(SNS_QUEUE_NAME, DEFAULT_PAYLOAD_COUNT))
                    .hasSize(DEFAULT_PAYLOAD_COUNT)
                    .anySatisfy(message -> assertThat(message).contains("\"message\":\"message_1\""));

            backgroundQueueConsumer.pollQueues();

            assertPersistedPayloads(SourceType.sns, expectedPayloads(DEFAULT_PAYLOAD_COUNT), false);
            await().atMost(Duration.ofSeconds(10))
                    .until(() -> testAwsSupport.getQueueDepth(SNS_QUEUE_NAME) == 0);
        }
    }

    @Nested
    class LambdaFlowEndpoint {

        @Test
        void persists_original_payloads_when_use_lambda_is_false() throws Exception {
            testAwsSupport.reset(DEFAULT_PAYLOAD_COUNT, 3, false);

            mockMvc.perform(post("/lambda-flow"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.flow").value("lambda-flow"))
                    .andExpect(jsonPath("$.recordsRead").value(DEFAULT_PAYLOAD_COUNT))
                    .andExpect(jsonPath("$.recordsPublished").value(0))
                    .andExpect(jsonPath("$.recordsPersisted").value(DEFAULT_PAYLOAD_COUNT))
                    .andExpect(jsonPath("$.sourceType").value("lambda"))
                    .andExpect(jsonPath("$.lambdaInvoked").value(false))
                    .andExpect(jsonPath("$.targetResource").value(PROCESSED_PAYLOAD_TABLE));

            assertPersistedPayloads(SourceType.lambda, expectedPayloads(DEFAULT_PAYLOAD_COUNT), true);
        }

        @Test
        void re_reads_use_lambda_after_ssm_change_without_restart() throws Exception {
            testAwsSupport.reset(DEFAULT_PAYLOAD_COUNT, 3, false);

            mockMvc.perform(post("/lambda-flow"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.lambdaInvoked").value(false));

            assertPersistedPayloads(SourceType.lambda, expectedPayloads(DEFAULT_PAYLOAD_COUNT), true);

            testAwsSupport.clearPersistedRecords();
            testAwsSupport.setUseLambda(true);

            mockMvc.perform(post("/lambda-flow"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.lambdaInvoked").value(true))
                    .andExpect(jsonPath("$.recordsPersisted").value(DEFAULT_PAYLOAD_COUNT));

            assertPersistedPayloads(SourceType.lambda, enrichedPayloads(DEFAULT_PAYLOAD_COUNT, 42), true);
        }
    }

    private void assertPersistedPayloads(SourceType sourceType, List<DemoPayload> expectedPayloads, boolean preserveOrder) {
        List<ProcessedPayloadEntity> entities = processedPayloadRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));

        assertThat(entities).hasSize(expectedPayloads.size());
        assertThat(entities).extracting(ProcessedPayloadEntity::getSourceType).containsOnly(sourceType);

        List<DemoPayload> actualPayloads = entities.stream()
                .map(this::toPayload)
                .toList();

        if (preserveOrder) {
            assertThat(actualPayloads).containsExactlyElementsOf(expectedPayloads);
        } else {
            assertThat(actualPayloads).containsExactlyInAnyOrderElementsOf(expectedPayloads);
        }
    }

    private DemoPayload toPayload(ProcessedPayloadEntity entity) {
        try {
            return objectMapper.treeToValue(entity.getPayload(), DemoPayload.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to deserialize persisted payload", exception);
        }
    }

    private List<DemoPayload> expectedPayloads(int count) {
        return IntStream.rangeClosed(1, count)
                .mapToObj(index -> new DemoPayload(index, "message_" + index))
                .toList();
    }

    private List<DemoPayload> enrichedPayloads(int count, int enrichmentValue) {
        return IntStream.rangeClosed(1, count)
                .mapToObj(index -> new DemoPayload(index, "message_" + index + "_" + enrichmentValue))
                .toList();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestConfig {

        @Bean
        TestAwsSupport testAwsSupport(
                S3Client s3Client,
                SqsClient sqsClient,
                SnsClient snsClient,
                SsmClient ssmClient,
                LambdaClient lambdaClient,
                DemoAppProperties properties,
                ProcessedPayloadRepository processedPayloadRepository
        ) {
            TestAwsSupport support = new TestAwsSupport(
                    s3Client,
                    sqsClient,
                    snsClient,
                    ssmClient,
                    lambdaClient,
                    properties,
                    processedPayloadRepository
            );
            support.provisionInfrastructure();
            return support;
        }

        @Bean(name = "taskScheduler")
        TaskScheduler taskScheduler() {
            return new NoOpTaskScheduler();
        }
    }

    static final class TestAwsSupport {

        private static final String ACCOUNT_ID = "000000000000";
        private static final String BATCH_SIZE_PARAMETER = "/localstack-demo/batchsize";
        private static final String USE_LAMBDA_PARAMETER = "/localstack-demo/useLambda";

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

    static final class NoOpTaskScheduler implements TaskScheduler {

        @Override
        public Clock getClock() {
            return Clock.systemUTC();
        }

        @Override
        public ScheduledFuture<?> schedule(Runnable task, org.springframework.scheduling.Trigger trigger) {
            return NoOpScheduledFuture.INSTANCE;
        }

        @Override
        public ScheduledFuture<?> schedule(Runnable task, Instant startTime) {
            return NoOpScheduledFuture.INSTANCE;
        }

        @Override
        public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Instant startTime, Duration period) {
            return NoOpScheduledFuture.INSTANCE;
        }

        @Override
        public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Duration period) {
            return NoOpScheduledFuture.INSTANCE;
        }

        @Override
        public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Instant startTime, Duration delay) {
            return NoOpScheduledFuture.INSTANCE;
        }

        @Override
        public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Duration delay) {
            return NoOpScheduledFuture.INSTANCE;
        }
    }

    static final class NoOpScheduledFuture implements ScheduledFuture<Object> {

        private static final NoOpScheduledFuture INSTANCE = new NoOpScheduledFuture();

        private NoOpScheduledFuture() {
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return unit.convert(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        }

        @Override
        public int compareTo(Delayed other) {
            return Long.compare(getDelay(TimeUnit.MILLISECONDS), other.getDelay(TimeUnit.MILLISECONDS));
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return false;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean isDone() {
            return false;
        }

        @Override
        public Object get() {
            return null;
        }

        @Override
        public Object get(long timeout, TimeUnit unit) {
            return null;
        }
    }
}
