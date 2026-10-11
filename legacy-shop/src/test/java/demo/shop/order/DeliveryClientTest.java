package demo.shop.order;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DeliveryClientTest {
    @Test
    void takesTimeoutFromCorpConfig() {
        assertThat(new DeliveryClient().timeoutMillis()).isEqualTo(1500);
    }

    @Test
    void fallsBackToFlatRateWhenDeliveryServiceIsDown() {
        assertThat(new DeliveryClient().quote(1L, 2)).isEqualByComparingTo(DeliveryClient.FLAT_RATE);
    }
}
