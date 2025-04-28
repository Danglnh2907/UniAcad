package controller.filter;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebFilter("/*") // Bắt tất cả URL
public class GlobalExceptionFilter implements Filter {

    private final Gson gson = new Gson();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        try {
            chain.doFilter(request, response); // Cho request chạy tiếp xuống servlet
        } catch (Exception e) {
            // Nếu Servlet/Controller có lỗi -> catch tại đây
            handleException((HttpServletResponse) response, e);
        }
    }

    private void handleException(HttpServletResponse response, Exception e) throws IOException {
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        response.setContentType("application/json");

        JsonObject errorJson = new JsonObject();
        errorJson.addProperty("error", -1);
        errorJson.addProperty("message", e.getMessage() != null ? e.getMessage() : "Internal Server Error");
        errorJson.add("data", null);

        response.getWriter().write(gson.toJson(errorJson));
    }
}
