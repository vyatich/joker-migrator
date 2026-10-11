package demo.shop.order;

import demo.shop.config.SecurityConfig;
import demo.shop.inventory.InsufficientStockException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@Import(SecurityConfig.class)
@WithMockUser
class OrderControllerTest {
    private static final String ONE_LAPTOP = "{\"customerId\":5,\"lines\":[{\"productId\":7,\"quantity\":1}]}";

    @Autowired
    MockMvc mvc;
    @MockBean
    OrderService orders;

    @Test
    void placesOrder() throws Exception {
        Order order = new Order(5L, Instant.parse("2026-10-01T10:15:30Z"));
        order.addLine(7L, "Laptop", 1, new BigDecimal("1349.10"));
        order.setDelivery(new BigDecimal("4.99"));
        ReflectionTestUtils.setField(order, "id", 100L);
        when(orders.place(eq(5L), anyList())).thenReturn(order);

        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(ONE_LAPTOP))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(100))
            .andExpect(jsonPath("$.status").value("PLACED"))
            .andExpect(jsonPath("$.lines[0].productName").value("Laptop"))
            .andExpect(jsonPath("$.total").value(1354.09))
            .andExpect(jsonPath("$.placedAt").value("2026-10-01T10:15:30Z"));
    }

    @Test
    void answersConflictWhenStockIsShort() throws Exception {
        when(orders.place(eq(5L), anyList())).thenThrow(new InsufficientStockException(7L, 1, 0));

        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(ONE_LAPTOP))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.message").value(containsString("requested 1, available 0")));
    }

    @Test
    void rejectsOrderWithoutLines() throws Exception {
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content("{\"customerId\":5,\"lines\":[]}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(containsString("lines")));
    }
}
