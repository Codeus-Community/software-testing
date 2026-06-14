package org.codeus.localstackdemo.service;

import java.util.ArrayList;
import java.util.List;

import org.codeus.localstackdemo.domain.DemoPayload;
import org.codeus.localstackdemo.domain.SourceType;
import org.codeus.localstackdemo.web.FlowResponse;
import org.springframework.stereotype.Service;

@Service
public class S3OnlyFlowService {

    private final S3PayloadReader s3PayloadReader;
    private final FlowSettingsProvider flowSettingsProvider;
    private final PayloadPersistenceService payloadPersistenceService;

    public S3OnlyFlowService(
            S3PayloadReader s3PayloadReader,
            FlowSettingsProvider flowSettingsProvider,
            PayloadPersistenceService payloadPersistenceService
    ) {
        this.s3PayloadReader = s3PayloadReader;
        this.flowSettingsProvider = flowSettingsProvider;
        this.payloadPersistenceService = payloadPersistenceService;
    }

    public FlowResponse process() {
        List<DemoPayload> payloads = s3PayloadReader.readPayloads();
        int batchSize = Math.min(flowSettingsProvider.getBatchSize(), payloads.size());
        List<DemoPayload> batch = new ArrayList<>(payloads.subList(0, batchSize));
        int persisted = payloadPersistenceService.savePayloads(batch, SourceType.s3_only);

        return new FlowResponse(
                "s3-only-flow",
                payloads.size(),
                0,
                persisted,
                SourceType.s3_only.name(),
                false,
                "processed_payload"
        );
    }
}
