# Rebuild the LocalStack Test Setup

This module is now a regular Spring Boot app with a `prod-like` default profile. It still compiles, but the integration test no longer brings up local AWS infrastructure for you, so local test runs can fail at runtime until you restore the LocalStack setup.

Use this guide to recreate the removed LocalStack pieces step by step. The order matters because each step unlocks the next one.

## What was intentionally removed

- `src/main/resources/application-local.yml`
- `src/main/java/org/codeus/localstackdemo/config/LocalAwsClientConfiguration.java`
- `src/test/java/org/codeus/localstackdemo/web/TestConfig.java`
- `src/test/java/org/codeus/localstackdemo/web/TestAwsSupport.java`
- the `org.testcontainers:localstack` test dependency
- the module `docker-compose.yml`
- `localstack/init/ready.d/01-bootstrap.sh`

The `localstack/bootstrap/` and `localstack/lambda/` directories are available as examples you can reuse while following the guide.

This guide does **not** cover recreating `docker-compose.yml` or `localstack/init/ready.d/01-bootstrap.sh`. Ignore those two files for this exercise.

## Step 1: Add the LocalStack Testcontainers dependency

Start in `pom.xml`. Re-add the LocalStack module next to the other Testcontainers dependencies:

```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>localstack</artifactId>
    <scope>test</scope>
</dependency>
```

This restores the `LocalStackContainer` class used by the integration test.

## Step 2: Recreate `application-local.yml`

Create `src/main/resources/application-local.yml`.

The easiest way is to copy the structure from `src/main/resources/application-prod-like.yml` and then adapt it for the local profile.

Use `application-prod-like.yml` as the example for:

- the overall YAML structure
- the `app.demo` property layout
- the existing AWS-related keys the app already expects

Then change it for LocalStack:

- switch the datasource to the local PostgreSQL values
- replace the AWS resource names with the LocalStack demo names
  - S3 bucket: `customer-events-ingestion` -> `localstack-demo-bucket`
  - S3 key: `inbound/customer-events.json` -> `payloads/payloads.json`
  - SQS queue: `customer-events-processing` -> `localstack-demo-queue`
  - SNS topic: `customer-events-notifications` -> `localstack-demo-topic`
  - SNS subscription queue: `customer-events-notifications-consumer` -> `localstack-demo-sns-subscription-queue`
  - Lambda function: `customer-events-enrichment` -> `localstack-demo-enrichment`
  - SSM batch size parameter: `/app/batchsize` -> `/localstack-demo/batchsize`
  - SSM use-lambda parameter: `/app/useLambda` -> `/localstack-demo/useLambda`
- add `app.demo.aws.endpoint-url`, because the local profile must point SDK clients to LocalStack
- keep the SSM parameter names aligned with the LocalStack demo setup
- enable the background consumer for local runs

Final example:

<details>
<summary>`application-local.yml` solution</summary>

```yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/localstack_demo
    username: localstack
    password: localstack

app:
  demo:
    region: us-east-1
    aws:
      access-key: test
      secret-key: test
      endpoint-url: http://localhost:4566
    s3:
      bucket: localstack-demo-bucket
      key: payloads/payloads.json
    sqs:
      queue-name: localstack-demo-queue
    sns:
      topic-name: localstack-demo-topic
      subscription-queue-name: localstack-demo-sns-subscription-queue
    lambda:
      function-name: localstack-demo-enrichment
    ssm:
      batch-size-parameter: /localstack-demo/batchsize
      use-lambda-parameter: /localstack-demo/useLambda
    defaults:
      batch-size: 3
      use-lambda: false
    consumer:
      enabled: true
      poll-delay-ms: 3000
      max-messages: 10
```

</details>

This file does two things:

- it gives the `local` profile its own datasource and AWS values
- it re-enables SSM-backed flow settings and the background consumer for LocalStack runs

## Step 3: Re-add `LocalAwsClientConfiguration`

Create `src/main/java/org/codeus/localstackdemo/config/LocalAwsClientConfiguration.java`.

Use `src/main/java/org/codeus/localstackdemo/config/ProdLikeAwsClientConfiguration.java` as the example and keep the same general bean layout for S3, SQS, SNS, SSM, and Lambda clients.

Then adapt it for the `local` profile:

- keep the same `DemoAppProperties`-based region and credentials setup
- add `endpointOverride(URI.create(properties.getAws().getEndpointUrl()))` to every client
- keep S3 path-style access enabled, because it is needed for LocalStack (as `.serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())`)
- change the profile annotation from `prod-like` to `local`

Final example:

<details>
<summary>`LocalAwsClientConfiguration.java` solution</summary>

```java
package org.codeus.localstackdemo.config;

import java.net.URI;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.ssm.SsmClient;

@Configuration(proxyBeanMethods = false)
@Profile("local")
public class LocalAwsClientConfiguration {

    @Bean
    S3Client localS3Client(DemoAppProperties properties) {
        return S3Client.builder()
                .region(Region.of(properties.getRegion()))
                .endpointOverride(URI.create(properties.getAws().getEndpointUrl()))
                .credentialsProvider(credentialsProvider(properties))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();
    }

    @Bean
    SqsClient localSqsClient(DemoAppProperties properties) {
        return SqsClient.builder()
                .region(Region.of(properties.getRegion()))
                .endpointOverride(URI.create(properties.getAws().getEndpointUrl()))
                .credentialsProvider(credentialsProvider(properties))
                .build();
    }

    @Bean
    SnsClient localSnsClient(DemoAppProperties properties) {
        return SnsClient.builder()
                .region(Region.of(properties.getRegion()))
                .endpointOverride(URI.create(properties.getAws().getEndpointUrl()))
                .credentialsProvider(credentialsProvider(properties))
                .build();
    }

    @Bean
    SsmClient localSsmClient(DemoAppProperties properties) {
        return SsmClient.builder()
                .region(Region.of(properties.getRegion()))
                .endpointOverride(URI.create(properties.getAws().getEndpointUrl()))
                .credentialsProvider(credentialsProvider(properties))
                .build();
    }

    @Bean
    LambdaClient localLambdaClient(DemoAppProperties properties) {
        return LambdaClient.builder()
                .region(Region.of(properties.getRegion()))
                .endpointOverride(URI.create(properties.getAws().getEndpointUrl()))
                .credentialsProvider(credentialsProvider(properties))
                .build();
    }

    private StaticCredentialsProvider credentialsProvider(DemoAppProperties properties) {
        return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(properties.getAws().getAccessKey(), properties.getAws().getSecretKey())
        );
    }
}
```

</details>

This is the profile-specific Spring configuration that points every AWS SDK client at the LocalStack endpoint instead of real AWS.

## Step 4: Recreate `TestAwsSupport`

Create `src/test/java/org/codeus/localstackdemo/web/TestAwsSupport.java`, because `TestConfig` depends on it.

Use the existing AWS service classes in `src/main/java/org/codeus/localstackdemo/service/` as the behavioral example for queue names, topic names, S3 key usage, and Lambda invocation flow.

`TestAwsSupport` should be recreated with the same collaborators as before:

- `S3Client`
- `SqsClient`
- `SnsClient`
- `SsmClient`
- `LambdaClient`
- `DemoAppProperties`
- `ProcessedPayloadRepository`

The main teaching goal in this file is infrastructure setup. Try to implement the core provisioning pieces yourself first, then compare with the final example.

### 4.1 Implement `createBucketIfNeeded()`

This helper should check whether the configured bucket already exists and create it only when needed.

The idea is:

- read the bucket name from `properties.getS3().getBucket()`
- call `headBucket(...)`
- if LocalStack reports that the bucket does not exist, create it with `createBucket(...)`

Starter snippet:

```java
private void createBucketIfNeeded() {
    try {
        s3Client.headBucket(HeadBucketRequest.builder()
                .bucket(properties.getS3().getBucket())
                .build());
    } catch (S3Exception exception) {
        // create the bucket here
    }
}
```

### 4.2 Implement `createQueue()`

You need one helper that creates a queue and returns its URL. This will be reused for the main SQS queue and the SNS subscription queue.

The idea is:

- call `sqsClient.createQueue(...)`
- look up the queue URL with `GetQueueUrlRequest`
- return that URL so later setup can use it for attributes and polling

Starter snippet:

```java
private String createQueue(String queueName) {
    sqsClient.createQueue(builder -> builder.queueName(queueName));
    return queueUrl(queueName);
}
```

### 4.3 Add the SNS topic creation and subscription logic

In `provisionInfrastructure()`, create the SNS topic first, then create the subscription queue, then connect them.

The flow should be:

1. create the topic with `snsClient.createTopic(...)`
2. create the subscription queue with your `createQueue(...)` helper
3. read the queue ARN
4. set the queue policy so SNS can publish into that queue
5. subscribe the queue to the topic with `RawMessageDelivery=true`

Starter snippets:

```java
String topicArn = snsClient.createTopic(CreateTopicRequest.builder()
                .name(properties.getSns().getTopicName())
                .build())
        .topicArn();
```

```java
snsClient.subscribe(SubscribeRequest.builder()
        .topicArn(topicArn)
        .protocol("sqs")
        .endpoint(snsQueueArn)
        .attributes(Map.of("RawMessageDelivery", "true"))
        .build());
```

### 4.4 Implement `createLambdaFunction()`

The Lambda body and archive are already preserved in:

- `localstack/lambda/index.js`
- `localstack/lambda/function.zip`

Your `createLambdaFunction()` helper should load the zip archive from disk and send it to LocalStack with `lambdaClient.createFunction(...)`.

The idea is:

- resolve the path to `localstack/lambda/function.zip`
- read the bytes
- call `CreateFunctionRequest.builder()`
- use the configured function name from `properties.getLambda().getFunctionName()`
- keep the handler as `index.handler`
- keep the runtime as `NODEJS18_X`

Starter snippet:

```java
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
```

### 4.5 Review the final example and utility methods

The rest of the class is mostly utility code around the core setup:

- `provisionInfrastructure()`:
  - create the S3 bucket if it does not exist
  - create the main SQS queue
  - create the SNS topic
  - create the SNS subscription queue
  - apply the queue policy that allows the topic to publish into the subscription queue
  - subscribe the queue to the topic with `RawMessageDelivery=true`
  - seed SSM with batch size `3` and `useLambda=false`
  - upload generated payload JSON into the configured S3 bucket/key
  - create the Lambda function
  - wait until the Lambda is active and returns `42`
- `reset(int payloadCount, int batchSize, boolean useLambda)`:
  - clear persisted rows
  - upload a new payload file
  - drain both queues
  - overwrite the SSM parameters
- helper methods for:
  - `clearPersistedRecords()`
  - `setBatchSize(int batchSize)`
  - `setUseLambda(boolean useLambda)`
  - `getQueueDepth(String queueName)`
  - `peekMessages(String queueName, int maxMessages)`

The payload upload should keep using `PayloadFileGenerator.generatePayloadJson(payloadCount)`. If you want an example of the expected payload shape, check `localstack/bootstrap/payloads.json`.

Final example:

<details>
<summary>`TestAwsSupport.java` solution</summary>

```java
package org.codeus.localstackdemo.web;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.codeus.localstackdemo.config.DemoAppProperties;
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
```

</details>

## Step 5: Re-add the test-only Spring wiring

Create `src/test/java/org/codeus/localstackdemo/web/TestConfig.java`.

Use the current test package as the example for package placement and imports. This configuration should do two things:

- expose a `TestAwsSupport` bean and call `provisionInfrastructure()` once
- override the `taskScheduler` bean with `NoOpTaskScheduler`

Keep `src/test/java/org/codeus/localstackdemo/web/NoOpTaskScheduler.java` and `src/test/java/org/codeus/localstackdemo/web/NoOpScheduledFuture.java` in use here.

Why the scheduler override is needed:

- `BackgroundQueueConsumer` is scheduled
- in integration tests we want queue polling to happen only when the test calls it explicitly
- without this override, background polling races with assertions and makes the test behavior non-deterministic

Final example:

<details>
<summary>`TestConfig.java` solution</summary>

```java
package org.codeus.localstackdemo.web;

import org.codeus.localstackdemo.config.DemoAppProperties;
import org.codeus.localstackdemo.persistence.ProcessedPayloadRepository;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.TaskScheduler;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.ssm.SsmClient;

@TestConfiguration(proxyBeanMethods = false)
class TestConfig {

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
```

</details>

## Step 6: Restore the LocalStack integration test setup

Update `src/test/java/org/codeus/localstackdemo/web/LocalstackDemoControllerIntegrationTest.java` so it uses the `local` profile again, imports `TestConfig`, and starts both PostgreSQL and LocalStack containers.

Use the current stripped integration test as the starting point:

- keep the PostgreSQL container and datasource dynamic properties
- keep the same endpoint-level test structure
- restore only the LocalStack-specific parts on top of that baseline

Restore these pieces:

- `import org.springframework.context.annotation.Import;`
- `import org.testcontainers.containers.localstack.LocalStackContainer;`
- `import org.testcontainers.utility.DockerImageName;`
- `@ActiveProfiles("local")`
- `@Import(TestConfig.class)`
- a `LocalStackContainer` field with S3, SQS, SNS, SSM, and Lambda services enabled
- `Startables.deepStart(POSTGRES, LOCALSTACK).join();`
```java
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
```


- dynamic properties for:
  - `app.demo.region`
  - `app.demo.aws.access-key`
  - `app.demo.aws.secret-key`
  - `app.demo.aws.endpoint-url`


The important part of the dynamic property setup looks like this:

<details>
<summary>`@DynamicPropertySource` snippet</summary>

```java
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
```

</details>

- uncomment the rest of the `LocalstackDemoControllerIntegrationTest`

## What success looks like

Before you restore these steps:

- the app compiles
- the integration test uses the `prod-like` profile
- local test runs can fail because the app expects real AWS resources

After you restore these steps:

- the integration test can provision AWS-like resources locally
- the app can talk to LocalStack through the `local` profile
- the LocalStack-backed integration scenario becomes reproducible on a local machine
