package controller.servlet.payment;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import util.service.paymentconfig.PayOSConfig;
import model.paymentModel.CreatePaymentLinkRequestBody;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.payos.PayOS;
import vn.payos.type.CheckoutResponseData;
import vn.payos.type.ItemData;
import vn.payos.type.PaymentData;
import vn.payos.type.PaymentLinkData;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@WebServlet(name = "OrderServlet", urlPatterns = {"/order/create", "/order/*", "/order/confirm-webhook"})
public class OrderServlet extends HttpServlet {

    private final PayOS payOS;
    private final Gson gson = new GsonBuilder().create();

    public OrderServlet() {
        this.payOS = PayOSConfig.getPayOS();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getServletPath();
        response.setContentType("application/json");
        JsonObject responseJson = new JsonObject();

        if (path.equals("/order/create")) {
            try {
                // Parse JSON request body
                StringBuilder jb = new StringBuilder();
                String line;
                BufferedReader reader = request.getReader();
                while ((line = reader.readLine()) != null) {
                    jb.append(line);
                }
                CreatePaymentLinkRequestBody requestBody = gson.fromJson(jb.toString(), CreatePaymentLinkRequestBody.class);

                final String productName = requestBody.getProductName();
                final String description = requestBody.getDescription();
                final String returnUrl = requestBody.getReturnUrl();
                final String cancelUrl = requestBody.getCancelUrl();
                final int price = requestBody.getPrice();

                // Generate order code
                String currentTimeString = String.valueOf(new Date().getTime());
                long orderCode = Long.parseLong(currentTimeString.substring(currentTimeString.length() - 6));

                ItemData item = ItemData.builder().name(productName).price(price).quantity(1).build();
                PaymentData paymentData = PaymentData.builder()
                        .orderCode(orderCode)
                        .description(description)
                        .amount(price)
                        .item(item)
                        .returnUrl(returnUrl)
                        .cancelUrl(cancelUrl)
                        .build();

                CheckoutResponseData data = payOS.createPaymentLink(paymentData);

                responseJson.addProperty("error", 0);
                responseJson.addProperty("message", "success");
                responseJson.add("data", gson.toJsonTree(data));
            } catch (Exception e) {
                e.printStackTrace();
                responseJson.addProperty("error", -1);
                responseJson.addProperty("message", "fail");
                responseJson.add("data", null);
            }
            response.getWriter().write(gson.toJson(responseJson));
        } else if (path.equals("/order/confirm-webhook")) {
            try {
                // Parse JSON request body as Map
                StringBuilder jb = new StringBuilder();
                String line;
                BufferedReader reader = request.getReader();
                while ((line = reader.readLine()) != null) {
                    jb.append(line);
                }
                Map<String, String> requestBody = gson.fromJson(jb.toString(), HashMap.class);

                String str = payOS.confirmWebhook(requestBody.get("webhookUrl"));
                responseJson.addProperty("error", 0);
                responseJson.addProperty("message", "ok");
                responseJson.add("data", gson.toJsonTree(str));
            } catch (Exception e) {
                e.printStackTrace();
                responseJson.addProperty("error", -1);
                responseJson.addProperty("message", e.getMessage());
                responseJson.add("data", null);
            }
            response.getWriter().write(gson.toJson(responseJson));
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo();
        response.setContentType("application/json");
        JsonObject responseJson = new JsonObject();

        if (pathInfo != null && pathInfo.matches("/\\d+")) {
            try {
                long orderId = Long.parseLong(pathInfo.substring(1));
                PaymentLinkData order = payOS.getPaymentLinkInformation(orderId);
                responseJson.addProperty("error", 0);
                responseJson.addProperty("message", "ok");
                responseJson.add("data", gson.toJsonTree(order));
            } catch (Exception e) {
                e.printStackTrace();
                responseJson.addProperty("error", -1);
                responseJson.addProperty("message", e.getMessage());
                responseJson.add("data", null);
            }
            response.getWriter().write(gson.toJson(responseJson));
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo();
        response.setContentType("application/json");
        JsonObject responseJson = new JsonObject();

        if (pathInfo != null && pathInfo.matches("/\\d+")) {
            try {
                int orderId = Integer.parseInt(pathInfo.substring(1));
                PaymentLinkData order = payOS.cancelPaymentLink(orderId, null);
                responseJson.addProperty("error", 0);
                responseJson.addProperty("message", "ok");
                responseJson.add("data", gson.toJsonTree(order));
            } catch (Exception e) {
                e.printStackTrace();
                responseJson.addProperty("error", -1);
                responseJson.addProperty("message", e.getMessage());
                responseJson.add("data", null);
            }
            response.getWriter().write(gson.toJson(responseJson));
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }
}