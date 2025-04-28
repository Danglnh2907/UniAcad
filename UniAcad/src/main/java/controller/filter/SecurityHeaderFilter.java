package controller.filter;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * A filter that adds essential security headers to all responses.
 */
@WebFilter(filterName = "SecurityHeaderFilter", urlPatterns = {"/*"})
public class SecurityHeaderFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(SecurityHeaderFilter.class);

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("SecurityHeaderFilter initialized");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (response instanceof HttpServletResponse httpResponse) {

            logger.debug("Applying security headers to response");

            // Prevent Clickjacking
            httpResponse.setHeader("X-Frame-Options", "DENY");
            logger.debug("Added X-Frame-Options: DENY");

            // Enable XSS Protection
            httpResponse.setHeader("X-XSS-Protection", "1; mode=block");
            logger.debug("Added X-XSS-Protection: 1; mode=block");

            // Prevent MIME sniffing
            httpResponse.setHeader("X-Content-Type-Options", "nosniff");
            logger.debug("Added X-Content-Type-Options: nosniff");

            // Content Security Policy (basic)
//            httpResponse.setHeader("Content-Security-Policy",
//                    "default-src 'self'; script-src 'self' https://apis.google.com; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self';");
//            logger.debug("Added Content-Security-Policy header");
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        logger.info("SecurityHeaderFilter destroyed");
    }
}
