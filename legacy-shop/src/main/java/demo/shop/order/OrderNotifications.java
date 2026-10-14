package demo.shop.order;

import com.corp.commons.core.CorpLog;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tells the customer about a placed order once it is safely committed.
 */
@Component
public class OrderNotifications {
    private static final CorpLog log = CorpLog.getLogger(OrderNotifications.class);

    private final NotificationSender sender;
    private final AtomicInteger sent = new AtomicInteger();

    public OrderNotifications(NotificationSender sender) {
        this.sender = sender;
    }

    @TransactionalEventListener
    public void onOrderPlaced(OrderPlacedEvent event) {
        String text = "order " + event.getOrderId() + " placed, total " + event.getTotal();
        sender.send(event.getCustomerId(), text).addCallback(
            delivered -> log.info("Notification for order {} delivered: {}", event.getOrderId(), delivered),
            failure -> log.error("Notification for order " + event.getOrderId() + " failed", failure));
        sent.incrementAndGet();
    }

    public int sentCount() {
        return sent.get();
    }
}
