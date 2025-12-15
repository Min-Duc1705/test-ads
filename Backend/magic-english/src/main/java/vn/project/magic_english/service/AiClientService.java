package vn.project.magic_english.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

import vn.project.magic_english.config.MagicEnglishProperties;

@Service
@Slf4j
public class AiClientService {

    private final MagicEnglishProperties magicEnglishProperties;
    private final List<String> apiKeys = new ArrayList<>();
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${spring.ai.openai.chat.base-url}")
    private String baseUrl;

    @Value("${spring.ai.openai.chat.completions-path}")
    private String completionsPath;

    @Value("${spring.ai.openai.chat.options.model}")
    private String modelName;

    @Value("${spring.ai.openai.chat.options.temperature}")
    private Double temperature;

    private final AtomicInteger currentKeyIndex = new AtomicInteger(0);

    public AiClientService(MagicEnglishProperties magicEnglishProperties) {
        this.magicEnglishProperties = magicEnglishProperties;
    }

    @PostConstruct
    public void init() {
        List<String> configuredKeys = magicEnglishProperties.getAi().getApiKeys();
        if (configuredKeys == null || configuredKeys.isEmpty()) {
            throw new RuntimeException("No API keys configured in magic_english.ai.api-keys");
        }

        this.apiKeys.addAll(configuredKeys);
        log.info("Initialized AI Client Service with {} API keys", apiKeys.size());

        for (int i = 0; i < apiKeys.size(); i++) {
            String key = apiKeys.get(i);
            log.info("API Key {}: ...{}", i + 1,
                    key.length() > 4 ? key.substring(key.length() - 4) : key);
        }
    }

    /**
     * Generate content using AI with automatic key rotation on failure
     */
    public String generate(String prompt) {
        int maxRetries = apiKeys.size();
        int attempts = 0;
        Exception lastException = null;

        while (attempts < maxRetries) {
            int index = currentKeyIndex.get() % apiKeys.size();
            String currentKey = apiKeys.get(index);

            try {
                String result = callAiApi(prompt, currentKey);
                return result;
            } catch (HttpClientErrorException e) {
                lastException = e;
                HttpStatus statusCode = (HttpStatus) e.getStatusCode();

                // Check for quota/auth errors (429, 401, 403)
                if (statusCode == HttpStatus.TOO_MANY_REQUESTS ||
                        statusCode == HttpStatus.UNAUTHORIZED ||
                        statusCode == HttpStatus.FORBIDDEN) {
                    log.warn("API Key {} failed with status {}. Rotating to next key.",
                            index + 1, statusCode.value());
                    rotateKey();
                    attempts++;
                } else {
                    // Other client errors, try rotating anyway
                    log.warn("API call failed with status {}: {}. Trying next key.",
                            statusCode.value(), e.getMessage());
                    rotateKey();
                    attempts++;
                }
            } catch (Exception e) {
                lastException = e;
                log.warn("AI generation failed: {}. Trying next key.", e.getMessage());
                rotateKey();
                attempts++;
            }
        }

        throw new RuntimeException("Failed to generate content after trying all available API keys. Last error: "
                + (lastException != null ? lastException.getMessage() : "Unknown"));
    }

    private String callAiApi(String prompt, String apiKey) {
        String url = baseUrl + (completionsPath.startsWith("/") ? completionsPath : "/" + completionsPath);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        // Build request body
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", modelName);
        requestBody.put("temperature", temperature);

        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);
        messages.add(userMessage);
        requestBody.put("messages", messages);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

        if (response.getBody() != null) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
            if (choices != null && !choices.isEmpty()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                if (message != null) {
                    return (String) message.get("content");
                }
            }
        }

        throw new RuntimeException("Invalid response from AI API");
    }

    private void rotateKey() {
        int newIndex = currentKeyIndex.updateAndGet(i -> (i + 1) % apiKeys.size());
        log.info("Rotated to API key index: {}", newIndex + 1);
    }
}
