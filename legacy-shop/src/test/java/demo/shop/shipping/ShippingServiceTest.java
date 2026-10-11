package demo.shop.shipping;

import demo.shop.inventory.InventoryService;
import demo.shop.order.Order;
import demo.shop.order.OrderService;
import demo.shop.order.OrderShippedEvent;
import demo.shop.order.OrderStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShippingServiceTest {
    private static final Clock NOW = Clock.fixed(Instant.parse("2026-10-11T12:00:00Z"), ZoneOffset.UTC);

    @Mock
    OrderService orders;
    @Mock
    InventoryService inventory;
    @Mock
    ShipmentRepository shipments;
    @Mock
    ApplicationEventPublisher events;

    @Test
    void shipsPaidOrderAndTakesStockOut() {
        Order order = new Order(5L, Instant.parse("2026-10-11T11:00:00Z"));
        order.addLine(7L, "Laptop", 2, new BigDecimal("2698.20"));
        order.markPaid();
        when(orders.getInStatus(42L, OrderStatus.PAID)).thenReturn(order);
        when(shipments.save(any(Shipment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Shipment shipment = new ShippingService(orders, inventory, shipments, events, NOW).ship(42L);

        assertThat(shipment.getTrackingNumber()).isEqualTo("TRK-00000042");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.SHIPPED);
        verify(inventory).commit(7L, 2);
        verify(events).publishEvent(any(OrderShippedEvent.class));
    }

    @Test
    void unpaidOrderIsNotShipped() {
        when(orders.getInStatus(42L, OrderStatus.PAID))
            .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Order 42 is PLACED, expected PAID"));

        assertThatThrownBy(() -> new ShippingService(orders, inventory, shipments, events, NOW).ship(42L))
            .isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(inventory, shipments);
    }
}
