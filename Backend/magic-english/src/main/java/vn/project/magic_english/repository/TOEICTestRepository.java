package vn.project.magic_english.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.project.magic_english.model.TOEICTest;

import java.util.Optional;

@Repository
public interface TOEICTestRepository extends JpaRepository<TOEICTest, Long> {
    Optional<TOEICTest> findBySectionAndDifficulty(String section, String difficulty);
}
