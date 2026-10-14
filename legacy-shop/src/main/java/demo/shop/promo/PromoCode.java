package demo.shop.promo;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;

@Entity
public class PromoCode {
    @Id
    @PromoCodeFormat
    private String code;
    @Min(1)
    @Max(50)
    private int percent;
    private LocalDate validUntil;

    protected PromoCode() {
    }

    public PromoCode(String code, int percent, LocalDate validUntil) {
        this.code = code;
        this.percent = percent;
        this.validUntil = validUntil;
    }

    public boolean isValidOn(LocalDate day) {
        return !day.isAfter(validUntil);
    }

    public String getCode() {
        return code;
    }

    public int getPercent() {
        return percent;
    }

    public LocalDate getValidUntil() {
        return validUntil;
    }
}
