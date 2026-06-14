package org.codeus.localstackdemo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.demo")
public class DemoAppProperties {

    private String region = "us-east-1";
    private AwsProperties aws = new AwsProperties();
    private S3Properties s3 = new S3Properties();
    private SqsProperties sqs = new SqsProperties();
    private SnsProperties sns = new SnsProperties();
    private LambdaProperties lambda = new LambdaProperties();
    private SsmProperties ssm = new SsmProperties();
    private DefaultsProperties defaults = new DefaultsProperties();
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
        private String accessKey = "test";
        private String secretKey = "test";

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

        private String bucket = "localstack-demo-bucket";
        private String key = "payloads/payloads.json";

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

        private String queueName = "localstack-demo-queue";

        public String getQueueName() {
            return queueName;
        }

        public void setQueueName(String queueName) {
            this.queueName = queueName;
        }
    }

    public static class SnsProperties {

        private String topicName = "localstack-demo-topic";
        private String subscriptionQueueName = "localstack-demo-sns-subscription-queue";

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

        private String functionName = "localstack-demo-enrichment";

        public String getFunctionName() {
            return functionName;
        }

        public void setFunctionName(String functionName) {
            this.functionName = functionName;
        }
    }

    public static class SsmProperties {

        private String batchSizeParameter = "/localstack-demo/batchsize";
        private String useLambdaParameter = "/localstack-demo/useLambda";

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

        private int batchSize = 3;
        private boolean useLambda = false;

        public int getBatchSize() {
            return batchSize;
        }

        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize;
        }

        public boolean isUseLambda() {
            return useLambda;
        }

        public void setUseLambda(boolean useLambda) {
            this.useLambda = useLambda;
        }
    }

    public static class ConsumerProperties {

        private boolean enabled = true;
        private long pollDelayMs = 3000L;
        private int maxMessages = 10;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public long getPollDelayMs() {
            return pollDelayMs;
        }

        public void setPollDelayMs(long pollDelayMs) {
            this.pollDelayMs = pollDelayMs;
        }

        public int getMaxMessages() {
            return maxMessages;
        }

        public void setMaxMessages(int maxMessages) {
            this.maxMessages = maxMessages;
        }
    }
}
