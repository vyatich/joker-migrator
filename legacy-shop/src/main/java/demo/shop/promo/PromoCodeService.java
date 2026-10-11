package demo.shop.promo;

import com.corp.commons.util.CorpLogger;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PromoCodeService {
    private static final CorpLogger log = CorpLogger.getLogger(PromoCodeService.class);

    private final PromoCodeRepository codes;
    private final Clock clock;

    public PromoCode get(String code) {
        return codes.findById(normalize(code))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Promo code " + code + " not found"));
    }

    @Transactional
    public PromoCode create(String code, int percent, LocalDate validUntil) {
        String normalized = normalize(code);
        if (!PromoCodeFormatValidator.isWellFormed(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Promo code " + code + " must be 4 to 20 letters or digits");
        }
        if (validUntil.isBefore(LocalDate.now(clock))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Promo code " + code + " would already be expired");
        }
        PromoCode created = codes.save(new PromoCode(normalized, percent, validUntil));
        log.info("Promo code {} created: {}% until {}", normalized, percent, validUntil);
        return created;
    }

    /**
     * Discount of a code that is valid today.
     *
     * @throws ResponseStatusException 400 if the code does not exist or has expired
     */
    public int discountPercent(String code) {
        return codes.findById(normalize(code))
            .filter(promo -> promo.isValidOn(LocalDate.now(clock)))
            .map(PromoCode::getPercent)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Promo code " + code + " is not valid"));
    }

    private static String normalize(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }
}
