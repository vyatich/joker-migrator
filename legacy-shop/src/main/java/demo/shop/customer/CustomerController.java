package demo.shop.customer;

import demo.shop.api.CustomersApi;
import demo.shop.api.model.CustomerDto;
import demo.shop.api.model.NewCustomerDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZoneOffset;

@RestController
public class CustomerController implements CustomersApi {
    private final CustomerService customers;

    public CustomerController(CustomerService customers) {
        this.customers = customers;
    }

    @Override
    public ResponseEntity<CustomerDto> getCustomer(Long id) {
        return ResponseEntity.ok(toDto(customers.get(id)));
    }

    @Override
    public ResponseEntity<CustomerDto> registerCustomer(NewCustomerDto newCustomer) {
        Customer created = customers.register(newCustomer.getName(), newCustomer.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(created));
    }

    static CustomerDto toDto(Customer customer) {
        return new CustomerDto()
            .id(customer.getId())
            .name(customer.getName())
            .email(customer.getEmail())
            .registeredAt(customer.getRegisteredAt().atOffset(ZoneOffset.UTC));
    }
}
