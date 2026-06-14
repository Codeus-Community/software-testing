package org.codeus.localstackdemo.service;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.codeus.localstackdemo.config.DemoAppProperties;
import org.codeus.localstackdemo.domain.DemoPayload;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

@Service
public class S3PayloadReader {

    private final S3Client s3Client;
    private final DemoAppProperties properties;
    private final ObjectMapper objectMapper;

    public S3PayloadReader(S3Client s3Client, DemoAppProperties properties, ObjectMapper objectMapper) {
        this.s3Client = s3Client;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public List<DemoPayload> readPayloads() {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(properties.getS3().getBucket())
                .key(properties.getS3().getKey())
                .build();

        try (ResponseInputStream<GetObjectResponse> response = s3Client.getObject(request)) {
            return objectMapper.readValue(response, new TypeReference<>() {
            });
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to deserialize the payload file from S3", exception);
        }
    }
}
