package demo.shop.customer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

@DataJpaTest
@Import(CustomerService.class)
class CustomerServiceTest {
    @Autowired
    CustomerService customers;

    @Test
    void registersCustomerWithNormalizedEmail() {
        Customer registered = customers.register("Ada", "Ada@Example.org");

        Customer loaded = customers.get(registered.getId());
        assertThat(loaded.getEmail()).isEqualTo("ada@example.org");
        assertThat(loaded.getRegisteredAt()).isNotNull();
    }

    @Test
    void refusesSecondRegistrationWithSameEmail() {
        customers.register("Ada", "ada@example.org");

        ResponseStatusException e = catchThrowableOfType(
            () -> customers.register("Ada again", "ADA@example.org"), ResponseStatusException.class);
        assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void unknownCustomerIsNotFound() {
        ResponseStatusException e = catchThrowableOfType(() -> customers.get(424242L), ResponseStatusException.class);
        assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
