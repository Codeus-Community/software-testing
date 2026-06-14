package org.codeus.localstackdemo.service;

import java.util.List;

import org.codeus.localstackdemo.domain.DemoPayload;
import org.codeus.localstackdemo.domain.SourceType;
import org.codeus.localstackdemo.web.FlowResponse;
import org.springframework.stereotype.Service;

@Service
public class LambdaFlowService {

    private final S3PayloadReader s3PayloadReader;
    private final FlowSettingsProvider flowSettingsProvider;
    private final LambdaEnrichmentClient lambdaEnrichmentClient;
    private final PayloadPersistenceService payloadPersistenceService;

    public LambdaFlowService(
            S3PayloadReader s3PayloadReader,
            FlowSettingsProvider flowSettingsProvider,
            LambdaEnrichmentClient lambdaEnrichmentClient,
            PayloadPersistenceService payloadPersistenceService
    ) {
        this.s3PayloadReader = s3PayloadReader;
        this.flowSettingsProvider = flowSettingsProvider;
        this.lambdaEnrichmentClient = lambdaEnrichmentClient;
        this.payloadPersistenceService = payloadPersistenceService;
    }

    public FlowResponse process() {
        List<DemoPayload> payloads = s3PayloadReader.readPayloads();
        boolean useLambda = flowSettingsProvider.isUseLambdaEnabled();
        List<DemoPayload> finalPayloads = payloads;

        if (useLambda) {
            int enrichmentValue = lambdaEnrichmentClient.loadEnrichmentValue();
            finalPayloads = payloads.stream()
                    .map(payload -> payload.enrichWith(enrichmentValue))
                    .toList();
        }

        int persisted = payloadPersistenceService.savePayloads(finalPayloads, SourceType.lambda);
        return new FlowResponse(
                "lambda-flow",
                payloads.size(),
                0,
                persisted,
                SourceType.lambda.name(),
                useLambda,
                "processed_payload"
        );
    }
}
