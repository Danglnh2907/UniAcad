package controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * A custom Servlet for handling HTTP requests.
 * Mapped to /loginservlet by default.
 */
@WebServlet(
        name = "LoginServlet",
        value = "/login"
)
public class LoginServlet extends HttpServlet {

    /**
     * Handle GET requests.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            request.getRequestDispatcher("index.jsp").forward(request, response);
        } catch (Exception e) {
            Logger.getLogger("LoginServlet").severe("Error in doGet: " + e.getMessage());
        }
    }

    /**
     * Handle POST requests.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try{
            doGet(request,response);
        } catch (Exception e) {
            Logger.getLogger("LoginServlet").severe("Error in doPost: " + e.getMessage());
        }
    }
}