package demo.shop.promo;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PromoCodeFormatValidatorTest {
    private final PromoCodeFormatValidator validator = new PromoCodeFormatValidator();

    @Test
    void acceptsUpperCaseLettersAndDigits() {
        assertThat(validator.isValid("WELCOME10", null)).isTrue();
        assertThat(validator.isValid(null, null)).isTrue();
    }

    @Test
    void rejectsLowerCaseShortAndPunctuation() {
        assertThat(validator.isValid("welcome10", null)).isFalse();
        assertThat(validator.isValid("ABC", null)).isFalse();
        assertThat(validator.isValid("SALE-10", null)).isFalse();
    }
}
