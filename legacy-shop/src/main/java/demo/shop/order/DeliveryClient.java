package demo.shop.order;

import com.corp.commons.util.CorpConfig;
import com.corp.commons.util.CorpContext;
import com.corp.commons.util.CorpHttpClient;
import com.corp.commons.util.CorpLogger;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;

/**
 * Asks the delivery service for a shipping quote. Falls back to a flat rate
 * when the service does not answer in time.
 */
@Component
public class DeliveryClient {
    static final BigDecimal FLAT_RATE = new BigDecimal("4.99");

    private static final CorpLogger log = CorpLogger.getLogger(DeliveryClient.class);

    private final CorpHttpClient http;

    public DeliveryClient() {
        CorpConfig config = CorpContext.current().getConfig();
        this.http = new CorpHttpClient(
            config.getString("shop.delivery.base-url", "http://localhost:8081"),
            config.getLong("shop.delivery.timeout-ms", 2000));
    }

    public long timeoutMillis() {
        return http.getTimeoutMillis();
    }

    public BigDecimal quote(long customerId, int items) {
        try {
            return new BigDecimal(http.get("/quotes?customer=" + customerId + "&items=" + items).trim());
        } catch (IOException | NumberFormatException e) {
            log.warn("Delivery quote failed after {} ms, using flat rate: {}", http.getTimeoutMillis(), e.getMessage());
            return FLAT_RATE;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return FLAT_RATE;
        }
    }
}
