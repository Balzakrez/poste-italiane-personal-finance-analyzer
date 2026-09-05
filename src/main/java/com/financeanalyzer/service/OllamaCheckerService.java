package com.financeanalyzer.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class OllamaCheckerService {

    private static final Logger log = LoggerFactory.getLogger(OllamaCheckerService.class);

    private static final Duration TIMEOUT_VERIFICA = Duration.ofSeconds(2);

    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(TIMEOUT_VERIFICA).build();

    private final String baseUrl;

    /* ********************************************************************************************** */

    public OllamaCheckerService(@Value("${spring.ai.ollama.base-url}") String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public boolean isDisponibile() {
        try {
            HttpRequest richiesta = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl))
                    .timeout(TIMEOUT_VERIFICA)
                    .GET()
                    .build();

            HttpResponse<Void> risposta = httpClient.send(richiesta, HttpResponse.BodyHandlers.discarding());

            return risposta.statusCode() == 200;
        } catch (Exception e) {
            log.warn("Ollama non raggiungibile su {}: categorizzazione Livello 2 saltata per questo import", baseUrl);
            return false;
        }
    }
}