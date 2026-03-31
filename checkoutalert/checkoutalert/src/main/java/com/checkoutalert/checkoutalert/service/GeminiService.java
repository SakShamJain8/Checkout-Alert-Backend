package com.checkoutalert.checkoutalert.service;
import java.util.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class GeminiService {
    @Value("${gemini.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/" +
                    "gemini-2.5-flash:generateContent?key=";

    public String getPossibleCause(String endpointName, String endpointUrl,
                                   long currentLatencyMs, double baselineLatencyMs,
                                   int statusCode) {
        try {
            String prompt = buildPrompt(endpointName, endpointUrl,
                    currentLatencyMs, baselineLatencyMs, statusCode);

            // build request body
            Map<String, Object> body = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(
                                    Map.of("text", prompt)
                            ))
                    )
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    GEMINI_URL + apiKey, request, Map.class
            );

            return extractText(response.getBody());

        } catch (Exception e) {
            System.out.println("Gemini error: " + e.getMessage());
            return "Unable to determine cause — check your logs.";
        }
    }

    private String buildPrompt(String name, String url, long current,
                               double baseline, int status) {
        return "You are an API monitoring assistant. " +
                "An API anomaly was detected. Give ONE short sentence (max 15 words) " +
                "as a possible cause. Be specific and technical. No fluff.\n\n" +
                "Endpoint: " + name + " (" + url + ")\n" +
                "Current latency: " + current + "ms\n" +
                "Normal baseline: " + (int) baseline + "ms\n" +
                "Status code: " + status + "\n\n" +
                "Possible cause:";
    }

    @SuppressWarnings("unchecked")
    private String extractText(Map responseBody) {
        try {
            List<Map> candidates = (List<Map>) responseBody.get("candidates");
            Map content = (Map) candidates.get(0).get("content");
            List<Map> parts = (List<Map>) content.get("parts");
            return (String) parts.get(0).get("text");
        } catch (Exception e) {
            return "Possible cause: unknown — check server logs.";
        }
    }
}
