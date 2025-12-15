package vn.project.magic_english.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.project.magic_english.model.request.PronunciationAnalysisRequest;
import vn.project.magic_english.model.response.PronunciationFeedbackResponse;
import vn.project.magic_english.service.PronunciationService;
import vn.project.magic_english.utils.annotation.ApiMessage;

/**
 * Controller for pronunciation assessment API
 */
@RestController
@RequestMapping("/api/v1/pronunciation")
@RequiredArgsConstructor
public class PronunciationController {

    private final PronunciationService pronunciationService;

    /**
     * Analyze pronunciation based on transcribed speech
     * POST /api/v1/pronunciation/analyze
     * 
     * @param request Contains expected word and transcribed text
     * @return Pronunciation feedback with score and suggestions
     */
    @PostMapping("/analyze")
    @ApiMessage("Pronunciation analyzed successfully")
    public ResponseEntity<PronunciationFeedbackResponse> analyzePronunciation(
            @Valid @RequestBody PronunciationAnalysisRequest request) {

        PronunciationFeedbackResponse response = pronunciationService.analyzePronunciation(
                request.getExpectedWord(),
                request.getTranscribedText(),
                request.getIpa());

        return ResponseEntity.ok(response);
    }
}
