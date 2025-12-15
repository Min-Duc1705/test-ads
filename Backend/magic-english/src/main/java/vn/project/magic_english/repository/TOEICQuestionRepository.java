package vn.project.magic_english.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.project.magic_english.model.TOEICQuestion;

@Repository
public interface TOEICQuestionRepository extends JpaRepository<TOEICQuestion, Long> {
}
