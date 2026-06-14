package org.codeus.localstackdemo.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.CreateTopicRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;

@Component
public class AwsResourceResolver {

    private final SqsClient sqsClient;
    private final SnsClient snsClient;
    private final Map<String, String> queueUrls = new ConcurrentHashMap<>();
    private final Map<String, String> topicArns = new ConcurrentHashMap<>();

    public AwsResourceResolver(SqsClient sqsClient, SnsClient snsClient) {
        this.sqsClient = sqsClient;
        this.snsClient = snsClient;
    }

    public String resolveQueueUrl(String queueName) {
        return queueUrls.computeIfAbsent(queueName, name -> sqsClient.getQueueUrl(
                GetQueueUrlRequest.builder().queueName(name).build()
        ).queueUrl());
    }

    public String resolveTopicArn(String topicName) {
        return topicArns.computeIfAbsent(topicName, name -> snsClient.createTopic(
                CreateTopicRequest.builder().name(name).build()
        ).topicArn());
    }
}
