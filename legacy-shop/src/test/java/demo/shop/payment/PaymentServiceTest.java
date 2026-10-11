package demo.shop.payment;

import demo.shop.history.OrderHistoryService;
import demo.shop.order.Order;
import demo.shop.order.OrderPaidEvent;
import demo.shop.order.OrderService;
import demo.shop.order.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {
    @Mock
    OrderService orders;
    @Mock
    PaymentRepository payments;
    @Mock
    PaymentClient client;
    @Mock
    OrderHistoryService history;
    @Mock
    ApplicationEventPublisher events;

    PaymentService service;
    Order order;

    @BeforeEach
    void setUp() {
        service = new PaymentService(orders, payments, client, history, events,
            Clock.fixed(Instant.parse("2026-10-11T12:00:00Z"), ZoneOffset.UTC));
        order = new Order(5L, Instant.parse("2026-10-11T11:00:00Z"));
        order.addLine(1L, "Coffee", 2, new BigDecimal("17.82"));
        order.setDelivery(new BigDecimal("4.99"));
        when(orders.getInStatus(42L, OrderStatus.PLACED)).thenReturn(order);
        when(payments.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void approvedPaymentMarksOrderPaid() {
        when(client.authorize(42L, new BigDecimal("22.81"))).thenReturn(true);

        Payment payment = service.pay(42L);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        verify(events).publishEvent(any(OrderPaidEvent.class));
    }

    @Test
    void declinedPaymentKeepsOrderPlacedAndIsRecorded() {
        when(client.authorize(42L, new BigDecimal("22.81"))).thenReturn(false);

        Payment payment = service.pay(42L);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.DECLINED);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PLACED);
        verify(history).record(eq(42L), eq("PAYMENT_DECLINED"), anyString());
        verify(events, never()).publishEvent(any(OrderPaidEvent.class));
    }
}
