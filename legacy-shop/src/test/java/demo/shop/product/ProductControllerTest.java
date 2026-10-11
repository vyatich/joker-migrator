package demo.shop.product;

import com.fasterxml.jackson.databind.ObjectMapper;
import demo.shop.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
    ProductRepository repository;

    @Test
    @WithMockUser
    void listsProducts() throws Exception {
        when(repository.findAll()).thenReturn(List.of(new Product("Coffee", new BigDecimal("9.90"))));
        mvc.perform(get("/api/products"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name").value("Coffee"));
    }

    @Test
    @WithMockUser
    void returns404WithReasonPhraseForMissingProduct() throws Exception {
        when(repository.findById(42L)).thenReturn(Optional.empty());
        mvc.perform(get("/api/products/42"))
            .andExpect(status().isNotFound())
            .andExpect(content().string("Not Found"));
    }

    @Test
    @WithMockUser
    void createsProduct() throws Exception {
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        String body = mapper.writeValueAsString(new Product("Tea", new BigDecimal("4.50")));
        mvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Tea"));
    }

    @Test
    void rejectsAnonymous() throws Exception {
        mvc.perform(get("/api/products")).andExpect(status().isUnauthorized());
    }
}
