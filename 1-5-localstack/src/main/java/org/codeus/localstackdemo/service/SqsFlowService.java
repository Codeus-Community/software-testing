package org.codeus.localstackdemo.service;

import java.util.List;

import org.codeus.localstackdemo.config.DemoAppProperties;
import org.codeus.localstackdemo.domain.DemoPayload;
import org.codeus.localstackdemo.domain.SourceType;
import org.codeus.localstackdemo.web.FlowResponse;
import org.springframework.stereotype.Service;

@Service
public class SqsFlowService {

    private final S3PayloadReader s3PayloadReader;
    private final SqsPublisher sqsPublisher;
    private final DemoAppProperties properties;

    public SqsFlowService(
            S3PayloadReader s3PayloadReader,
            SqsPublisher sqsPublisher,
            DemoAppProperties properties
    ) {
        this.s3PayloadReader = s3PayloadReader;
        this.sqsPublisher = sqsPublisher;
        this.properties = properties;
    }

    public FlowResponse process() {
        List<DemoPayload> payloads = s3PayloadReader.readPayloads();
        int published = sqsPublisher.publish(payloads);

        return new FlowResponse(
                "sqs-flow",
                payloads.size(),
                published,
                0,
                SourceType.sqs.name(),
                false,
                properties.getSqs().getQueueName()
        );
    }
}
