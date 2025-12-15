package vn.project.magic_english.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.project.magic_english.model.Achievement;
import vn.project.magic_english.model.User;
import vn.project.magic_english.model.UserAchievement;
import vn.project.magic_english.repository.AchievementRepository;
import vn.project.magic_english.repository.UserAchievementRepository;

import java.time.Instant;
import java.util.List;

@Service
public class UserAchievementService {
    @Autowired
    private AchievementRepository achievementRepository;

    @Autowired
    private UserAchievementRepository userAchievementRepository;

    /**
     * Method chính: Kiểm tra và cấp thành tựu cho user
     * 
     * @param user         - User cần kiểm tra
     * @param metricType   - Loại metric: "vocab_added", "grammar_check",
     *                     "learning_streak"
     * @param currentValue - Giá trị hiện tại của user (số từ vựng, số lần kiểm tra
     *                     ngữ pháp, số ngày streak)
     */
    public List<Achievement> checkAndGrantAchievements(User user, String metricType, Long currentValue) {
        // Lấy tất cả achievement có metricType phù hợp
        List<Achievement> achievements = achievementRepository.findAll();
        List<Achievement> newAchievements = new java.util.ArrayList<>();

        for (Achievement achievement : achievements) {
            // Kiểm tra metricType có khớp không
            if (!metricType.equals(achievement.getMetricType())) {
                continue;
            }

            // Kiểm tra user đã đạt đủ điều kiện chưa
            if (currentValue < achievement.getRequiredValue()) {
                continue;
            }

            // Kiểm tra user đã có achievement này chưa
            boolean alreadyHas = userAchievementRepository.existsByUserIdAndAchievementId(
                    user.getId(),
                    achievement.getId());

            if (alreadyHas) {
                continue;
            }

            // Tạo và lưu UserAchievement mới
            UserAchievement userAchievement = new UserAchievement();
            userAchievement.setUser(user);
            userAchievement.setAchievement(achievement);
            userAchievement.setAchievedAt(Instant.now());

            userAchievementRepository.save(userAchievement);
            newAchievements.add(achievement);

            System.out
                    .println("🏆 Achievement unlocked: " + user.getName() + " earned '" + achievement.getTitle() + "'");
        }

        return newAchievements;
    }

    /**
     * Lấy danh sách achievement của user
     */
    public List<UserAchievement> getUserAchievements(Long userId) {
        return userAchievementRepository.findByUserId(userId);
    }

    /**
     * Lấy tất cả achievement trong hệ thống
     */
    public List<Achievement> getAllAchievements() {
        return achievementRepository.findAll();
    }

    /**
     * Reset tất cả achievements của user (dùng cho testing)
     */
    @org.springframework.transaction.annotation.Transactional
    public void resetUserAchievements(Long userId) {
        userAchievementRepository.deleteByUserId(userId);
        System.out.println("🔄 Reset achievements for user ID: " + userId);
    }
}
