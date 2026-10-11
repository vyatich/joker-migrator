package demo.shop.promo;

import demo.shop.api.PromoCodesApi;
import demo.shop.api.model.PromoCodeDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PromoCodeController implements PromoCodesApi {
    private final PromoCodeService codes;

    @Override
    public ResponseEntity<PromoCodeDto> createPromoCode(PromoCodeDto request) {
        PromoCode created = codes.create(request.getCode(), request.getPercent(), request.getValidUntil());
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(created));
    }

    @Override
    public ResponseEntity<PromoCodeDto> getPromoCode(String code) {
        return ResponseEntity.ok(toDto(codes.get(code)));
    }

    private static PromoCodeDto toDto(PromoCode code) {
        return new PromoCodeDto().code(code.getCode()).percent(code.getPercent()).validUntil(code.getValidUntil());
    }
}
