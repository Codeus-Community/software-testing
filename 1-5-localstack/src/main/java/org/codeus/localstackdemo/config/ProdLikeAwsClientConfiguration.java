package org.codeus.localstackdemo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.ssm.SsmClient;

@Configuration(proxyBeanMethods = false)
@Profile("prod-like")
public class ProdLikeAwsClientConfiguration {

    @Bean
    S3Client prodLikeS3Client(DemoAppProperties properties) {
        return S3Client.builder()
                .region(Region.of(properties.getRegion()))
                .credentialsProvider(credentialsProvider(properties))
                .build();
    }

    @Bean
    SqsClient prodLikeSqsClient(DemoAppProperties properties) {
        return SqsClient.builder()
                .region(Region.of(properties.getRegion()))
                .credentialsProvider(credentialsProvider(properties))
                .build();
    }

    @Bean
    SnsClient prodLikeSnsClient(DemoAppProperties properties) {
        return SnsClient.builder()
                .region(Region.of(properties.getRegion()))
                .credentialsProvider(credentialsProvider(properties))
                .build();
    }

    @Bean
    SsmClient prodLikeSsmClient(DemoAppProperties properties) {
        return SsmClient.builder()
                .region(Region.of(properties.getRegion()))
                .credentialsProvider(credentialsProvider(properties))
                .build();
    }

    @Bean
    LambdaClient prodLikeLambdaClient(DemoAppProperties properties) {
        return LambdaClient.builder()
                .region(Region.of(properties.getRegion()))
                .credentialsProvider(credentialsProvider(properties))
                .build();
    }

    private StaticCredentialsProvider credentialsProvider(DemoAppProperties properties) {
        return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(properties.getAws().getAccessKey(), properties.getAws().getSecretKey())
        );
    }
}
