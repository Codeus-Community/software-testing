package org.codeus.localstackdemo.service;

import java.util.List;

import org.codeus.localstackdemo.config.DemoAppProperties;
import org.codeus.localstackdemo.domain.DemoPayload;
import org.codeus.localstackdemo.domain.SourceType;
import org.codeus.localstackdemo.web.FlowResponse;
import org.springframework.stereotype.Service;

@Service
public class SnsFlowService {

    private final S3PayloadReader s3PayloadReader;
    private final SnsPublisher snsPublisher;
    private final DemoAppProperties properties;

    public SnsFlowService(
            S3PayloadReader s3PayloadReader,
            SnsPublisher snsPublisher,
            DemoAppProperties properties
    ) {
        this.s3PayloadReader = s3PayloadReader;
        this.snsPublisher = snsPublisher;
        this.properties = properties;
    }

    public FlowResponse process() {
        List<DemoPayload> payloads = s3PayloadReader.readPayloads();
        int published = snsPublisher.publish(payloads);

        return new FlowResponse(
                "sns-flow",
                payloads.size(),
                published,
                0,
                SourceType.sns.name(),
                false,
                properties.getSns().getTopicName()
        );
    }
}
