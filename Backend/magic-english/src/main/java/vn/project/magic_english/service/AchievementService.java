package vn.project.magic_english.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.project.magic_english.model.Achievement;
import vn.project.magic_english.repository.AchievementRepository;

import java.util.List;
import java.util.Optional;

@Service
public class AchievementService {
    

    @Autowired
    private AchievementRepository achievementRepository;

    public List<Achievement> getAllAchievements() {
        return achievementRepository.findAll();
    }

    public Optional<Achievement> getAchievementById(Long id) {
        return achievementRepository.findById(id);
    }

    public Achievement createAchievement(Achievement achievement) {
        return achievementRepository.save(achievement);
    }

    public Achievement updateAchievement(Long id, Achievement achievementDetails) {
        Achievement achievement = achievementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Achievement not found"));
        achievement.setTitle(achievementDetails.getTitle());
        achievement.setDescription(achievementDetails.getDescription());
        achievement.setIconUrl(achievementDetails.getIconUrl());
        achievement.setRequiredValue(achievementDetails.getRequiredValue());
        achievement.setMetricType(achievementDetails.getMetricType());
        // createdAt giữ nguyên, updatedAt sẽ tự động cập nhật nếu có @PreUpdate
        return achievementRepository.save(achievement);
    }

    public Achievement updateAchievementIcon(Long achievementId, String iconUrl) {
        Achievement achievement = achievementRepository.findById(achievementId)
                .orElseThrow(() -> new RuntimeException("Achievement not found"));
        achievement.setIconUrl(iconUrl);
        return achievementRepository.save(achievement);
    }

    public void deleteAchievement(Long id) {
        achievementRepository.deleteById(id);
    }
}
