package demo.shop.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByCustomerIdOrderByPlacedAtDesc(Long customerId);

    @Query("select o from ShopOrder o where o.status = :status and o.placedAt < :before")
    List<Order> findPlacedBefore(@Param("status") OrderStatus status, @Param("before") Instant before);
}
