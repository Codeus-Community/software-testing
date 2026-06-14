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
