package demo.shop.product;

import com.fasterxml.jackson.databind.ObjectMapper;
import demo.shop.api.model.NewProductDto;
import demo.shop.config.SecurityConfig;
import demo.shop.review.ReviewService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@Import(SecurityConfig.class)
class ProductControllerTest {
    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper mapper;
    @MockBean
    ProductService products;
    @MockBean
    ReviewService reviews;

    static Product product(long id, String name, String price) {
        Product product = new Product(name, new BigDecimal(price));
        ReflectionTestUtils.setField(product, "id", id);
        return product;
    }

    @Test
    @WithMockUser
    void listsProducts() throws Exception {
        when(products.findAll()).thenReturn(List.of(product(1, "Coffee", "9.90")));
        mvc.perform(get("/api/products"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].name").value("Coffee"));
    }

    @Test
    @WithMockUser
    void answersNotFoundWithReasonPhrase() throws Exception {
        when(products.get(42L)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));
        mvc.perform(get("/api/products/42"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value("Not Found"));
    }

    @Test
    @WithMockUser
    void createsProduct() throws Exception {
        when(products.create(eq("Tea"), any(), isNull())).thenReturn(product(3, "Tea", "4.50"));
        String body = mapper.writeValueAsString(new NewProductDto().name("Tea").price(new BigDecimal("4.50")));
        mvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(3))
            .andExpect(jsonPath("$.name").value("Tea"));
    }

    @Test
    @WithMockUser
    void rejectsNonPositivePrice() throws Exception {
        mvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Tea\",\"price\":-1}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(containsString("price")));
    }

    @Test
    void rejectsAnonymous() throws Exception {
        mvc.perform(get("/api/products")).andExpect(status().isUnauthorized());
    }
}
