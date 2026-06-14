package org.codeus.localstackdemo.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public final class PayloadFileGenerator {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private PayloadFileGenerator() {
    }

    public static ArrayNode generatePayloadArray(int count) {
        ArrayNode arrayNode = OBJECT_MAPPER.createArrayNode();
        for (int index = 1; index <= count; index++) {
            ObjectNode payloadNode = OBJECT_MAPPER.createObjectNode();
            payloadNode.put("id", index);
            payloadNode.put("message", "message_" + index);
            arrayNode.add(payloadNode);
        }
        return arrayNode;
    }

    public static String generatePayloadJson(int count) {
        try {
            return OBJECT_MAPPER.writeValueAsString(generatePayloadArray(count));
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to generate payload JSON", exception);
        }
    }

    public static void writeToFile(Path targetPath, int count) {
        try {
            Path parent = targetPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(targetPath, generatePayloadJson(count));
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to write generated payload file", exception);
        }
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            throw new IllegalArgumentException("Usage: PayloadFileGenerator <targetPath> <recordCount>");
        }
        writeToFile(Path.of(args[0]), Integer.parseInt(args[1]));
    }
}
