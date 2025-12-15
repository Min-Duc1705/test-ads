package vn.project.magic_english.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.project.magic_english.model.Achievement;
import vn.project.magic_english.model.User;
import vn.project.magic_english.model.UserAchievement;
import vn.project.magic_english.repository.UserRepository;
import vn.project.magic_english.service.UserAchievementService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/user-achievements")
public class UserAchievementController {

    @Autowired
    private UserAchievementService userAchievementService;

    @Autowired
    private UserRepository userRepository;

    /**
     * Trigger check and grant achievements
     * Returns list of newly unlocked achievements directly (no wrapper)
     */
    @PostMapping("/check")
    public ResponseEntity<List<Achievement>> checkAchievements(Authentication authentication,
            @RequestBody Map<String, Object> payload) {
        // Debug: Request ID để track multiple requests
        String requestId = java.util.UUID.randomUUID().toString().substring(0, 8);
        System.out.println("\n🔔 [" + requestId + "] New /check request at " + java.time.LocalTime.now());

        String email = authentication.getName();
        User user = userRepository.findByEmail(email);

        if (user == null) {
            throw new RuntimeException("User not found");
        }

        String metricType = (String) payload.get("metricType");
        // Handle both Integer and Long coming from JSON
        Number valueNum = (Number) payload.get("currentValue");
        Long currentValue = valueNum.longValue();

        System.out.println("📊 [" + requestId + "] Check achievements for " + user.getName() +
                " | metricType=" + metricType +
                " | currentValue=" + currentValue);

        List<Achievement> newAchievements = userAchievementService.checkAndGrantAchievements(user, metricType,
                currentValue);

        System.out.println("📊 [" + requestId + "] Result: newAchievements=" + newAchievements.size());

        if (!newAchievements.isEmpty()) {
            System.out.println("✅ [" + requestId + "] Returning " + newAchievements.size() + " achievements to client");
            for (Achievement a : newAchievements) {
                System.out.println("   - " + a.getTitle() + " (id=" + a.getId() + ")");
            }
        } else {
            System.out.println("ℹ️ [" + requestId + "] No new achievements to return");
        }

        // Return list directly - FormatRestResponse will wrap it
        return ResponseEntity.ok(newAchievements);
    }

    /**
     * Get validated user achievements
     * Returns list directly (no wrapper)
     */
    @GetMapping
    public ResponseEntity<List<UserAchievement>> getUserAchievements(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email);

        if (user == null) {
            throw new RuntimeException("User not found");
        }

        List<UserAchievement> userAchievements = userAchievementService.getUserAchievements(user.getId());
        return ResponseEntity.ok(userAchievements);
    }

    /**
     * Reset all achievements for current user (for testing purposes)
     */
    @DeleteMapping("/reset")
    public ResponseEntity<String> resetAchievements(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email);

        if (user == null) {
            throw new RuntimeException("User not found");
        }

        userAchievementService.resetUserAchievements(user.getId());
        return ResponseEntity.ok("Achievements reset successfully for user: " + user.getName());
    }

    /**
     * Get all achievements in the system (for showing locked/unlocked status)
     */
    @GetMapping("/all")
    public ResponseEntity<List<Achievement>> getAllAchievements() {
        List<Achievement> achievements = userAchievementService.getAllAchievements();
        return ResponseEntity.ok(achievements);
    }
}
