package demo.shop.order;

import com.corp.commons.core.CorpLog;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Sends customer notifications off the request thread. In the demo a notification is a log line.
 */
@Component
public class NotificationSender {
    private static final CorpLog log = CorpLog.getLogger(NotificationSender.class);

    @Async
    public CompletableFuture<Boolean> send(long customerId, String text) {
        log.info("Notify customer {}: {}", customerId, text);
        return AsyncResult.forValue(Boolean.TRUE);
    }
}
