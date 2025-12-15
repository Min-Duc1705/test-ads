package vn.project.magic_english.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import vn.project.magic_english.model.request.CheckGrammarRequest;
import vn.project.magic_english.model.response.GrammarCheckResponse;
import vn.project.magic_english.model.response.ResultPaginationDTO;
import vn.project.magic_english.service.GrammarService;

@RestController
@RequestMapping("/api/v1/grammar")
@RequiredArgsConstructor
public class GrammarController {

    private final GrammarService grammarService;

    /**
     * Check grammar for input text
     * POST /api/v1/grammar/check
     */
    @PostMapping("/check")
    public ResponseEntity<GrammarCheckResponse> checkGrammar(
            @Valid @RequestBody CheckGrammarRequest request) {
        GrammarCheckResponse response = grammarService.checkGrammar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get all grammar checks with pagination
     * GET /api/v1/grammar?page=0&size=20
     */
    @GetMapping
    public ResponseEntity<ResultPaginationDTO> getAllGrammarChecks(Pageable pageable) {
        return ResponseEntity.ok(grammarService.handleGetAllGrammarChecks(pageable));
    }

    /**
     * Get grammar check by ID
     * GET /api/v1/grammar/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<GrammarCheckResponse> getGrammarCheckById(@PathVariable Long id) {
        GrammarCheckResponse response = grammarService.getGrammarCheckById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete grammar check
     * DELETE /api/v1/grammar/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGrammarCheck(@PathVariable Long id) {
        grammarService.deleteGrammarCheck(id);
        return ResponseEntity.noContent().build();
    }

}
