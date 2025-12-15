package vn.project.magic_english.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * Proxy controller to stream audio from external TTS APIs
 * This solves CORS and compatibility issues with audioplayers on mobile
 */
@RestController
@RequestMapping("/api/audio")
@Slf4j
@CrossOrigin(origins = "*")
public class AudioProxyController {

    @Value("${voicerss.api-key:}")
    private String voiceRssApiKey;

    /**
     * Proxy endpoint to stream audio from VoiceRSS or other TTS services
     * Usage: GET /api/audio/proxy?url=<encoded_audio_url>
     */
    @GetMapping(value = "/proxy", produces = "audio/mpeg")
    public ResponseEntity<InputStreamResource> proxyAudio(@RequestParam String url) {
        try {
            log.info("Proxying audio from URL: {}", url);

            URL audioUrl = new URL(url);
            URLConnection connection = audioUrl.openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            InputStream inputStream = connection.getInputStream();
            InputStreamResource resource = new InputStreamResource(inputStream);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("audio/mpeg"));
            headers.add("Accept-Ranges", "bytes");
            headers.add("Cache-Control", "public, max-age=3600");

            log.info("Successfully streaming audio");
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);

        } catch (Exception e) {
            log.error("Error proxying audio: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Direct audio endpoint - generates TTS on demand
     * Usage: GET /api/audio/tts?text=<encoded_text>
     * Uses API key from application.yaml config
     */
    @GetMapping(value = "/tts", produces = "audio/mpeg")
    public ResponseEntity<InputStreamResource> generateTTS(@RequestParam(name = "text") String text) {
        try {
            // Decode the text in case it's URL encoded
            try {
                text = java.net.URLDecoder.decode(text, java.nio.charset.StandardCharsets.UTF_8);
            } catch (Exception e) {
                log.warn("Could not decode text, using as-is");
            }

            log.info("Generating TTS for text length: {} chars", text.length());

            // Check for speaker labels to determine if we need multi-voice generation
            // Simple parsing for "Man:", "Woman:", "Male:", "Female:"
            if (hasSpeakerLabels(text)) {
                return generateMultiVoiceTTS(text);
            }

            // Default single voice (Alice - UK Female)
            byte[] audioBytes = fetchVoiceRSSAudio(text, "Alice");

            return createAudioResponse(audioBytes);

        } catch (Exception e) {
            log.error("Error generating TTS: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private boolean hasSpeakerLabels(String text) {
        String lower = text.toLowerCase();
        return lower.contains("man:") || lower.contains("woman:") ||
                lower.contains("male:") || lower.contains("female:");
    }

    private ResponseEntity<InputStreamResource> generateMultiVoiceTTS(String text) throws Exception {
        java.io.ByteArrayOutputStream combinedAudio = new java.io.ByteArrayOutputStream();

        // Split by speaker labels but keep the delimiters or reconstruct
        // Logic: find first occurrence, determine speaker, find next occurrence...
        // Simplified approach: Split by regex and match alternating parts?
        // Better: simple string scanning

        String remaining = text;
        String currentVoice = "Alice"; // Default start

        while (!remaining.isEmpty()) {
            // Find next speaker marker
            int idxMan = indexOfIgnoreCase(remaining, "Man:");
            int idxWoman = indexOfIgnoreCase(remaining, "Woman:");
            int idxMale = indexOfIgnoreCase(remaining, "Male:");
            int idxFemale = indexOfIgnoreCase(remaining, "Female:");

            // Normalize "Man"/"Male" -> Harry, "Woman"/"Female" -> Alice
            int nextIdx = -1;
            String nextVoice = null;
            int offset = 0;

            // Find the earliest marker
            if (idxMan != -1 && (nextIdx == -1 || idxMan < nextIdx)) {
                nextIdx = idxMan;
                nextVoice = "Harry";
                offset = 4;
            } // "Man:".length
            if (idxMale != -1 && (nextIdx == -1 || idxMale < nextIdx)) {
                nextIdx = idxMale;
                nextVoice = "Harry";
                offset = 5;
            } // "Male:".length
            if (idxWoman != -1 && (nextIdx == -1 || idxWoman < nextIdx)) {
                nextIdx = idxWoman;
                nextVoice = "Alice";
                offset = 6;
            } // "Woman:".length
            if (idxFemale != -1 && (nextIdx == -1 || idxFemale < nextIdx)) {
                nextIdx = idxFemale;
                nextVoice = "Alice";
                offset = 7;
            } // "Female:".length

            if (nextIdx == -1) {
                // No more markers, process rest with current voice
                if (!remaining.trim().isEmpty()) {
                    byte[] validBytes = fetchVoiceRSSAudio(remaining.trim(), currentVoice);
                    combinedAudio.write(validBytes);
                }
                break;
            }

            if (nextIdx > 0) {
                // Process content before the marker
                String segment = remaining.substring(0, nextIdx).trim();
                if (!segment.isEmpty()) {
                    byte[] validBytes = fetchVoiceRSSAudio(segment, currentVoice);
                    combinedAudio.write(validBytes);
                }
            }

            // Update voice and advance
            currentVoice = nextVoice;
            remaining = remaining.substring(nextIdx + offset);
        }

        return createAudioResponse(combinedAudio.toByteArray());
    }

    private int indexOfIgnoreCase(String str, String search) {
        String lowerStr = str.toLowerCase();
        String lowerSearch = search.toLowerCase();
        return lowerStr.indexOf(lowerSearch);
    }

    private byte[] fetchVoiceRSSAudio(String text, String voice) throws Exception {
        if (text == null || text.trim().isEmpty())
            return new byte[0];

        String encodedText = java.net.URLEncoder.encode(text, java.nio.charset.StandardCharsets.UTF_8);
        String voiceRssUrl = "https://api.voicerss.org/?key=" + voiceRssApiKey +
                "&hl=en-gb&v=" + voice + "&src=" + encodedText +
                "&c=MP3&f=44khz_16bit_mono&r=-4";

        URL audioUrl = new URL(voiceRssUrl);
        URLConnection connection = audioUrl.openConnection();
        connection.setRequestProperty("User-Agent", "Mozilla/5.0");
        connection.setConnectTimeout(30000);
        connection.setReadTimeout(60000);

        InputStream inputStream = connection.getInputStream();
        byte[] audioBytes = inputStream.readAllBytes();
        inputStream.close();

        // Basic error check
        if (audioBytes.length < 1000) {
            String response = new String(audioBytes, java.nio.charset.StandardCharsets.UTF_8);
            if (response.startsWith("ERROR")) {
                throw new Exception("VoiceRSS Error: " + response);
            }
        }
        return audioBytes;
    }

    private ResponseEntity<InputStreamResource> createAudioResponse(byte[] audioBytes) {
        InputStreamResource resource = new InputStreamResource(
                new java.io.ByteArrayInputStream(audioBytes));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("audio/mpeg"));
        headers.setContentLength(audioBytes.length);
        headers.add("Accept-Ranges", "bytes");
        headers.add("Cache-Control", "public, max-age=3600");

        log.info("Successfully generated Audio, size: {} bytes", audioBytes.length);
        return ResponseEntity.ok()
                .headers(headers)
                .body(resource);
    }

    /**
     * Simple test endpoint to verify audio streaming works
     */
    @GetMapping(value = "/test", produces = "audio/mpeg")
    public ResponseEntity<String> testAudio() {
        return ResponseEntity.ok("Audio proxy is working");
    }
}
