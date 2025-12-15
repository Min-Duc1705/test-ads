package vn.project.magic_english.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import vn.project.magic_english.model.UserAchievement;

import java.util.List;

public interface UserAchievementRepository
        extends JpaRepository<UserAchievement, Long>, JpaSpecificationExecutor<UserAchievement> {
    boolean existsByUserIdAndAchievementId(Long userId, Long achievementId);

    List<UserAchievement> findByUserId(Long userId);

    void deleteByUserId(Long userId);
}
