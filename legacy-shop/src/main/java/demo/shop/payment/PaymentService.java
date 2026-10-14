package demo.shop.payment;

import com.corp.commons.core.CorpLog;
import demo.shop.history.OrderHistoryService;
import demo.shop.order.Order;
import demo.shop.order.OrderPaidEvent;
import demo.shop.order.OrderService;
import demo.shop.order.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentService {
    private static final CorpLog log = CorpLog.getLogger(PaymentService.class);

    private final OrderService orders;
    private final PaymentRepository payments;
    private final PaymentClient client;
    private final OrderHistoryService history;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    /**
     * Charges the order total. A declined payment is recorded and leaves the order placed, so the
     * customer can try again.
     */
    public Payment pay(long orderId) {
        Order order = orders.getInStatus(orderId, OrderStatus.PLACED);
        BigDecimal amount = order.getTotal();
        boolean approved = client.authorize(orderId, amount);
        Payment payment = payments.save(new Payment(orderId, amount,
            approved ? PaymentStatus.APPROVED : PaymentStatus.DECLINED, Instant.now(clock)));
        if (approved) {
            order.markPaid();
            events.publishEvent(new OrderPaidEvent(orderId, amount));
        } else {
            history.record(orderId, "PAYMENT_DECLINED", "amount " + amount);
            log.warn("Payment for order {} declined", orderId);
        }
        return payment;
    }
}
