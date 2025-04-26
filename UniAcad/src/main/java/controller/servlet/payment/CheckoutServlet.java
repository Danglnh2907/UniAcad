package controller.servlet.payment;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import util.service.paymentconfig.PayOSConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.payos.PayOS;
import vn.payos.type.CheckoutResponseData;
import vn.payos.type.ItemData;
import vn.payos.type.PaymentData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
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
                request.setAttribute("errorMessage", "Resource not found: " + path);
                request.getRequestDispatcher("/error.html").forward(request, response);
                return;
        }
        request.getRequestDispatcher(resource).forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (request.getServletPath().equals("/create-payment-link")) {
            response.setContentType("application/json");
            JsonObject responseJson = new JsonObject();
            try {
                logger.info("Processing /create-payment-link POST request");
                final String baseUrl = getBaseUrl(request);
                logger.info("Base URL: {}", baseUrl);
                final String productName = "Học phí đại học FPT";
                final String returnUrl = baseUrl + "/success";
                final String cancelUrl = baseUrl + "/cancel";

                // Get and validate amount
                String amountStr = request.getParameter("amount");
                logger.info("Input amount: {}", amountStr);
                if (amountStr == null || amountStr.trim().isEmpty()) {
                    throw new IllegalArgumentException("Amount is required");
                }
                int price;
                try {
                    price = Integer.parseInt(amountStr);
                    if (price < 1000) {
                        throw new IllegalArgumentException("Amount must be at least 1000 VND");
                    }
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid amount format");
                }
                logger.info("Validated price: {}", price);

                // Get and validate description
                String description = request.getParameter("description");
                logger.info("Input description: {}", description);
                if (description == null || description.trim().isEmpty()) {
                    throw new IllegalArgumentException("Description is required");
                }
                if (description.length() > 200) {
                    throw new IllegalArgumentException("Description must not exceed 200 characters");
                }

                // Generate order code
                String currentTimeString = String.valueOf(new Date().getTime());
                logger.info("Current time string: {}", currentTimeString);
                long orderCode = Long.parseLong(currentTimeString.substring(currentTimeString.length() - 6));
                logger.info("Order code: {}", orderCode);

                ItemData item = ItemData.builder().name(productName).quantity(1).price(price).build();
                PaymentData paymentData = PaymentData.builder()
                        .orderCode(orderCode)
                        .amount(price)
                        .description(description)
                        .returnUrl(returnUrl)
                        .cancelUrl(cancelUrl)
                        .item(item)
                        .build();
                logger.info("PaymentData created: {}", paymentData);

                CheckoutResponseData data = payOS.createPaymentLink(paymentData);
                logger.info("CheckoutResponseData: {}", data);
                String checkoutUrl = data.getCheckoutUrl();
                if (checkoutUrl == null) {
                    throw new IllegalStateException("PayOS returned null checkout URL");
                }
                logger.info("Checkout URL: {}", checkoutUrl);

                responseJson.addProperty("error", 0);
                responseJson.addProperty("message", "success");
                JsonObject dataJson = new JsonObject();
                dataJson.addProperty("checkoutUrl", checkoutUrl);
                responseJson.add("data", dataJson);
            } catch (Exception e) {
                logger.error("Error in /create-payment-link: {}", e.getMessage(), e);
                responseJson.addProperty("error", -1);
                responseJson.addProperty("message", "Failed to create payment link: " + e.getMessage());
                responseJson.add("data", null);
            }
            response.getWriter().write(gson.toJson(responseJson));
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private String getBaseUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        String contextPath = request.getContextPath();

        String url = scheme + "://" + serverName;
        if ((scheme.equals("http") && serverPort != 80) || (scheme.equals("https") && serverPort != 443)) {
            url += ":" + serverPort;
        }
        url += contextPath;
        return url;
    }
}