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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class S3OnlyFlowServiceTest {

    @Mock
    private S3PayloadReader s3PayloadReader;

    @Mock
    private FlowSettingsProvider flowSettingsProvider;

    @Mock
    private PayloadPersistenceService payloadPersistenceService;

    @InjectMocks
    private S3OnlyFlowService service;

    @Test
    void limitsRecordsByBatchSizeBeforePersisting() {
        List<DemoPayload> payloads = List.of(
                new DemoPayload(1, "message_1"),
                new DemoPayload(2, "message_2"),
                new DemoPayload(3, "message_3"),
                new DemoPayload(4, "message_4")
        );

        when(s3PayloadReader.readPayloads()).thenReturn(payloads);
        when(flowSettingsProvider.getBatchSize()).thenReturn(2);
        when(payloadPersistenceService.savePayloads(anyList(), eq(SourceType.s3_only))).thenReturn(2);

        FlowResponse response = service.process();

        ArgumentCaptor<List<DemoPayload>> payloadCaptor = ArgumentCaptor.forClass(List.class);
        verify(payloadPersistenceService).savePayloads(payloadCaptor.capture(), eq(SourceType.s3_only));

        assertThat(payloadCaptor.getValue()).containsExactly(
                new DemoPayload(1, "message_1"),
                new DemoPayload(2, "message_2")
        );
        assertThat(response.recordsRead()).isEqualTo(4);
        assertThat(response.recordsPersisted()).isEqualTo(2);
        assertThat(response.sourceType()).isEqualTo("s3_only");
    }
}
