package demo.shop.history;

import com.corp.commons.core.CorpLog;
import demo.shop.order.OrderPaidEvent;
import demo.shop.order.OrderPlacedEvent;
import demo.shop.order.OrderShippedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Keeps a trail of what happened to each order. Entries are written in the same transaction as
 * the change itself, so the trail never disagrees with the order.
 */
@Service
@RequiredArgsConstructor
public class OrderHistoryService {
    private static final CorpLog log = CorpLog.getLogger(OrderHistoryService.class);

    private final HistoryRepository history;

    @Transactional(readOnly = true)
    public List<HistoryEntry> forOrder(long orderId) {
        return history.findByOrderIdOrderByAtAscIdAsc(orderId);
    }

    @Transactional
    public void record(long orderId, String action, String details) {
        history.save(new HistoryEntry(orderId, action, details, Instant.now()));
        log.info("Order {}: {} {}", orderId, action, details == null ? "" : details);
    }

    @EventListener
    public void onPlaced(OrderPlacedEvent event) {
        record(event.getOrderId(), "PLACED", "total " + event.getTotal());
    }

    @EventListener
    public void onPaid(OrderPaidEvent event) {
        record(event.getOrderId(), "PAID", "amount " + event.getAmount());
    }

    @EventListener
    public void onShipped(OrderShippedEvent event) {
        record(event.getOrderId(), "SHIPPED", "tracking " + event.getTrackingNumber());
    }
}
