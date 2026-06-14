package org.codeus.localstackdemo.web;

public record FlowResponse(
        String flow,
        int recordsRead,
        int recordsPublished,
        int recordsPersisted,
        String sourceType,
        boolean lambdaInvoked,
        String targetResource
) {
}
