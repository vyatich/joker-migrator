package demo.shop.order;

import demo.shop.config.ShopProperties;
import demo.shop.customer.CustomerService;
import demo.shop.inventory.InventoryService;
import demo.shop.product.Product;
import demo.shop.product.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    @Mock
    OrderRepository orders;
    @Mock
    CustomerService customers;
    @Mock
    ProductService products;
    @Mock
    InventoryService inventory;
    @Mock
    PricingService pricing;
    @Mock
    DeliveryClient delivery;
    @Mock
    ApplicationEventPublisher events;

    OrderService service;

    @BeforeEach
    void setUp() {
        ShopProperties properties = new ShopProperties(2, Duration.ofDays(2));
        service = new OrderService(orders, customers, products, inventory, pricing, delivery, events, properties);
    }

    @Test
    void placesOrderReservesStockAndPublishesEvent() {
        Product laptop = new Product("Laptop", new BigDecimal("1499.00"));
        ReflectionTestUtils.setField(laptop, "id", 7L);
        when(products.get(7L)).thenReturn(laptop);
        when(pricing.total(new BigDecimal("1499.00"), 1)).thenReturn(new BigDecimal("1349.10"));
        when(delivery.quote(5L, 1)).thenReturn(new BigDecimal("4.99"));
        when(orders.save(any(Order.class))).thenAnswer(invocation -> {
            Order saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 100L);
            return saved;
        });

        Order order = service.place(5L, List.of(new OrderItem(7L, 1)));

        assertThat(order.getTotal()).isEqualByComparingTo("1354.09");
        assertThat(order.getLines()).extracting(OrderLine::getProductName).containsExactly("Laptop");
        verify(inventory).reserve(7L, 1);
        verify(events).publishEvent(any(OrderPlacedEvent.class));
    }

    @Test
    void rejectsTooManyLines() {
        List<OrderItem> items = List.of(new OrderItem(1L, 1), new OrderItem(2L, 1), new OrderItem(3L, 1));

        assertThatThrownBy(() -> service.place(5L, items))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("at most 2 lines");
        verifyNoInteractions(inventory, orders);
    }

    @Test
    void rejectsUnknownCustomer() {
        when(customers.get(9L)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer 9 not found"));

        assertThatThrownBy(() -> service.place(9L, List.of(new OrderItem(7L, 1))))
            .isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(inventory, orders);
    }

    @Test
    void expiresStaleOrdersAndReleasesTheirStock() {
        Order stale = new Order(5L, Instant.parse("2026-01-01T00:00:00Z"));
        stale.addLine(7L, "Laptop", 2, new BigDecimal("2698.20"));
        when(orders.findPlacedBefore(eq(OrderStatus.PLACED), any())).thenReturn(List.of(stale));

        int expired = service.expireStale(Instant.parse("2026-01-05T00:00:00Z"));

        assertThat(expired).isEqualTo(1);
        assertThat(stale.getStatus()).isEqualTo(OrderStatus.EXPIRED);
        verify(inventory).release(7L, 2);
    }
}
