package demo.shop;

import demo.shop.config.ShopProperties;
import demo.shop.inventory.StockRepository;
import demo.shop.order.OrderNotifications;
import demo.shop.product.Product;
import demo.shop.product.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ShopApplicationTest {
    @Autowired
    MockMvc mvc;
    @Autowired
    ProductRepository products;
    @Autowired
    StockRepository stock;
    @Autowired
    OrderNotifications notifications;
    @Autowired
    ShopProperties properties;

    @Test
    void seedsCatalogAndQueriesIt() {
        assertThat(products.findExpensive(new BigDecimal("100")))
            .extracting(Product::getName)
            .containsExactlyInAnyOrder("Laptop", "Headphones");
    }

    @Test
    void bindsShopProperties() {
        assertThat(properties.getMaxLinesPerOrder()).isEqualTo(20);
        assertThat(properties.getOrderExpiry()).isEqualTo(Duration.ofDays(2));
    }

    @Test
    void placesOrderEndToEnd() throws Exception {
        long customerId = registerCustomer();
        long coffee = productId("Coffee");
        int availableBefore = stock.findById(coffee).orElseThrow().getAvailable();
        int notifiedBefore = notifications.sentCount();

        String order = "{\"customerId\":" + customerId + ",\"lines\":[{\"productId\":" + coffee + ",\"quantity\":2}]}";
        mvc.perform(post("/api/orders").with(httpBasic("user", "password"))
                .contentType(MediaType.APPLICATION_JSON).content(order))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.itemsTotal").value(17.82))
            .andExpect(jsonPath("$.delivery").value(4.99))
            .andExpect(jsonPath("$.total").value(22.81));

        assertThat(stock.findById(coffee).orElseThrow().getAvailable()).isEqualTo(availableBefore - 2);
        assertThat(notifications.sentCount()).isEqualTo(notifiedBefore + 1);
    }

    @Test
    void refusesOrderBeyondStock() throws Exception {
        long customerId = registerCustomer();
        long laptop = productId("Laptop");
        int available = stock.findById(laptop).orElseThrow().getAvailable();

        String order = "{\"customerId\":" + customerId + ",\"lines\":[{\"productId\":" + laptop
            + ",\"quantity\":" + (available + 1) + "}]}";
        mvc.perform(post("/api/orders").with(httpBasic("user", "password"))
                .contentType(MediaType.APPLICATION_JSON).content(order))
            .andExpect(status().isConflict());

        assertThat(stock.findById(laptop).orElseThrow().getAvailable()).isEqualTo(available);
    }

    @Test
    void healthIsPublicAndShowsDeliverySettings() throws Exception {
        mvc.perform(get("/actuator/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.components.delivery.details.timeoutMs").value(1500));
    }

    private long registerCustomer() throws Exception {
        String email = UUID.randomUUID() + "@example.org";
        String response = mvc.perform(post("/api/customers").with(httpBasic("user", "password"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Test\",\"email\":\"" + email + "\"}"))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return Long.parseLong(response.replaceAll(".*\"id\":(\\d+).*", "$1"));
    }

    private long productId(String name) {
        return products.findAll().stream()
            .filter(product -> product.getName().equals(name))
            .findFirst()
            .orElseThrow()
            .getId();
    }
}
