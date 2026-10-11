package demo.shop.promo;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class PromoCodeFormatValidator implements ConstraintValidator<PromoCodeFormat, String> {
    private static final Pattern FORMAT = Pattern.compile("[A-Z0-9]{4,20}");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || isWellFormed(value);
    }

    static boolean isWellFormed(String code) {
        return FORMAT.matcher(code).matches();
    }
}
