package vn.project.magic_english.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.project.magic_english.model.TOEICTestHistory;
import vn.project.magic_english.model.User;

import java.util.List;

@Repository
public interface TOEICTestHistoryRepository extends JpaRepository<TOEICTestHistory, Long> {
    List<TOEICTestHistory> findByUserOrderByCreatedAtDesc(User user);
}
