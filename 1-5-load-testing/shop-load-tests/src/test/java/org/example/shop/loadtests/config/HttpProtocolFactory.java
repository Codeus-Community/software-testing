package org.example.shop.loadtests.config;

import io.gatling.javaapi.http.HttpProtocolBuilder;

import static io.gatling.javaapi.http.HttpDsl.http;

public class HttpProtocolFactory {

    public static HttpProtocolBuilder create(String baseUrl) {
        return http
            .baseUrl(baseUrl)
            .acceptHeader("application/json")
            .contentTypeHeader("application/json");
    }
}
