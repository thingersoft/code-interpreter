package it.aci.ai.mcp.servers.code_interpreter.librechat.api;

import java.io.IOException;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

import it.aci.ai.mcp.servers.code_interpreter.config.AppConfig;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * LibreChat API authorization filter configuration
 */
@Configuration
public class LibreChatApiAuthConfig {

    public static class LibreChatAuthFilter implements Filter {

        private String apiKey;

        public LibreChatAuthFilter(String apiKey) {
            this.apiKey = apiKey;
        }

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {
            if (apiKey.equals(((HttpServletRequest) request).getHeader("x-api-key"))) {
                chain.doFilter(request, response);
            } else {
                ((HttpServletResponse) response).setStatus(HttpStatus.UNAUTHORIZED.value());
            }
        }

    }

    @Bean
    public FilterRegistrationBean<LibreChatAuthFilter> loggingFilter(AppConfig appConfig) {
        FilterRegistrationBean<LibreChatAuthFilter> authFilterBean = new FilterRegistrationBean<>();
        authFilterBean.setFilter(new LibreChatAuthFilter(appConfig.getApiKey()));
        authFilterBean.addUrlPatterns(LibreChatApiController.LIBRECHAT_API_PATH + "/*");
        authFilterBean.setOrder(1);
        return authFilterBean;
    }

}
