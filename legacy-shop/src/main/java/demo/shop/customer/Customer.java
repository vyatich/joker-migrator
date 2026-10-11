package demo.shop.customer;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import java.time.Instant;

@Entity
public class Customer {
    @Id
    @GeneratedValue
    private Long id;
    @NotBlank
    private String name;
    @Email
    @Column(unique = true, nullable = false)
    private String email;
    private Instant registeredAt;

    protected Customer() {
    }

    public Customer(String name, String email, Instant registeredAt) {
        this.name = name;
        this.email = email;
        this.registeredAt = registeredAt;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public Instant getRegisteredAt() {
        return registeredAt;
    }
}
