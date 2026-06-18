package org.codeus.localstackdemo.web;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Comparator;
import java.util.stream.Collectors;

import org.testcontainers.containers.localstack.LocalStackContainer;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.CloudFormationException;
import software.amazon.awssdk.services.cloudformation.model.CreateStackRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackEventsRequest;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.GetFunctionConfigurationRequest;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

final class LocalstackCloudFormationDeployer {

    static final String STACK_NAME = "localstack-demo-stack";
    private static final String LAMBDA_NAME = "localstack-demo-enrichment";

    private LocalstackCloudFormationDeployer() {
    }

    static void deploy(LocalStackContainer localstack) {
        String templateBody = readTemplate();

        try (CloudFormationClient cloudFormationClient = cloudFormationClient(localstack)) {
            cloudFormationClient.createStack(CreateStackRequest.builder()
                    .stackName(STACK_NAME)
                    .templateBody(templateBody)
                    .build());

            cloudFormationClient.waiter()
                    .waitUntilStackCreateComplete(
                            request -> request.stackName(STACK_NAME),
                            configuration -> configuration.waitTimeout(Duration.ofMinutes(2))
                    )
                    .matched()
                    .exception()
                    .ifPresent(exception -> {
                        throw new IllegalStateException(
                                "CloudFormation stack creation failed:\n" + describeStackEvents(cloudFormationClient),
                                exception
                        );
                    });
        } catch (CloudFormationException exception) {
            throw new IllegalStateException("Unable to deploy LocalStack CloudFormation stack", exception);
        }

        waitUntilLambdaIsReady(localstack);
    }

    private static CloudFormationClient cloudFormationClient(LocalStackContainer localstack) {
        return CloudFormationClient.builder()
                .endpointOverride(localstack.getEndpointOverride(LocalStackContainer.Service.CLOUDFORMATION))
                .region(Region.of(localstack.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(localstack.getAccessKey(), localstack.getSecretKey())
                ))
                .build();
    }

    private static LambdaClient lambdaClient(LocalStackContainer localstack) {
        return LambdaClient.builder()
                .endpointOverride(localstack.getEndpointOverride(LocalStackContainer.Service.LAMBDA))
                .region(Region.of(localstack.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(localstack.getAccessKey(), localstack.getSecretKey())
                ))
                .build();
    }

    private static String describeStackEvents(CloudFormationClient cloudFormationClient) {
        try {
            return cloudFormationClient.describeStackEvents(DescribeStackEventsRequest.builder()
                            .stackName(STACK_NAME)
                            .build())
                    .stackEvents()
                    .stream()
                    .sorted(Comparator.comparing(event -> event.timestamp(), Comparator.reverseOrder()))
                    .limit(10)
                    .map(event -> "%s %s %s".formatted(
                            event.resourceStatusAsString(),
                            event.logicalResourceId(),
                            event.resourceStatusReason() == null ? "" : event.resourceStatusReason()
                    ).trim())
                    .collect(Collectors.joining(System.lineSeparator()));
        } catch (RuntimeException exception) {
            return "Unable to read CloudFormation stack events: " + exception.getMessage();
        }
    }

    private static void waitUntilLambdaIsReady(LocalStackContainer localstack) {
        try (LambdaClient lambdaClient = lambdaClient(localstack)) {
            await().atMost(Duration.ofSeconds(60))
                    .ignoreExceptions()
                    .until(() -> "Active".equals(lambdaClient.getFunctionConfiguration(GetFunctionConfigurationRequest.builder()
                                    .functionName(LAMBDA_NAME)
                                    .build())
                            .stateAsString()));

            await().atMost(Duration.ofSeconds(60))
                    .ignoreExceptions()
                    .untilAsserted(() -> {
                        String payload = lambdaClient.invoke(InvokeRequest.builder()
                                        .functionName(LAMBDA_NAME)
                                        .payload(SdkBytes.fromUtf8String("{}"))
                                        .build())
                                .payload()
                                .asUtf8String()
                                .replace("\"", "")
                                .trim();
                        assertThat(payload).isEqualTo("42");
                    });
        }
    }

    private static String readTemplate() {
        try {
            return Files.readString(resolveTemplatePath());
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read LocalStack CloudFormation template", exception);
        }
    }

    private static Path resolveTemplatePath() {
        Path workingDirectory = Path.of(System.getProperty("user.dir"));
        Path moduleRelative = workingDirectory.resolve("localstack/cloudformation/localstack-demo.yml");
        if (Files.exists(moduleRelative)) {
            return moduleRelative;
        }

        Path repoRelative = workingDirectory.resolve("1-5-localstack/localstack/cloudformation/localstack-demo.yml");
        if (Files.exists(repoRelative)) {
            return repoRelative;
        }

        throw new IllegalStateException("Unable to locate localstack/cloudformation/localstack-demo.yml from " + workingDirectory);
    }
}
