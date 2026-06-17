package org.codeus.localstackdemo.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.demo")
public class DemoAppProperties {

    @NotBlank
    private String region;
    @Valid
    private AwsProperties aws = new AwsProperties();
    @Valid
    private S3Properties s3 = new S3Properties();
    @Valid
    private SqsProperties sqs = new SqsProperties();
    @Valid
    private SnsProperties sns = new SnsProperties();
    @Valid
    private LambdaProperties lambda = new LambdaProperties();
    @Valid
    private SsmProperties ssm = new SsmProperties();
    @Valid
    private DefaultsProperties defaults = new DefaultsProperties();
    @Valid
    private ConsumerProperties consumer = new ConsumerProperties();

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public AwsProperties getAws() {
        return aws;
    }

    public void setAws(AwsProperties aws) {
        this.aws = aws;
    }

    public S3Properties getS3() {
        return s3;
    }

    public void setS3(S3Properties s3) {
        this.s3 = s3;
    }

    public SqsProperties getSqs() {
        return sqs;
    }

    public void setSqs(SqsProperties sqs) {
        this.sqs = sqs;
    }

    public SnsProperties getSns() {
        return sns;
    }

    public void setSns(SnsProperties sns) {
        this.sns = sns;
    }

    public LambdaProperties getLambda() {
        return lambda;
    }

    public void setLambda(LambdaProperties lambda) {
        this.lambda = lambda;
    }

    public SsmProperties getSsm() {
        return ssm;
    }

    public void setSsm(SsmProperties ssm) {
        this.ssm = ssm;
    }

    public DefaultsProperties getDefaults() {
        return defaults;
    }

    public void setDefaults(DefaultsProperties defaults) {
        this.defaults = defaults;
    }

    public ConsumerProperties getConsumer() {
        return consumer;
    }

    public void setConsumer(ConsumerProperties consumer) {
        this.consumer = consumer;
    }

    public static class AwsProperties {

        private String endpointUrl;
        @NotBlank
        private String accessKey;
        @NotBlank
        private String secretKey;

        public String getEndpointUrl() {
            return endpointUrl;
        }

        public void setEndpointUrl(String endpointUrl) {
            this.endpointUrl = endpointUrl;
        }

        public String getAccessKey() {
            return accessKey;
        }

        public void setAccessKey(String accessKey) {
            this.accessKey = accessKey;
        }

        public String getSecretKey() {
            return secretKey;
        }

        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }
    }

    public static class S3Properties {

        @NotBlank
        private String bucket;
        @NotBlank
        private String key;

        public String getBucket() {
            return bucket;
        }

        public void setBucket(String bucket) {
            this.bucket = bucket;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }
    }

    public static class SqsProperties {

        @NotBlank
        private String queueName;

        public String getQueueName() {
            return queueName;
        }

        public void setQueueName(String queueName) {
            this.queueName = queueName;
        }
    }

    public static class SnsProperties {

        @NotBlank
        private String topicName;
        @NotBlank
        private String subscriptionQueueName;

        public String getTopicName() {
            return topicName;
        }

        public void setTopicName(String topicName) {
            this.topicName = topicName;
        }

        public String getSubscriptionQueueName() {
            return subscriptionQueueName;
        }

        public void setSubscriptionQueueName(String subscriptionQueueName) {
            this.subscriptionQueueName = subscriptionQueueName;
        }
    }

    public static class LambdaProperties {

        @NotBlank
        private String functionName;

        public String getFunctionName() {
            return functionName;
        }

        public void setFunctionName(String functionName) {
            this.functionName = functionName;
        }
    }

    public static class SsmProperties {

        private String batchSizeParameter;
        private String useLambdaParameter;

        public String getBatchSizeParameter() {
            return batchSizeParameter;
        }

        public void setBatchSizeParameter(String batchSizeParameter) {
            this.batchSizeParameter = batchSizeParameter;
        }

        public String getUseLambdaParameter() {
            return useLambdaParameter;
        }

        public void setUseLambdaParameter(String useLambdaParameter) {
            this.useLambdaParameter = useLambdaParameter;
        }
    }

    public static class DefaultsProperties {

        @NotNull
        @Min(1)
        private Integer batchSize;
        @NotNull
        private Boolean useLambda;

        public Integer getBatchSize() {
            return batchSize;
        }

        public void setBatchSize(Integer batchSize) {
            this.batchSize = batchSize;
        }

        public Boolean isUseLambda() {
            return useLambda;
        }

        public void setUseLambda(Boolean useLambda) {
            this.useLambda = useLambda;
        }
    }

    public static class ConsumerProperties {

        @NotNull
        private Boolean enabled;
        @NotNull
        @Min(0)
        private Long pollDelayMs;
        @NotNull
        @Min(1)
        @Max(10)
        private Integer maxMessages;

        public Boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public Long getPollDelayMs() {
            return pollDelayMs;
        }

        public void setPollDelayMs(Long pollDelayMs) {
            this.pollDelayMs = pollDelayMs;
        }

        public Integer getMaxMessages() {
            return maxMessages;
        }

        public void setMaxMessages(Integer maxMessages) {
            this.maxMessages = maxMessages;
        }
    }
}
