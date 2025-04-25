package controller.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;

import java.io.IOException;

/**
 * A custom Filter for processing HTTP requests and responses.
 * Applied to URL pattern /* by default.
 */
@WebFilter(
        filterName = "RoleFilter",
        urlPatterns = "/*"
)
public class RoleFilter implements Filter {

    /**
     * Initialize the filter.
     */
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    /**
     * Process the request and response.
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

    }

    /**
     * Clean up resources.
     */
    @Override
    public void destroy() {
    }
}