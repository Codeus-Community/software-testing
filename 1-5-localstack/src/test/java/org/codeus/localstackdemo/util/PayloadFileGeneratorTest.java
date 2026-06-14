package org.codeus.localstackdemo.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PayloadFileGeneratorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void generatesExpectedJsonArrayShape() throws Exception {
        JsonNode jsonNode = objectMapper.readTree(PayloadFileGenerator.generatePayloadJson(2));

        assertThat(jsonNode.isArray()).isTrue();
        assertThat(jsonNode).hasSize(2);
        assertThat(jsonNode.get(0).get("id").asInt()).isEqualTo(1);
        assertThat(jsonNode.get(0).get("message").asText()).isEqualTo("message_1");
        assertThat(jsonNode.get(1).get("id").asInt()).isEqualTo(2);
        assertThat(jsonNode.get(1).get("message").asText()).isEqualTo("message_2");
    }
}
