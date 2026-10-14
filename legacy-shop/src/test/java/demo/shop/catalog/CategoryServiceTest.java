package demo.shop.catalog;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

@DataJpaTest
@Import(CategoryService.class)
class CategoryServiceTest {
    @Autowired
    CategoryService categories;

    @Test
    void listsCategoriesByName() {
        categories.create("Toys");
        categories.create(" Books ");

        assertThat(categories.findAll()).extracting(Category::getName).containsExactly("Books", "Toys");
    }

    @Test
    void refusesDuplicateNamesIgnoringCase() {
        categories.create("Books");

        ResponseStatusException e = catchThrowableOfType(() -> categories.create("BOOKS"), ResponseStatusException.class);
        assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }
}
