package controller.servlet.payment;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import dao.FeeDAO;
import dao.StudentDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.database.Fee;
import model.database.Student;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.service.paymentconfig.PayOSConfig;
import vn.payos.PayOS;
import vn.payos.type.CheckoutResponseData;
import vn.payos.type.ItemData;
import vn.payos.type.PaymentData;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;

@WebServlet(name = "CheckoutServlet", urlPatterns = {"/success", "/cancel", "/create-payment-link"})
public class CheckoutServlet extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(CheckoutServlet.class);
    private final PayOS payOS;
    private final Gson gson = new GsonBuilder().create();

    public CheckoutServlet() {
        this.payOS = PayOSConfig.getPayOS();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getServletPath();
        String resource;
        switch (path) {
            case "/success":
                resource = "/success.html";
                break;
            case "/cancel":
                resource = "/cancel.html";
                break;
            default:
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Resource not found: " + path);
                return;
        }
        request.getRequestDispatcher(resource).forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        JsonObject responseJson = new JsonObject();

        if (!request.getServletPath().equals("/create-payment-link")) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        try {
            HttpSession session = request.getSession();
            StudentDAO studentDAO = new StudentDAO();
            Student student = studentDAO.getStudentByEmail(session.getAttribute("email").toString());
            logger.info("Processing /create-payment-link POST request for student: {}", student.getStudentID());

            final String baseUrl = getBaseUrl(request);
            final String productName = "University Fee, Student ID: " + student.getStudentID();
            final String returnUrl = baseUrl + "/success";
            final String cancelUrl = baseUrl + "/cancel";

            // Fetch unpaid fee
            FeeDAO feeDAO = new FeeDAO();
            Fee fee = feeDAO.findUnpaidFeeByStudentId(student.getStudentID());
            if (fee == null) {
                responseJson.addProperty("error", -1);
                responseJson.addProperty("message", "No unpaid fee found for student ID: " + student.getStudentID());
                response.getWriter().write(gson.toJson(responseJson));
                return;
            }

            // Amount
            BigDecimal feeAmount = fee.getAmount();
            if (feeAmount == null || feeAmount.compareTo(BigDecimal.valueOf(1000)) < 0) {
                throw new IllegalArgumentException("Invalid or too small fee amount.");
            }
            int price = feeAmount.intValue();
            logger.info("Validated price: {}", price);

            // Description
            String description = request.getParameter("description");
            if (description == null || description.trim().isEmpty()) {
                throw new IllegalArgumentException("Description is required.");
            }
            if (description.length() > 200) {
                throw new IllegalArgumentException("Description must not exceed 200 characters.");
            }

            // Generate OrderCode
            String currentTimeString = String.valueOf(new Date().getTime());
            long orderCode = fee.getId();
            logger.info("Generated orderCode: {}", orderCode);

            // Build Payment
            ItemData item = ItemData.builder()
                    .name(productName)
                    .quantity(1)
                    .price(price)
                    .build();

            PaymentData paymentData = PaymentData.builder()
                    .orderCode(orderCode)
                    .amount(price)
                    .description(description)
                    .returnUrl(returnUrl)
                    .cancelUrl(cancelUrl)
                    .item(item)
                    .build();

            CheckoutResponseData checkoutResponse = payOS.createPaymentLink(paymentData);
            if (checkoutResponse == null || checkoutResponse.getCheckoutUrl() == null) {
                throw new IllegalStateException("Failed to generate checkout URL.");
            }
            logger.info("Checkout URL: {}", checkoutResponse.getCheckoutUrl());

            // Response success
            responseJson.addProperty("error", 0);
            responseJson.addProperty("message", "success");
            JsonObject dataJson = new JsonObject();
            dataJson.addProperty("checkoutUrl", checkoutResponse.getCheckoutUrl());
            responseJson.add("data", dataJson);

        } catch (Exception e) {
            logger.error("Error creating payment link: {}", e.getMessage(), e);
            responseJson.addProperty("error", -1);
            responseJson.addProperty("message", "Failed to create payment link: " + e.getMessage());
            responseJson.add("data", null);
        }

        response.getWriter().write(gson.toJson(responseJson));
    }

    private String getBaseUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        String contextPath = request.getContextPath();

        StringBuilder url = new StringBuilder();
        url.append(scheme).append("://").append(serverName);
        if ((scheme.equals("http") && serverPort != 80) || (scheme.equals("https") && serverPort != 443)) {
            url.append(":").append(serverPort);
        }
        url.append(contextPath);
        return url.toString();
    }
}
