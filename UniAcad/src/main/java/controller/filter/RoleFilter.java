package controller.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * A filter that enforces authentication and role-based access control for /teacher/*, /student/*, and /staff/*.
 * Redirects unauthenticated users to /google-oauth and denies unauthorized access with /error.
 */
@WebFilter(
        filterName = "RoleFilter",
        urlPatterns = {"/teacher/*", "/student/*", "/staff/*"}
)
public class RoleFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(RoleFilter.class);

    // Role-based path prefixes (Teacher, Student, Admin)
    private static final Map<String, String> ROLE_PATH_PREFIXES = new HashMap<>();

    static {
        ROLE_PATH_PREFIXES.put("staff", "/staff/");   // Admin role maps to /staff/*
        ROLE_PATH_PREFIXES.put("student", "/student/"); // Student role maps to /student/*
        ROLE_PATH_PREFIXES.put("teacher", "/teacher/"); // Teacher role maps to /teacher/*
    }

    // Session timeout in seconds (30 minutes)
    private static final long SESSION_TIMEOUT = 30 * 60;

    /**
     * Initialize the filter.
     */
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("RoleFilter initialized");
    }

    /**
     * Process the request for /teacher/*, /student/*, and /staff/*:
     * - Redirect unauthenticated users to /google-oauth.
     * - Enforce role-based access for Teacher (/teacher/*), Student (/student/*), and Admin (/staff/*).
     * - Redirect to /error for invalid roles or unauthorized paths.
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        HttpSession session = httpRequest.getSession(false);

        String requestURI = httpRequest.getRequestURI();
        String contextPath = httpRequest.getContextPath();
        String path = requestURI.substring(contextPath.length());

        // Check if user is authenticated
        if (session == null || session.getAttribute("email") == null || session.getAttribute("role") == null) {
            logger.warn("Unauthorized access attempt to: {}. Redirecting to /google-oauth", path);
            httpResponse.sendRedirect(contextPath + "/google-oauth");
            return;
        }

        // Check session timeout
        long lastAccessedTime = session.getLastAccessedTime();
        long currentTime = System.currentTimeMillis();
        if ((currentTime - lastAccessedTime) / 1000 > SESSION_TIMEOUT) {
            logger.warn("Session expired for user with email {}. Redirecting to /google-oauth", session.getAttribute("email"));
            session.invalidate();
            httpResponse.sendRedirect(contextPath + "/google-oauth");
            return;
        }

        // Get user email and role from session
        String email = (String) session.getAttribute("email");
        String role = (String) session.getAttribute("role");

        // Verify if role is valid
        if (!ROLE_PATH_PREFIXES.containsKey(role)) {
            logger.warn("Invalid role {} for user with email {}. Redirecting to /error", role, email);
            httpResponse.sendRedirect(contextPath + "/error?message=Invalid%20user%20role");
            return;
        }

        // Check if the requested path is allowed for the user's role
        String allowedPrefix = ROLE_PATH_PREFIXES.get(role);
        boolean isAllowed = path.startsWith(allowedPrefix);

        // Allow or deny access based on role
        if (isAllowed) {
            logger.debug("User with email {} and role {} accessing allowed path: {}", email, role, path);
            chain.doFilter(request, response);
        } else {
            logger.warn("User with email {} and role {} attempted to access forbidden path: {}. Redirecting to /error",
                    email, role, path);
            httpResponse.sendRedirect(contextPath + "/error?message=Access%20denied%20for%20this%20path");
        }
    }

    /**
     * Clean up resources.
     */
    @Override
    public void destroy() {
        logger.info("RoleFilter destroyed");
    }
}