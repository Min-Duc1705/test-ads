package vn.project.magic_english.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import vn.project.magic_english.model.Vocabulary;

import java.util.List;

@Repository
public interface VocabularyRepository extends JpaRepository<Vocabulary, Long>, JpaSpecificationExecutor<Vocabulary> {

    @Query("SELECT v.wordType, COUNT(v) FROM Vocabulary v WHERE v.user.id = :userId GROUP BY v.wordType")
    List<Object[]> countByWordTypeForUser(@Param("userId") Long userId);

    @Query("SELECT v.cefrLevel, COUNT(v) FROM Vocabulary v WHERE v.user.id = :userId GROUP BY v.cefrLevel")
    List<Object[]> countByCefrLevelForUser(@Param("userId") Long userId);

    @Query("SELECT COUNT(v) FROM Vocabulary v WHERE v.user.id = :userId")
    Long countByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(v) FROM Vocabulary v WHERE v.user.id = :userId AND DATE(v.createdAt) = CURRENT_DATE")
    Long countTodayVocabularyByUserId(@Param("userId") Long userId);
}
