package vn.project.magic_english.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request model for pronunciation analysis
 */
@Data
public class PronunciationAnalysisRequest {

    @NotBlank(message = "Expected word is required")
    private String expectedWord;

    @NotBlank(message = "Transcribed text is required")
    private String transcribedText;

    private String ipa; // Optional IPA pronunciation
}
