package com.email.writer.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
public class EmailGeneratorService {

    private final WebClient webClient;

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    public EmailGeneratorService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public String generateEmailReply(EmailRequest emailRequest) {
        String cleanApiKey = getCleanApiKey();
        if (cleanApiKey == null || cleanApiKey.isEmpty()) {
            throw new IllegalArgumentException("Gemini API key is not configured. Please set the GEMINI_API_KEY environment variable or gemini.api.key property in application.properties.");
        }

        // Build the prompt
        String prompt = buildPrompt(emailRequest);

        // Craft a request
        Map<String, Object> requestBody = Map.of(
                "contents", new Object[] {
                       Map.of("parts", new Object[]{
                               Map.of("text", prompt)
                       })
                }
        );

        // Determine URL with API key
        String targetUrl;
        if (geminiApiUrl.contains("?key=")) {
            targetUrl = geminiApiUrl + cleanApiKey;
        } else if (geminiApiUrl.contains("?")) {
            targetUrl = geminiApiUrl + "&key=" + cleanApiKey;
        } else {
            targetUrl = geminiApiUrl + "?key=" + cleanApiKey;
        }

        // Do request and get response
        String response = webClient.post()
                .uri(targetUrl)
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(
                        status -> status.isError(),
                        clientResponse -> clientResponse.bodyToMono(String.class)
                                .map(body -> new RuntimeException(
                                        "Gemini API error: "
                                                + clientResponse.statusCode()
                                                + " - " + body))
                )
                .bodyToMono(String.class)
                .block();

        // Extract Response and Return
        return extractResponseContent(response);
    }

    private String extractResponseContent(String response) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(response);
            return rootNode.path("candidates")
                    .get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text")
                    .asText();
        } catch (Exception e) {
            return "Error processing request: " + e.getMessage();
        }
    }

    private String buildPrompt(EmailRequest emailRequest) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Generate a professional email reply for the following email content. Please don't generate a subject line ");
        if (emailRequest.getTone() != null && !emailRequest.getTone().isEmpty()) {
            prompt.append("Use a ").append(emailRequest.getTone()).append(" tone.");
        }
        prompt.append("\nOriginal email: \n").append(emailRequest.getEmailContent());
        return prompt.toString();
    }

    private String getCleanApiKey() {
        if (geminiApiKey == null) {
            return null;
        }
        String key = geminiApiKey.trim();
        // Remove surrounding quotes if present
        if ((key.startsWith("\"") && key.endsWith("\"")) || (key.startsWith("'") && key.endsWith("'"))) {
            key = key.substring(1, key.length() - 1).trim();
        }
        // Remove common accidental prefixes
        if (key.startsWith("GEMINI_API_KEY:")) {
            key = key.substring("GEMINI_API_KEY:".length()).trim();
        } else if (key.startsWith("GEMINI_API_KEY=")) {
            key = key.substring("GEMINI_API_KEY=".length()).trim();
        }
        return key;
    }
}
