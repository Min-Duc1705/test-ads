package vn.project.magic_english.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.project.magic_english.model.response.PronunciationFeedbackResponse;

/**
 * Service for AI-powered pronunciation assessment
 * Uses speech-to-text and AI analysis to evaluate pronunciation
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PronunciationService {

    private final AiClientService aiClientService;
    private final ObjectMapper objectMapper;

    /**
     * Analyze pronunciation based on transcribed text
     * Compares the transcribed speech with the expected word
     * 
     * @param expectedWord    The word the user was supposed to say
     * @param transcribedText The text transcribed from user's speech
     * @param ipa             The IPA pronunciation of the expected word
     * @return Pronunciation feedback with score and suggestions
     */
    public PronunciationFeedbackResponse analyzePronunciation(
            String expectedWord,
            String transcribedText,
            String ipa) {

        try {
            String prompt = buildAnalysisPrompt(expectedWord, transcribedText, ipa);
            String aiResponse = generateWithAI(prompt);
            return parseAIResponse(aiResponse, expectedWord, transcribedText);
        } catch (Exception e) {
            log.error("Error analyzing pronunciation: {}", e.getMessage());
            // Return a default response on error
            return createDefaultResponse(expectedWord, transcribedText);
        }
    }

    private String buildAnalysisPrompt(String expectedWord, String transcribedText, String ipa) {
        return String.format("""
                You are an expert English pronunciation coach. Analyze the user's pronunciation attempt.

                Expected word: "%s"
                IPA pronunciation: %s
                What the user said (transcribed): "%s"

                Analyze and provide feedback in the following JSON format only (no markdown, no code blocks):
                {
                    "score": <number from 0-100>,
                    "accuracy": "<excellent/good/fair/poor>",
                    "feedback": "<brief encouraging feedback message>",
                    "suggestions": ["<suggestion 1>", "<suggestion 2>"],
                    "phonemeAnalysis": {
                        "correctPhonemes": ["<correct sounds>"],
                        "problematicPhonemes": ["<sounds that need work>"]
                    },
                    "overallAssessment": "<detailed assessment of pronunciation>"
                }

                Scoring guidelines:
                - 90-100: Excellent - Nearly perfect pronunciation
                - 70-89: Good - Minor issues but clearly understandable
                - 50-69: Fair - Some pronunciation issues
                - 0-49: Needs practice - Significant pronunciation differences

                If the transcribed text is empty or completely different, give a low score but be encouraging.
                Always provide constructive feedback to help the learner improve.

                Return ONLY valid JSON, no additional text.
                """, expectedWord, ipa != null ? ipa : "N/A", transcribedText);
    }

    private String generateWithAI(String prompt) {
        return aiClientService.generate(prompt);
    }

    private PronunciationFeedbackResponse parseAIResponse(
            String aiResponse,
            String expectedWord,
            String transcribedText) {

        try {
            // Clean the response - remove markdown code blocks if present
            String cleanResponse = aiResponse
                    .replaceAll("```json\\s*", "")
                    .replaceAll("```\\s*", "")
                    .trim();

            JsonNode jsonNode = objectMapper.readTree(cleanResponse);

            PronunciationFeedbackResponse response = new PronunciationFeedbackResponse();
            response.setExpectedWord(expectedWord);
            response.setTranscribedText(transcribedText);
            response.setScore(jsonNode.has("score") ? jsonNode.get("score").asInt() : 50);
            response.setAccuracy(jsonNode.has("accuracy") ? jsonNode.get("accuracy").asText() : "fair");
            response.setFeedback(jsonNode.has("feedback") ? jsonNode.get("feedback").asText() : "Keep practicing!");
            response.setOverallAssessment(
                    jsonNode.has("overallAssessment") ? jsonNode.get("overallAssessment").asText() : "");

            // Parse suggestions
            if (jsonNode.has("suggestions") && jsonNode.get("suggestions").isArray()) {
                java.util.List<String> suggestions = new java.util.ArrayList<>();
                for (JsonNode suggestion : jsonNode.get("suggestions")) {
                    suggestions.add(suggestion.asText());
                }
                response.setSuggestions(suggestions);
            }

            // Parse phoneme analysis
            if (jsonNode.has("phonemeAnalysis")) {
                JsonNode phonemeNode = jsonNode.get("phonemeAnalysis");
                PronunciationFeedbackResponse.PhonemeAnalysis phonemeAnalysis = new PronunciationFeedbackResponse.PhonemeAnalysis();

                if (phonemeNode.has("correctPhonemes") && phonemeNode.get("correctPhonemes").isArray()) {
                    java.util.List<String> correct = new java.util.ArrayList<>();
                    for (JsonNode p : phonemeNode.get("correctPhonemes")) {
                        correct.add(p.asText());
                    }
                    phonemeAnalysis.setCorrectPhonemes(correct);
                }

                if (phonemeNode.has("problematicPhonemes") && phonemeNode.get("problematicPhonemes").isArray()) {
                    java.util.List<String> problematic = new java.util.ArrayList<>();
                    for (JsonNode p : phonemeNode.get("problematicPhonemes")) {
                        problematic.add(p.asText());
                    }
                    phonemeAnalysis.setProblematicPhonemes(problematic);
                }

                response.setPhonemeAnalysis(phonemeAnalysis);
            }

            return response;
        } catch (Exception e) {
            log.error("Error parsing AI response: {}", e.getMessage());
            return createDefaultResponse(expectedWord, transcribedText);
        }
    }

    private PronunciationFeedbackResponse createDefaultResponse(String expectedWord, String transcribedText) {
        PronunciationFeedbackResponse response = new PronunciationFeedbackResponse();
        response.setExpectedWord(expectedWord);
        response.setTranscribedText(transcribedText);

        // Calculate basic similarity score
        int score = calculateBasicSimilarity(expectedWord, transcribedText);
        response.setScore(score);

        if (score >= 90) {
            response.setAccuracy("excellent");
            response.setFeedback("Excellent pronunciation! Keep up the great work!");
        } else if (score >= 70) {
            response.setAccuracy("good");
            response.setFeedback("Good job! Your pronunciation is clear and understandable.");
        } else if (score >= 50) {
            response.setAccuracy("fair");
            response.setFeedback("Nice try! Practice a bit more to improve your pronunciation.");
        } else {
            response.setAccuracy("poor");
            response.setFeedback("Keep practicing! Listen to the word again and try to match the sounds.");
        }

        response.setSuggestions(java.util.List.of(
                "Listen to the audio carefully before recording",
                "Practice speaking slowly and clearly"));

        return response;
    }

    private int calculateBasicSimilarity(String expected, String transcribed) {
        if (transcribed == null || transcribed.isEmpty()) {
            return 0;
        }

        String normalizedExpected = expected.toLowerCase().trim();
        String normalizedTranscribed = transcribed.toLowerCase().trim();

        if (normalizedExpected.equals(normalizedTranscribed)) {
            return 100;
        }

        // Calculate Levenshtein distance based similarity
        int distance = levenshteinDistance(normalizedExpected, normalizedTranscribed);
        int maxLen = Math.max(normalizedExpected.length(), normalizedTranscribed.length());

        if (maxLen == 0)
            return 100;

        double similarity = (1.0 - (double) distance / maxLen) * 100;
        return Math.max(0, Math.min(100, (int) similarity));
    }

    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost);
            }
        }

        return dp[s1.length()][s2.length()];
    }
}
