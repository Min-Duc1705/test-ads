package vn.project.magic_english.model.response;

import lombok.Data;
import java.util.List;

/**
 * Response model for pronunciation feedback
 */
@Data
public class PronunciationFeedbackResponse {

    private String expectedWord;
    private String transcribedText;
    private int score; // 0-100
    private String accuracy; // excellent, good, fair, poor
    private String feedback;
    private List<String> suggestions;
    private PhonemeAnalysis phonemeAnalysis;
    private String overallAssessment;

    @Data
    public static class PhonemeAnalysis {
        private List<String> correctPhonemes;
        private List<String> problematicPhonemes;
    }
}
