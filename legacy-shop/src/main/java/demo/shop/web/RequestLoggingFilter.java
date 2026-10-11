package demo.shop.web;

import com.corp.commons.util.CorpLogger;
import org.slf4j.MDC;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

/**
 * Gives every request a correlation id (taken from the caller or generated), puts it into the
 * logging context and the response, and logs requests that change data.
 */
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {
    static final String HEADER = "X-Correlation-Id";

    private static final CorpLogger log = CorpLogger.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String correlationId = request.getHeader(HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        MDC.put("correlationId", correlationId);
        response.setHeader(HEADER, correlationId);
        try {
            HttpMethod method = HttpMethod.resolve(request.getMethod());
            if (method != null && changesData(method)) {
                log.info("{} {} [{}]", method, request.getRequestURI(), correlationId);
            }
            chain.doFilter(request, response);
        } finally {
            MDC.remove("correlationId");
        }
    }

    static boolean changesData(HttpMethod method) {
        switch (method) {
            case POST:
            case PUT:
            case PATCH:
            case DELETE:
                return true;
            default:
                return false;
        }
    }
}
