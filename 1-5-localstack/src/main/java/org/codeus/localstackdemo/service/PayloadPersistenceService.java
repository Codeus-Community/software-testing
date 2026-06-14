package org.codeus.localstackdemo.service;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.codeus.localstackdemo.domain.DemoPayload;
import org.codeus.localstackdemo.domain.SourceType;
import org.codeus.localstackdemo.persistence.ProcessedPayloadEntity;
import org.codeus.localstackdemo.persistence.ProcessedPayloadRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PayloadPersistenceService {

    private final ObjectMapper objectMapper;
    private final ProcessedPayloadRepository repository;

    public PayloadPersistenceService(ObjectMapper objectMapper, ProcessedPayloadRepository repository) {
        this.objectMapper = objectMapper;
        this.repository = repository;
    }

    @Transactional
    public int savePayloads(List<DemoPayload> payloads, SourceType sourceType) {
        List<ProcessedPayloadEntity> entities = payloads.stream()
                .map(payload -> new ProcessedPayloadEntity(objectMapper.valueToTree(payload), sourceType))
                .toList();

        repository.saveAll(entities);
        return entities.size();
    }
}
