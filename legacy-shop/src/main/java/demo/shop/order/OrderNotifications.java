package demo.shop.order;

import com.corp.commons.util.CorpLogger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tells the customer about a placed order once it is safely committed. In the demo the
 * notification is a log line.
 */
@Component
public class OrderNotifications {
    private static final CorpLogger log = CorpLogger.getLogger(OrderNotifications.class);

    private final AtomicInteger sent = new AtomicInteger();

    @TransactionalEventListener
    public void onOrderPlaced(OrderPlacedEvent event) {
        log.info("Notify customer {}: order {} placed, total {}", event.getCustomerId(), event.getOrderId(), event.getTotal());
        sent.incrementAndGet();
    }

    public int sentCount() {
        return sent.get();
    }
}
