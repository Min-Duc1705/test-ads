package vn.project.magic_english.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import vn.project.magic_english.model.User;
import vn.project.magic_english.repository.GrammarRepository;
import vn.project.magic_english.repository.UserRepository;
import vn.project.magic_english.repository.VocabularyRepository;

@Service
@RequiredArgsConstructor
public class StatsService {
    private final VocabularyRepository vocabularyRepository;
    private final UserRepository userRepository;
    private final GrammarRepository grammarRepository;
    private final UserAchievementService userAchievementService;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Get vocabulary breakdown by word type (verb, noun, adjective, adverb)
     */
    public Map<String, Long> getVocabularyBreakdownByEmail(String email) {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        List<Object[]> results = vocabularyRepository.countByWordTypeForUser(user.getId());
        Map<String, Long> breakdown = new HashMap<>();

        // Initialize all types with 0
        breakdown.put("verb", 0L);
        breakdown.put("noun", 0L);
        breakdown.put("adjective", 0L);
        breakdown.put("adverb", 0L);
        breakdown.put("other", 0L);

        // Fill with actual data
        for (Object[] result : results) {
            String type = ((String) result[0]).toLowerCase();
            Long count = (Long) result[1];

            if (breakdown.containsKey(type)) {
                breakdown.put(type, count);
            } else {
                breakdown.put("other", breakdown.get("other") + count);
            }
        }

        return breakdown;
    }

    /**
     * Get CEFR level distribution (A1-C2)
     */
    public Map<String, Long> getCefrLevelDistributionByEmail(String email) {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        List<Object[]> results = vocabularyRepository.countByCefrLevelForUser(user.getId());
        Map<String, Long> distribution = new HashMap<>();

        // Initialize all levels with 0
        distribution.put("A1", 0L);
        distribution.put("A2", 0L);
        distribution.put("B1", 0L);
        distribution.put("B2", 0L);
        distribution.put("C1", 0L);
        distribution.put("C2", 0L);

        // Fill with actual data
        for (Object[] result : results) {
            String level = ((String) result[0]).toUpperCase();
            Long count = (Long) result[1];

            if (distribution.containsKey(level)) {
                distribution.put(level, count);
            }
        }

        return distribution;
    }

    /**
     * Get total vocabulary count for user
     */
    public Long getTotalVocabularyCountByEmail(String email) {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        return vocabularyRepository.countByUserId(user.getId());
    }

    /**
     * Get home statistics for current user
     * Returns: streakDays, wordsToday, totalWords, grammarChecks, avgGrammarScore
     */
    public Map<String, Object> getHomeStatsByEmail(String email) {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        Map<String, Object> stats = new HashMap<>();

        // Get today's vocabulary count
        Long wordsToday = vocabularyRepository.countTodayVocabularyByUserId(user.getId());
        stats.put("wordsToday", wordsToday);

        // Get total vocabulary count
        Long totalWords = vocabularyRepository.countByUserId(user.getId());
        stats.put("totalWords", totalWords);

        // Get current learning streak
        int currentStreak = calculateCurrentStreak(user.getId());
        stats.put("streakDays", currentStreak);

        // Kiểm tra và cấp achievement cho learning streak
        if (currentStreak > 0) {
            userAchievementService.checkAndGrantAchievements(user, "learning_streak", (long) currentStreak);
        }

        // Get today's grammar checks count
        Long grammarChecks = grammarRepository.countTodayGrammarChecksByUserId(user.getId());
        stats.put("grammarChecks", grammarChecks != null ? grammarChecks : 0L);

        // Get total grammar check count
        long totalChecks = grammarRepository.countByUserId(user.getId());
        stats.put("totalChecks", totalChecks);

        // Get today's average grammar score
        Double avgScoreToday = grammarRepository.getTodayAverageScoreByUserId(user.getId());
        stats.put("avgGrammarScore", avgScoreToday != null ? avgScoreToday.intValue() : 0);

        // Get average score (all time)
        Double avgScoreTotal = grammarRepository.getAverageScoreByUserId(user.getId());
        stats.put("avgGrammarScoreTotal", avgScoreTotal != null ? avgScoreTotal.intValue() : 0);

        // Get longest streak ever
        int longestStreak = calculateLongestStreak(user.getId());
        stats.put("longestStreak", longestStreak);

        return stats;
    }

    /**
     * Calculate current learning streak based on vocabulary and grammar activity
     * Streak = consecutive days with any activity (vocabulary or grammar)
     */
    private int calculateCurrentStreak(Long userId) {
        // Get all unique activity dates (vocabulary + grammar)
        List<java.sql.Date> vocabDates = userRepository.findAllVocabularyDatesByUserId(userId);
        List<java.sql.Date> grammarDates = userRepository.findAllGrammarDatesByUserId(userId);

        // Merge and sort all dates
        Set<LocalDate> allActivityDates = new HashSet<>();
        vocabDates.forEach(date -> allActivityDates.add(date.toLocalDate()));
        grammarDates.forEach(date -> allActivityDates.add(date.toLocalDate()));

        if (allActivityDates.isEmpty()) {
            return 0;
        }

        // Sort dates in descending order
        List<LocalDate> sortedDates = allActivityDates.stream()
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());

        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        // Check if streak is active (activity today or yesterday)
        LocalDate mostRecentDate = sortedDates.get(0);
        if (!mostRecentDate.equals(today) && !mostRecentDate.equals(yesterday)) {
            return 0; // Streak broken
        }

        // Count consecutive days
        int streak = 1;
        LocalDate currentDate = mostRecentDate;

        for (int i = 1; i < sortedDates.size(); i++) {
            LocalDate previousDate = sortedDates.get(i);
            long daysBetween = ChronoUnit.DAYS.between(previousDate, currentDate);

            if (daysBetween == 1) {
                // Consecutive day found
                streak++;
                currentDate = previousDate;
            } else {
                // Gap found, streak ends
                break;
            }
        }

        return streak;
    }

    /**
     * Calculate longest streak ever achieved by user
     * Finds the maximum consecutive days with activity (vocabulary or grammar)
     */
    private int calculateLongestStreak(Long userId) {
        // Get all unique activity dates (vocabulary + grammar)
        List<java.sql.Date> vocabDates = userRepository.findAllVocabularyDatesByUserId(userId);
        List<java.sql.Date> grammarDates = userRepository.findAllGrammarDatesByUserId(userId);

        // Merge and sort all dates
        Set<LocalDate> allActivityDates = new HashSet<>();
        vocabDates.forEach(date -> allActivityDates.add(date.toLocalDate()));
        grammarDates.forEach(date -> allActivityDates.add(date.toLocalDate()));

        if (allActivityDates.isEmpty()) {
            return 0;
        }

        // Sort dates in ascending order (oldest first)
        List<LocalDate> sortedDates = allActivityDates.stream()
                .sorted()
                .collect(Collectors.toList());

        // Find longest consecutive streak
        int longestStreak = 1;
        int currentStreak = 1;
        LocalDate previousDate = sortedDates.get(0);

        for (int i = 1; i < sortedDates.size(); i++) {
            LocalDate currentDate = sortedDates.get(i);
            long daysBetween = ChronoUnit.DAYS.between(previousDate, currentDate);

            if (daysBetween == 1) {
                // Consecutive day found
                currentStreak++;
                longestStreak = Math.max(longestStreak, currentStreak);
            } else {
                // Gap found, reset current streak
                currentStreak = 1;
            }
            previousDate = currentDate;
        }

        return longestStreak;
    }
}
