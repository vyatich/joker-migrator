package demo.shop.history;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistoryRepository extends JpaRepository<HistoryEntry, Long> {
    List<HistoryEntry> findByOrderIdOrderByAtAscIdAsc(Long orderId);
}
