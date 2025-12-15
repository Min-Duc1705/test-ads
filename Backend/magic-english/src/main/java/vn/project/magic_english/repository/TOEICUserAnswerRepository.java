package vn.project.magic_english.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.project.magic_english.model.TOEICUserAnswer;

@Repository
public interface TOEICUserAnswerRepository extends JpaRepository<TOEICUserAnswer, Long> {
}
