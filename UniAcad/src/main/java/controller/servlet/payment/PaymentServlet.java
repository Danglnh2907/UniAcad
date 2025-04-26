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
import vn.payos.type.Webhook;
import vn.payos.type.WebhookData;

import java.io.BufferedReader;
import java.io.IOException;

@WebServlet(name = "PaymentServlet", urlPatterns = {"/payment/payos_transfer_handler"})
public class PaymentServlet extends HttpServlet {

    private final PayOS payOS;
    private final Gson gson = new GsonBuilder().create();

    public PaymentServlet() {
        this.payOS = PayOSConfig.getPayOS();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        JsonObject responseJson = new JsonObject();

        try {
            // Read JSON request body
            StringBuilder jb = new StringBuilder();
            String line;
            BufferedReader reader = request.getReader();
            while ((line = reader.readLine()) != null) {
                jb.append(line);
            }
            Webhook webhookBody = gson.fromJson(jb.toString(), Webhook.class);

            WebhookData data = payOS.verifyPaymentWebhookData(webhookBody);
            System.out.println(data);
            responseJson.addProperty("error", 0);
            responseJson.addProperty("message", "Webhook delivered");
            responseJson.add("data", null);
        } catch (Exception e) {
            e.printStackTrace();
            responseJson.addProperty("error", -1);
            responseJson.addProperty("message", e.getMessage());
            responseJson.add("data", null);
        }
        response.getWriter().write(gson.toJson(responseJson));
    }
}