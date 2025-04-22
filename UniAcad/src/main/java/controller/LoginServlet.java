package controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * LoginServlet is a servlet that handles user login requests.
 *
 * It manages both GET and POST HTTP methods for directing user requests
 * and facilitating interaction with the login page.
 */
@WebServlet(
        name = "LoginServlet",
        value = "/login"
)
public class LoginServlet extends HttpServlet {

    /**
     * Handles HTTP GET requests.
     * Forwards the incoming request to the "index.jsp" page and processes the response.
     * Logs an error message in case of any exception during the operation.
     *
     * @param request  the HttpServletRequest object that contains the request the client has made
     * @param response the HttpServletResponse object that contains the response the servlet returns to the client
     * @throws ServletException if the request could not be handled
     * @throws IOException      if an input or output error occurs while the servlet is handling the request
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
     * Handles HTTP POST requests by delegating the processing to the doGet method.
     * Logs an error message in case of any exception during the operation.
     *
     * @param request  the HttpServletRequest object that contains the request the client has made
     * @param response the HttpServletResponse object that contains the response the servlet returns to the client
     * @throws ServletException if the request could not be handled
     * @throws IOException      if an input or output error occurs while the servlet is handling the request
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