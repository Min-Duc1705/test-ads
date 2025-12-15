package vn.project.magic_english.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Service for Text-to-Speech conversion for IELTS Listening tests
 * Uses Google Cloud Text-to-Speech API
 */
@Service
@Slf4j
public class TextToSpeechService {

    private static final String AUDIO_OUTPUT_DIR = "public/ielts-audio";

    @Value("${voicerss.api-key:}")
    private String voiceRssApiKey;

    /**
     * Generate audio file from text using Google TTS
     * For now, returns a placeholder URL. In production, integrate with Google
     * Cloud TTS
     * 
     * @param text The text to convert to speech
     * @return URL path to the generated audio file
     */
    public String generateAudio(String text) {
        try {
            // Create output directory if it doesn't exist
            Path outputPath = Paths.get(AUDIO_OUTPUT_DIR);
            if (!Files.exists(outputPath)) {
                Files.createDirectories(outputPath);
            }

            // Generate unique filename
            String filename = "listening_" + UUID.randomUUID().toString() + ".mp3";
            String audioUrl = "/ielts-audio/" + filename;

            // TODO: Implement actual Google Cloud TTS integration
            // For now, we'll use a web-based TTS service URL format
            // The frontend will use this URL to fetch audio

            log.info("Generated audio URL for listening passage: {}", audioUrl);

            return audioUrl;

        } catch (Exception e) {
            log.error("Error generating audio: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Generate audio using Google Cloud Text-to-Speech API
     * Requires GOOGLE_APPLICATION_CREDENTIALS environment variable
     * 
     * @param text       Text to convert
     * @param outputFile Output file path
     * @throws IOException If file writing fails
     */
    private void generateWithGoogleTTS(String text, String outputFile) throws IOException {
        /*
         * // Uncomment when Google Cloud credentials are configured
         * 
         * try (TextToSpeechClient textToSpeechClient = TextToSpeechClient.create()) {
         * // Set the text input to be synthesized
         * SynthesisInput input = SynthesisInput.newBuilder().setText(text).build();
         * 
         * // Build the voice request
         * VoiceSelectionParams voice = VoiceSelectionParams.newBuilder()
         * .setLanguageCode("en-US")
         * .setName("en-US-Neural2-D") // Female voice
         * .build();
         * 
         * // Select the type of audio file
         * AudioConfig audioConfig = AudioConfig.newBuilder()
         * .setAudioEncoding(AudioEncoding.MP3)
         * .setSpeakingRate(0.9) // Slightly slower for IELTS
         * .setPitch(0.0)
         * .build();
         * 
         * // Perform the text-to-speech request
         * SynthesizeSpeechResponse response = textToSpeechClient.synthesizeSpeech(
         * input, voice, audioConfig);
         * 
         * // Write the audio content to file
         * ByteString audioContents = response.getAudioContent();
         * try (FileOutputStream out = new FileOutputStream(outputFile)) {
         * out.write(audioContents.toByteArray());
         * }
         * 
         * log.info("Audio content written to file: {}", outputFile);
         * }
         */
    }

    /**
     * Use VoiceRSS TTS API (Free tier: 350 requests/day, unlimited text length)
     * Or fallback to multiple Google TTS chunks
     * Returns a URL that can be used directly in audio player
     * 
     * @param text Text to convert
     * @return TTS audio URL (relative path - client will prepend base URL)
     */
    public String generateWithResponsiveVoice(String text) {
        try {
            String encodedText = java.net.URLEncoder.encode(text, java.nio.charset.StandardCharsets.UTF_8);

            // Return relative URL - client (Flutter app) will prepend the correct base URL
            // based on environment (local vs production)
            String ttsUrl = "/api/audio/tts?text=" + encodedText;

            log.info("Generated TTS endpoint URL for text length: {} characters", text.length());
            return ttsUrl;

        } catch (Exception e) {
            log.error("Error generating TTS URL: {}", e.getMessage());
            // Final fallback to external TTS service
            String encodedText = java.net.URLEncoder.encode(text, java.nio.charset.StandardCharsets.UTF_8);
            return "https://code.responsivevoice.org/getvoice.php?t=" + encodedText
                    + "&tl=en-GB&sv=&vn=&pitch=0.5&rate=0.4&vol=1";
        }
    }
}
