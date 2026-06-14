package org.codeus.localstackdemo.service;

import java.util.List;

import org.codeus.localstackdemo.domain.DemoPayload;
import org.codeus.localstackdemo.domain.SourceType;
import org.codeus.localstackdemo.web.FlowResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LambdaFlowServiceTest {

    @Mock
    private S3PayloadReader s3PayloadReader;

    @Mock
    private FlowSettingsProvider flowSettingsProvider;

    @Mock
    private LambdaEnrichmentClient lambdaEnrichmentClient;

    @Mock
    private PayloadPersistenceService payloadPersistenceService;

    @InjectMocks
    private LambdaFlowService service;

    @Test
    void enrichesPayloadsWhenLambdaIsEnabled() {
        when(s3PayloadReader.readPayloads()).thenReturn(List.of(new DemoPayload(1, "message_1")));
        when(flowSettingsProvider.isUseLambdaEnabled()).thenReturn(true);
        when(lambdaEnrichmentClient.loadEnrichmentValue()).thenReturn(42);
        when(payloadPersistenceService.savePayloads(anyList(), eq(SourceType.lambda))).thenReturn(1);

        FlowResponse response = service.process();

        ArgumentCaptor<List<DemoPayload>> payloadCaptor = ArgumentCaptor.forClass(List.class);
        verify(payloadPersistenceService).savePayloads(payloadCaptor.capture(), eq(SourceType.lambda));

        assertThat(payloadCaptor.getValue()).containsExactly(new DemoPayload(1, "message_1_42"));
        assertThat(response.lambdaInvoked()).isTrue();
        assertThat(response.recordsPersisted()).isEqualTo(1);
    }

    @Test
    void skipsLambdaWhenDisabled() {
        when(s3PayloadReader.readPayloads()).thenReturn(List.of(new DemoPayload(1, "message_1")));
        when(flowSettingsProvider.isUseLambdaEnabled()).thenReturn(false);
        when(payloadPersistenceService.savePayloads(anyList(), eq(SourceType.lambda))).thenReturn(1);

        service.process();

        ArgumentCaptor<List<DemoPayload>> payloadCaptor = ArgumentCaptor.forClass(List.class);
        verify(payloadPersistenceService).savePayloads(payloadCaptor.capture(), eq(SourceType.lambda));
        verify(lambdaEnrichmentClient, never()).loadEnrichmentValue();
        assertThat(payloadCaptor.getValue()).containsExactly(new DemoPayload(1, "message_1"));
    }
}
