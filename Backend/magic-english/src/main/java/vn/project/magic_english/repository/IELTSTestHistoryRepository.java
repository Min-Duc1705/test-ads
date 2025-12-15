package vn.project.magic_english.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.project.magic_english.model.IELTSTestHistory;

import java.util.List;

@Repository
public interface IELTSTestHistoryRepository extends JpaRepository<IELTSTestHistory, Long> {
    List<IELTSTestHistory> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<IELTSTestHistory> findByUserIdAndStatus(Long userId, String status);
}
