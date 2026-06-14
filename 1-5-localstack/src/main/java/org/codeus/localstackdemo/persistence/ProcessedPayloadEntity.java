package org.codeus.localstackdemo.persistence;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.codeus.localstackdemo.domain.SourceType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "processed_payload")
public class ProcessedPayloadEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private JsonNode payload;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "source_type", nullable = false, columnDefinition = "source_type")
    private SourceType sourceType;

    @Column(name = "processing_datetime", insertable = false, updatable = false)
    private OffsetDateTime processingDatetime;

    protected ProcessedPayloadEntity() {
    }

    public ProcessedPayloadEntity(JsonNode payload, SourceType sourceType) {
        this.payload = payload;
        this.sourceType = sourceType;
    }

    public Integer getId() {
        return id;
    }

    public JsonNode getPayload() {
        return payload;
    }

    public void setPayload(JsonNode payload) {
        this.payload = payload;
    }

    public SourceType getSourceType() {
        return sourceType;
    }

    public void setSourceType(SourceType sourceType) {
        this.sourceType = sourceType;
    }

    public OffsetDateTime getProcessingDatetime() {
        return processingDatetime;
    }
}
