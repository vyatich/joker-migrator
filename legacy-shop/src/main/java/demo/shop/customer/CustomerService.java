package demo.shop.customer;

import com.corp.commons.core.CorpLog;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@Service
@Transactional(readOnly = true)
public class CustomerService {
    private static final CorpLog log = CorpLog.getLogger(CustomerService.class);

    private final CustomerRepository customers;

    public CustomerService(CustomerRepository customers) {
        this.customers = customers;
    }

    public Customer get(long id) {
        return customers.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer " + id + " not found"));
    }

    @Transactional
    public Customer register(String name, String email) {
        if (customers.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email " + email + " is already registered");
        }
        Customer customer = customers.save(new Customer(name, email.toLowerCase(), Instant.now()));
        log.info("Registered customer {}", customer.getId());
        return customer;
    }
}
