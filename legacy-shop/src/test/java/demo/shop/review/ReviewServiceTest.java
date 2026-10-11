package demo.shop.review;

import demo.shop.customer.Customer;
import demo.shop.customer.CustomerRepository;
import demo.shop.product.Product;
import demo.shop.product.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

@DataJpaTest
@Import(ReviewService.class)
class ReviewServiceTest {
    @Autowired
    ReviewService reviews;
    @Autowired
    ProductRepository products;
    @Autowired
    CustomerRepository customers;

    long productId;
    long ada;
    long bob;

    @BeforeEach
    void setUp() {
        productId = products.save(new Product("Laptop", new BigDecimal("1499.00"))).getId();
        ada = customers.save(new Customer("Ada", "ada@example.org", Instant.now())).getId();
        bob = customers.save(new Customer("Bob", "bob@example.org", Instant.now())).getId();
    }

    @Test
    void averagesRatingsToOneDecimal() {
        assertThat(reviews.averageRating(productId)).isEmpty();

        reviews.add(productId, ada, 5, "Great");
        reviews.add(productId, bob, 4, null);

        assertThat(reviews.averageRating(productId)).hasValue(new BigDecimal("4.5"));
        assertThat(reviews.findByProduct(productId)).hasSize(2);
    }

    @Test
    void oneReviewPerCustomerAndProduct() {
        reviews.add(productId, ada, 5, "Great");

        ResponseStatusException e = catchThrowableOfType(
            () -> reviews.add(productId, ada, 1, "Changed my mind"), ResponseStatusException.class);
        assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void reviewsOnlyExistingProducts() {
        ResponseStatusException e = catchThrowableOfType(
            () -> reviews.add(999_999L, ada, 5, null), ResponseStatusException.class);
        assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
