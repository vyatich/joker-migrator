package demo.shop.review;

import com.corp.commons.util.CorpLogger;
import demo.shop.customer.CustomerRepository;
import demo.shop.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {
    private static final CorpLogger log = CorpLogger.getLogger(ReviewService.class);

    private final ReviewRepository reviews;
    private final ProductRepository products;
    private final CustomerRepository customers;

    public List<Review> findByProduct(long productId) {
        requireProduct(productId);
        return reviews.findByProductIdOrderByCreatedAtDesc(productId);
    }

    /**
     * Average rating rounded to one decimal, or empty when the product has no reviews yet.
     */
    public Optional<BigDecimal> averageRating(long productId) {
        Double average = reviews.averageRating(productId);
        return Optional.ofNullable(average).map(value -> BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP));
    }

    @Transactional
    public Review add(long productId, long customerId, int rating, String text) {
        requireProduct(productId);
        if (!customers.existsById(customerId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer " + customerId + " not found");
        }
        if (reviews.existsByProductIdAndCustomerId(productId, customerId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Customer " + customerId + " has already reviewed product " + productId);
        }
        Review review = reviews.save(new Review(productId, customerId, rating, text, Instant.now()));
        log.info("Review {} for product {}: {} stars", review.getId(), productId, rating);
        return review;
    }

    private void requireProduct(long productId) {
        if (!products.existsById(productId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product " + productId + " not found");
        }
    }
}
