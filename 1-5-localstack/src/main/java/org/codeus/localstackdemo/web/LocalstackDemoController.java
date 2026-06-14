package org.codeus.localstackdemo.web;

import org.codeus.localstackdemo.service.LambdaFlowService;
import org.codeus.localstackdemo.service.S3OnlyFlowService;
import org.codeus.localstackdemo.service.SnsFlowService;
import org.codeus.localstackdemo.service.SqsFlowService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LocalstackDemoController {

    private final S3OnlyFlowService s3OnlyFlowService;
    private final SqsFlowService sqsFlowService;
    private final SnsFlowService snsFlowService;
    private final LambdaFlowService lambdaFlowService;

    public LocalstackDemoController(
            S3OnlyFlowService s3OnlyFlowService,
            SqsFlowService sqsFlowService,
            SnsFlowService snsFlowService,
            LambdaFlowService lambdaFlowService
    ) {
        this.s3OnlyFlowService = s3OnlyFlowService;
        this.sqsFlowService = sqsFlowService;
        this.snsFlowService = snsFlowService;
        this.lambdaFlowService = lambdaFlowService;
    }

    @PostMapping("/s3-only-flow")
    public FlowResponse s3OnlyFlow() {
        return s3OnlyFlowService.process();
    }

    @PostMapping("/sqs-flow")
    public FlowResponse sqsFlow() {
        return sqsFlowService.process();
    }

    @PostMapping("/sns-flow")
    public FlowResponse snsFlow() {
        return snsFlowService.process();
    }

    @PostMapping("/lambda-flow")
    public FlowResponse lambdaFlow() {
        return lambdaFlowService.process();
    }
}
