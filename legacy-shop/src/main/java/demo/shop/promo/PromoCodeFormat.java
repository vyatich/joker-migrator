package demo.shop.promo;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A promo code is 4 to 20 upper-case letters or digits, so it can be read out over the phone.
 */
@Documented
@Constraint(validatedBy = PromoCodeFormatValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface PromoCodeFormat {
    String message() default "must be 4 to 20 upper-case letters or digits";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
