package demo.shop.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.filter.UrlHandlerFilter;

@Configuration
public class WebConfig {
    /**
     * Spring 7 no longer matches "/path/" to "/path"; version 1 of the mobile app still calls the
     * API with a trailing slash, so strip it before security and handler mapping see the request.
     */
    @Bean
    FilterRegistrationBean<UrlHandlerFilter> trailingSlashFilter() {
        FilterRegistrationBean<UrlHandlerFilter> registration = new FilterRegistrationBean<>(
            UrlHandlerFilter.trailingSlashHandler("/api/**").wrapRequest().build());
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
