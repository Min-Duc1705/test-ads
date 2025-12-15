package vn.project.magic_english.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.project.magic_english.model.IELTSTest;

import java.util.List;
import java.util.Optional;

@Repository
public interface IELTSTestRepository extends JpaRepository<IELTSTest, Long> {
    List<IELTSTest> findBySkillAndLevel(String skill, String level);

    List<IELTSTest> findBySkillAndLevelAndDifficulty(String skill, String level, String difficulty);

    Optional<IELTSTest> findFirstBySkillAndLevelAndDifficulty(String skill, String level, String difficulty);
}
