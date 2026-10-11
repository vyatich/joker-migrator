package demo.shop.order;

import com.corp.commons.util.CorpLogger;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;

/**
 * Sends customer notifications off the request thread. In the demo a notification is a log line.
 */
@Component
public class NotificationSender {
    private static final CorpLogger log = CorpLogger.getLogger(NotificationSender.class);

    @Async
    public ListenableFuture<Boolean> send(long customerId, String text) {
        log.info("Notify customer {}: {}", customerId, text);
        return AsyncResult.forValue(Boolean.TRUE);
    }
}
