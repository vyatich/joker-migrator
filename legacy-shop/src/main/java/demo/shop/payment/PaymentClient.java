package demo.shop.payment;

import com.corp.commons.core.CorpConfig;
import com.corp.commons.core.CorpContext;
import com.corp.commons.core.CorpHttpClient;
import com.corp.commons.core.CorpLog;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.math.BigDecimal;

/**
 * Asks the payment service to authorize an amount. In fake mode (demo and tests) every amount up to
 * a limit is approved without a network call.
 */
@Component
public class PaymentClient {
    private static final CorpLog log = CorpLog.getLogger(PaymentClient.class);

    private final CorpHttpClient http;
    private final boolean fake;
    private final BigDecimal fakeLimit;

    public PaymentClient() {
        CorpConfig config = CorpContext.current().config();
        this.http = new CorpHttpClient(
            config.getString("shop.payment.base-url", "http://localhost:8082"),
            Duration.ofMillis(config.getLong("shop.payment.timeout-ms", 3000)));
        this.fake = Boolean.parseBoolean(config.getString("shop.payment.fake", "false"));
        this.fakeLimit = new BigDecimal(config.getString("shop.payment.fake-limit", "10000"));
    }

    public boolean authorize(long orderId, BigDecimal amount) {
        if (fake) {
            boolean approved = amount.compareTo(fakeLimit) <= 0;
            log.info("Fake payment for order {}: {} {}", orderId, amount, approved ? "approved" : "declined");
            return approved;
        }
        try {
            String answer = http.get("/authorize?order=" + orderId + "&amount=" + amount.toPlainString());
            return "APPROVED".equals(answer.trim());
        } catch (IOException e) {
            log.warn("Payment service did not answer within {} ms: {}", http.getTimeout().toMillis(), e.getMessage());
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
