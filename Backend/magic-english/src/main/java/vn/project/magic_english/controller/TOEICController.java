package vn.project.magic_english.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.project.magic_english.model.request.GenerateTOEICTestRequest;
import vn.project.magic_english.model.request.StartTOEICTestRequest;
import vn.project.magic_english.model.request.SubmitTOEICTestRequest;
import vn.project.magic_english.model.response.TOEICTestHistoryResponse;
import vn.project.magic_english.model.response.TOEICTestResponse;
import vn.project.magic_english.model.response.TOEICTestResultResponse;
import vn.project.magic_english.service.TOEICService;
import vn.project.magic_english.utils.annotation.ApiMessage;
import vn.project.magic_english.utils.error.IdInvalidException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/toeic")
@RequiredArgsConstructor
public class TOEICController {

    private final TOEICService toeicService;

    /**
     * Generate a new TOEIC test using AI
     * POST /api/v1/toeic/generate
     */
    @PostMapping("/generate")
    @ApiMessage("Generate TOEIC test successfully")
    public ResponseEntity<TOEICTestResponse> generateTest(
            @Valid @RequestBody GenerateTOEICTestRequest request) throws JsonProcessingException {
        TOEICTestResponse response = toeicService.generateTest(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get test details by ID
     * GET /api/v1/toeic/tests/{testId}
     */
    @GetMapping("/tests/{testId}")
    @ApiMessage("Fetch test successfully")
    public ResponseEntity<TOEICTestResponse> getTest(@PathVariable Long testId) throws IdInvalidException {
        TOEICTestResponse response = toeicService.getTestById(testId);
        return ResponseEntity.ok(response);
    }

    /**
     * Start a test session
     * POST /api/v1/toeic/start
     */
    @PostMapping("/start")
    @ApiMessage("Start test successfully")
    public ResponseEntity<TOEICTestHistoryResponse> startTest(
            @Valid @RequestBody StartTOEICTestRequest request) throws IdInvalidException {
        TOEICTestHistoryResponse response = toeicService.startTest(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Submit test answers and get results
     * POST /api/v1/toeic/submit
     */
    @PostMapping("/submit")
    @ApiMessage("Submit test successfully")
    public ResponseEntity<TOEICTestResultResponse> submitTest(
            @Valid @RequestBody SubmitTOEICTestRequest request) throws IdInvalidException {
        TOEICTestResultResponse response = toeicService.submitTest(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Get user's test history
     * GET /api/v1/toeic/history
     */
    @GetMapping("/history")
    @ApiMessage("Fetch test history successfully")
    public ResponseEntity<List<TOEICTestHistoryResponse>> getUserHistory() throws IdInvalidException {
        List<TOEICTestHistoryResponse> history = toeicService.getUserHistory();
        return ResponseEntity.ok(history);
    }
}
