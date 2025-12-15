package vn.project.magic_english.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import vn.project.magic_english.service.StatsService;
import vn.project.magic_english.utils.SecurityUtil;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class StatsController {

    private final StatsService statsService;

    @GetMapping("/vocabulary/breakdown")
    public ResponseEntity<Map<String, Long>> getVocabularyBreakdown() {
        String email = SecurityUtil.getCurrentUserLogin().orElseThrow(
                () -> new RuntimeException("User not authenticated"));
        Map<String, Long> breakdown = statsService.getVocabularyBreakdownByEmail(email);
        return ResponseEntity.ok(breakdown);
    }

    /**
     * Get CEFR level distribution (A1-C2)
     * GET /api/v1/vocabulary/cefr-distribution
     * Response: {"A1": 30, "A2": 50, "B1": 90, "B2": 70, "C1": 20, "C2": 10}
     */
    @GetMapping("/vocabulary/cefr-distribution")
    public ResponseEntity<Map<String, Long>> getCefrDistribution() {
        String email = SecurityUtil.getCurrentUserLogin().orElseThrow(
                () -> new RuntimeException("User not authenticated"));
        Map<String, Long> distribution = statsService.getCefrLevelDistributionByEmail(email);
        return ResponseEntity.ok(distribution);
    }

    /**
     * Get total vocabulary count for current user
     * GET /api/v1/vocabulary/count
     * Response: 1204
     */
    @GetMapping("/vocabulary/count")
    public ResponseEntity<Long> getTotalVocabularyCount() {
        String email = SecurityUtil.getCurrentUserLogin().orElseThrow(
                () -> new RuntimeException("User not authenticated"));
        Long count = statsService.getTotalVocabularyCountByEmail(email);
        return ResponseEntity.ok(count);
    }

    /**
     * Get home statistics for dashboard
     * GET /api/v1/vocabulary/home-stats
     * Response: {"wordsToday": 5, "totalWords": 120, "streakDays": 0,
     * "grammarChecks": 0, "avgGrammarScore": 85}
     */
    @GetMapping("/vocabulary/home-stats")
    public ResponseEntity<Map<String, Object>> getHomeStats() {
        String email = SecurityUtil.getCurrentUserLogin().orElseThrow(
                () -> new RuntimeException("User not authenticated"));

        // Get all home stats (vocabulary + grammar)
        Map<String, Object> stats = statsService.getHomeStatsByEmail(email);

        return ResponseEntity.ok(stats);
    }
}
