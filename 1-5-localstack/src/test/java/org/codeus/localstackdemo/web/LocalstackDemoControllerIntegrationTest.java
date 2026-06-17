package org.codeus.localstackdemo.web;

import java.time.Duration;
import java.util.List;
import java.util.stream.IntStream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.codeus.localstackdemo.domain.DemoPayload;
import org.codeus.localstackdemo.domain.SourceType;
import org.codeus.localstackdemo.persistence.ProcessedPayloadEntity;
import org.codeus.localstackdemo.persistence.ProcessedPayloadRepository;
import org.codeus.localstackdemo.service.BackgroundQueueConsumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Sort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
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
@Import(TestConfig.class)
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

}
