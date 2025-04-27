package controller.filter;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@WebFilter(
        filterName = "RoleFilter",
        urlPatterns = {"/teacher/*", "/student/*", "/staff/*"}
)
public class RoleFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(RoleFilter.class);

    private static final Map<String, String> ROLE_PATH_PREFIXES = new HashMap<>();

    static {
        ROLE_PATH_PREFIXES.put("staff", "/staff/");
        ROLE_PATH_PREFIXES.put("student", "/student/");
        ROLE_PATH_PREFIXES.put("teacher", "/teacher/");
    }

    private long sessionTimeout = 30 * 60 * 1000L; // 30 phút default (ms)

    @Override
    public void init(FilterConfig filterConfig) {
        String timeoutParam = filterConfig.getServletContext().getInitParameter("sessionTimeout");
        if (timeoutParam != null) {
            try {
                sessionTimeout = Long.parseLong(timeoutParam) * 1000L;
            } catch (NumberFormatException e) {
                logger.warn("Invalid sessionTimeout value in context, using default 30 minutes");
            }
        }
        logger.info("RoleFilter initialized with session timeout {} seconds", sessionTimeout / 1000);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        HttpSession session = httpRequest.getSession(false);
        String contextPath = httpRequest.getContextPath();
        String requestURI = httpRequest.getRequestURI();
        String path = requestURI.substring(contextPath.length());

        if (session == null || session.getAttribute("email") == null || session.getAttribute("role") == null) {
            logger.warn("Unauthorized access to {}. Redirecting to login page", path);
            redirect(httpRequest, httpResponse, contextPath);
            return;
        }

        if (isSessionExpired(session)) {
            logger.warn("Session expired for user. Invalidating session.");
            session.invalidate();
            redirect(httpRequest, httpResponse, contextPath);
            return;
        }

        String email = (String) session.getAttribute("email");
        String role = (String) session.getAttribute("role");

        String allowedPrefix = ROLE_PATH_PREFIXES.get(role);

        if (allowedPrefix == null || !path.startsWith(allowedPrefix)) {
            logger.warn("Access denied for {} with role {} at path {}", email, role, path);
            redirectError(httpRequest, httpResponse, contextPath);
            return;
        }

        logger.debug("Access granted for {} with role {} at {}", email, role, path);
        chain.doFilter(request, response);
    }

    private void redirect(HttpServletRequest req, HttpServletResponse resp, String contextPath) throws IOException {
        if (isAjaxRequest(req)) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.setContentType("application/json");
            resp.getWriter().write("{\"error\":\"Unauthorized\"}");
        } else {
            resp.sendRedirect(contextPath + "/");
        }
    }

    private void redirectError(HttpServletRequest req, HttpServletResponse resp, String contextPath) throws IOException {
        if (isAjaxRequest(req)) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            resp.setContentType("application/json");
            resp.getWriter().write("{\"error\":\"Access Denied\"}");
        } else {
            resp.sendRedirect(contextPath + "/error?message=Access%20Denied");
        }
    }

    private boolean isSessionExpired(HttpSession session) {
        return (System.currentTimeMillis() - session.getLastAccessedTime()) > sessionTimeout;
    }

    private boolean isAjaxRequest(HttpServletRequest request) {
        String requestedWith = request.getHeader("X-Requested-With");
        return "XMLHttpRequest".equalsIgnoreCase(requestedWith);
    }

    @Override
    public void destroy() {
        logger.info("RoleFilter destroyed");
    }
}
