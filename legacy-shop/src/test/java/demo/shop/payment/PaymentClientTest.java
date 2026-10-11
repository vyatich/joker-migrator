package demo.shop.payment;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentClientTest {
    private final PaymentClient client = new PaymentClient();

    @Test
    void fakeModeApprovesUpToTheLimit() {
        assertThat(client.authorize(1L, new BigDecimal("9999.99"))).isTrue();
        assertThat(client.authorize(1L, new BigDecimal("10000.01"))).isFalse();
    }
}
