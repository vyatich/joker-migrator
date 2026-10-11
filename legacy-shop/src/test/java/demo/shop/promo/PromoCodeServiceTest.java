package demo.shop.promo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PromoCodeServiceTest {
    private static final Clock OCT_11 = Clock.fixed(Instant.parse("2026-10-11T12:00:00Z"), ZoneOffset.UTC);

    @Mock
    PromoCodeRepository repository;

    PromoCodeService codes;

    @BeforeEach
    void setUp() {
        codes = new PromoCodeService(repository, OCT_11);
    }

    @Test
    void storesCodesUpperCase() {
        when(repository.save(any(PromoCode.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PromoCode created = codes.create(" autumn15 ", 15, LocalDate.of(2026, 11, 30));

        assertThat(created.getCode()).isEqualTo("AUTUMN15");
    }

    @Test
    void refusesMalformedCodes() {
        ResponseStatusException e = catchThrowableOfType(
            () -> codes.create("no spaces!", 15, LocalDate.of(2026, 11, 30)), ResponseStatusException.class);
        assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void givesDiscountOnlyWhileValid() {
        when(repository.findById("AUTUMN15")).thenReturn(Optional.of(new PromoCode("AUTUMN15", 15, LocalDate.of(2026, 10, 11))));
        when(repository.findById("SUMMER")).thenReturn(Optional.of(new PromoCode("SUMMER", 20, LocalDate.of(2026, 8, 31))));

        assertThat(codes.discountPercent("autumn15")).isEqualTo(15);
        ResponseStatusException e = catchThrowableOfType(() -> codes.discountPercent("summer"), ResponseStatusException.class);
        assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
