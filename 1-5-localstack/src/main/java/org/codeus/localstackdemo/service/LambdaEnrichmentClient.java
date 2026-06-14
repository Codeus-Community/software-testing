package org.codeus.localstackdemo.service;

import org.codeus.localstackdemo.config.DemoAppProperties;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;

@Service
public class LambdaEnrichmentClient {

    private final LambdaClient lambdaClient;
    private final DemoAppProperties properties;

    public LambdaEnrichmentClient(LambdaClient lambdaClient, DemoAppProperties properties) {
        this.lambdaClient = lambdaClient;
        this.properties = properties;
    }

    public int loadEnrichmentValue() {
        InvokeResponse response = lambdaClient.invoke(InvokeRequest.builder()
                .functionName(properties.getLambda().getFunctionName())
                .payload(SdkBytes.fromUtf8String("{}"))
                .build());

        if (response.functionError() != null) {
            throw new IllegalStateException("Lambda invocation failed: " + response.functionError());
        }

        String payload = response.payload().asUtf8String().replace("\"", "").trim();
        return Integer.parseInt(payload);
    }
}
