package org.codeus.localstackdemo.web;

import org.codeus.localstackdemo.config.DemoAppProperties;
import org.codeus.localstackdemo.persistence.ProcessedPayloadRepository;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.TaskScheduler;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.ssm.SsmClient;

@TestConfiguration(proxyBeanMethods = false)
class TestConfig {

    @Bean
    TestAwsSupport testAwsSupport(
            S3Client s3Client,
            SqsClient sqsClient,
            SsmClient ssmClient,
            DemoAppProperties properties,
            ProcessedPayloadRepository processedPayloadRepository
    ) {
        return new TestAwsSupport(
                s3Client,
                sqsClient,
                ssmClient,
                properties,
                processedPayloadRepository
        );
    }

    @Bean(name = "taskScheduler")
    TaskScheduler taskScheduler() {
        return new NoOpTaskScheduler();
    }
}
