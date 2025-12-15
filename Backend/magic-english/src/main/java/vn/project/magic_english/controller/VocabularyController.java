package vn.project.magic_english.controller;

import jakarta.validation.Valid;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import vn.project.magic_english.model.request.AddVocabularyRequest;
import vn.project.magic_english.model.response.ResultPaginationDTO;
import vn.project.magic_english.model.response.VocabularyDetailResponse;
import vn.project.magic_english.service.VocabularyService;
import vn.project.magic_english.utils.SecurityUtil;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class VocabularyController {

    private final VocabularyService vocabularyService;

    /**
     * FR1.1 & FR1.2: Thêm từ mới và tự động làm giàu dữ liệu bằng AI
     * POST /api/v1/vocabulary
     */
    @PostMapping("/vocabulary")
    public ResponseEntity<VocabularyDetailResponse> addVocabulary(
            @Valid @RequestBody AddVocabularyRequest request) {
        VocabularyDetailResponse response = vocabularyService.addVocabulary(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get all vocabulary with optional search and pagination
     * GET /api/v1/vocabulary?search=keyword&page=0&size=10
     */
    @GetMapping("/vocabulary")
    public ResponseEntity<ResultPaginationDTO> getAllVocabulary(
            @RequestParam(value = "search", required = false) String search,
            Pageable pageable) {

        return ResponseEntity.ok(
                vocabularyService.handleGetAllVocabulary(search, pageable));
    }

    /**
     * Preview vocabulary data without saving to database
     * POST /api/v1/vocabulary/preview
     */
    @PostMapping("/vocabulary/preview")
    public ResponseEntity<VocabularyDetailResponse> previewVocabulary(
            @Valid @RequestBody AddVocabularyRequest request) {
        VocabularyDetailResponse response = vocabularyService.previewVocabulary(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Get vocabulary breakdown by word type (verb, noun, adjective, adverb)
     * GET /api/v1/vocabulary/breakdown
     * Response: {"verb": 120, "noun": 80, "adjective": 60, "adverb": 40, "other":
     * 10}
     */
    

}
