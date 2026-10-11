package demo.shop.web;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class RequestLoggingFilterTest {
    private final RequestLoggingFilter filter = new RequestLoggingFilter();

    @Test
    void keepsCorrelationIdOfTheCaller() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/orders");
        request.addHeader(RequestLoggingFilter.HEADER, "abc-123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader(RequestLoggingFilter.HEADER)).isEqualTo("abc-123");
    }

    @Test
    void generatesCorrelationIdWhenMissing() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(new MockHttpServletRequest("GET", "/api/products"), response, new MockFilterChain());

        assertThat(response.getHeader(RequestLoggingFilter.HEADER)).hasSize(36);
    }

    @Test
    void onlyWritesChangeData() {
        assertThat(RequestLoggingFilter.changesData(HttpMethod.POST)).isTrue();
        assertThat(RequestLoggingFilter.changesData(HttpMethod.DELETE)).isTrue();
        assertThat(RequestLoggingFilter.changesData(HttpMethod.GET)).isFalse();
    }
}
