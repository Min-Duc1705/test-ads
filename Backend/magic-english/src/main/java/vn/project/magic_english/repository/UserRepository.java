package vn.project.magic_english.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import vn.project.magic_english.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    User findByEmail(String email);

    boolean existsByEmail(String email);

    User findByRefreshTokenAndEmail(String token, String email);

    // Get all unique dates when user added vocabulary (for streak calculation)
    @Query("SELECT DISTINCT DATE(v.createdAt) FROM Vocabulary v WHERE v.user.id = :userId ORDER BY DATE(v.createdAt) DESC")
    List<java.sql.Date> findAllVocabularyDatesByUserId(@Param("userId") Long userId);

    // Get all unique dates when user checked grammar (for streak calculation)
    @Query("SELECT DISTINCT DATE(g.createdAt) FROM Grammar g WHERE g.user.id = :userId ORDER BY DATE(g.createdAt) DESC")
    List<java.sql.Date> findAllGrammarDatesByUserId(@Param("userId") Long userId);
}
