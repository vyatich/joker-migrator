package demo.shop.review;

import demo.shop.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReviewController.class)
@Import(SecurityConfig.class)
@WithMockUser
class ReviewControllerTest {
    @Autowired
    MockMvc mvc;
    @MockitoBean
    ReviewService reviews;

    @Test
    void addsReview() throws Exception {
        Review review = new Review(7L, 5L, 4, "Solid", Instant.parse("2026-10-01T10:00:00Z"));
        ReflectionTestUtils.setField(review, "id", 1L);
        when(reviews.add(7L, 5L, 4, "Solid")).thenReturn(review);

        mvc.perform(post("/api/products/7/reviews").contentType(MediaType.APPLICATION_JSON)
                .content("{\"customerId\":5,\"rating\":4,\"text\":\"Solid\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.rating").value(4))
            .andExpect(jsonPath("$.createdAt").value("2026-10-01T10:00:00Z"));
    }

    @Test
    void rejectsRatingAboveFive() throws Exception {
        mvc.perform(post("/api/products/7/reviews").contentType(MediaType.APPLICATION_JSON)
                .content("{\"customerId\":5,\"rating\":6}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(containsString("rating")));
        verifyNoInteractions(reviews);
    }
}
