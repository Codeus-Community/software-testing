package org.codeus.localstackdemo.service;

public interface FlowSettingsProvider {

    int getBatchSize();

    boolean isUseLambdaEnabled();
}
