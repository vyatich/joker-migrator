package demo.shop.review;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByProductIdOrderByCreatedAtDesc(Long productId);

    boolean existsByProductIdAndCustomerId(Long productId, Long customerId);

    @Query("select avg(r.rating) from Review r where r.productId = :productId")
    Double averageRating(@Param("productId") Long productId);
}
