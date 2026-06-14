package org.codeus.localstackdemo.domain;

public record DemoPayload(int id, String message) {

    public DemoPayload enrichWith(int enrichmentValue) {
        return new DemoPayload(id, message + "_" + enrichmentValue);
    }
}
