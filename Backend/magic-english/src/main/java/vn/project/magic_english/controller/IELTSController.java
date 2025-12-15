package vn.project.magic_english.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.project.magic_english.model.request.GenerateIELTSTestRequest;
import vn.project.magic_english.model.request.StartIELTSTestRequest;
import vn.project.magic_english.model.request.SubmitIELTSTestRequest;
import vn.project.magic_english.model.response.IELTSTestHistoryResponse;
import vn.project.magic_english.model.response.IELTSTestResponse;
import vn.project.magic_english.model.response.IELTSTestResultResponse;
import vn.project.magic_english.service.IELTSService;
import vn.project.magic_english.utils.annotation.ApiMessage;
import vn.project.magic_english.utils.error.IdInvalidException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ielts")
@RequiredArgsConstructor
public class IELTSController {

    private final IELTSService ieltsService;

    /**
     * Generate a new IELTS test using AI
     * POST /api/v1/ielts/generate
     */
    @PostMapping("/generate")
    @ApiMessage("Generate IELTS test successfully")
    public ResponseEntity<IELTSTestResponse> generateTest(
            @Valid @RequestBody GenerateIELTSTestRequest request) throws JsonProcessingException {
        IELTSTestResponse response = ieltsService.generateTest(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get test details by ID
     * GET /api/v1/ielts/tests/{testId}
     */
    @GetMapping("/tests/{testId}")
    @ApiMessage("Fetch test successfully")
    public ResponseEntity<IELTSTestResponse> getTest(@PathVariable Long testId) throws IdInvalidException {
        IELTSTestResponse response = ieltsService.getTestById(testId);
        return ResponseEntity.ok(response);
    }

    /**
     * Start a test session
     * POST /api/v1/ielts/start
     */
    @PostMapping("/start")
    @ApiMessage("Start test successfully")
    public ResponseEntity<IELTSTestHistoryResponse> startTest(
            @Valid @RequestBody StartIELTSTestRequest request) throws IdInvalidException {
        IELTSTestHistoryResponse response = ieltsService.startTest(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Submit test answers and get results
     * POST /api/v1/ielts/submit
     */
    @PostMapping("/submit")
    @ApiMessage("Submit test successfully")
    public ResponseEntity<IELTSTestResultResponse> submitTest(
            @Valid @RequestBody SubmitIELTSTestRequest request) throws IdInvalidException {
        IELTSTestResultResponse response = ieltsService.submitTest(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Get user's test history
     * GET /api/v1/ielts/history
     */
    @GetMapping("/history")
    @ApiMessage("Fetch test history successfully")
    public ResponseEntity<List<IELTSTestHistoryResponse>> getUserHistory() throws IdInvalidException {
        List<IELTSTestHistoryResponse> history = ieltsService.getUserHistory();
        return ResponseEntity.ok(history);
    }
}
