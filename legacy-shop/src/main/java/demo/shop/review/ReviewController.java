package demo.shop.review;

import demo.shop.api.ReviewsApi;
import demo.shop.api.model.NewReviewDto;
import demo.shop.api.model.ReviewDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class ReviewController implements ReviewsApi {
    private final ReviewService reviews;

    @Override
    public ResponseEntity<List<ReviewDto>> listReviews(Long productId) {
        return ResponseEntity.ok(reviews.findByProduct(productId).stream()
            .map(ReviewController::toDto)
            .collect(Collectors.toList()));
    }

    @Override
    public ResponseEntity<ReviewDto> addReview(Long productId, NewReviewDto newReview) {
        Review added = reviews.add(productId, newReview.getCustomerId(), newReview.getRating(), newReview.getText());
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(added));
    }

    static ReviewDto toDto(Review review) {
        return new ReviewDto()
            .id(review.getId())
            .productId(review.getProductId())
            .customerId(review.getCustomerId())
            .rating(review.getRating())
            .text(review.getText())
            .createdAt(review.getCreatedAt().atOffset(ZoneOffset.UTC));
    }
}
