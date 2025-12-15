package vn.project.magic_english.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import vn.project.magic_english.model.Grammar;

@Repository
public interface GrammarRepository extends JpaRepository<Grammar, Long>, JpaSpecificationExecutor<Grammar> {

    // Find grammar checks by user ID with pagination
    Page<Grammar> findByUserId(Long userId, Pageable pageable);

    // Count grammar checks by user
    long countByUserId(Long userId);

    // Count grammar checks created today
    @Query("SELECT COUNT(g) FROM Grammar g WHERE g.user.id = :userId AND DATE(g.createdAt) = CURRENT_DATE")
    Long countTodayGrammarChecksByUserId(@Param("userId") Long userId);

    // Calculate average score for user (today only)
    @Query("SELECT AVG(g.score) FROM Grammar g WHERE g.user.id = :userId AND DATE(g.createdAt) = CURRENT_DATE")
    Double getTodayAverageScoreByUserId(@Param("userId") Long userId);

    // Calculate average score for user (all time)
    @Query("SELECT AVG(g.score) FROM Grammar g WHERE g.user.id = :userId")
    Double getAverageScoreByUserId(@Param("userId") Long userId);
}
