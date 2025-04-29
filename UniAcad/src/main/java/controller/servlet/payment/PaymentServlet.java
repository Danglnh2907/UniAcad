package controller.servlet.payment;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import util.service.payment.PaymentService;
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
import java.math.BigDecimal;

@WebServlet(name = "PaymentServlet", urlPatterns = {"/student/payment/payos_transfer_handler"})
public class PaymentServlet extends HttpServlet {

    private final PayOS payOS;
    private final Gson gson = new GsonBuilder().create();
    private final PaymentService paymentService = new PaymentService(); // ✅ Thêm PaymentService

    public PaymentServlet() {
        this.payOS = PayOSConfig.getPayOS();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        JsonObject responseJson = new JsonObject();

        try {
            // 1. Read Webhook Body
            StringBuilder jb = new StringBuilder();
            String line;
            BufferedReader reader = request.getReader();
            while ((line = reader.readLine()) != null) {
                jb.append(line);
            }
            Webhook webhookBody = gson.fromJson(jb.toString(), Webhook.class);

            // 2. Verify Webhook
            WebhookData data = payOS.verifyPaymentWebhookData(webhookBody);
            System.out.println("Webhook received: " + data);

            // 3. Check if successful
            if ("00".equals(data.getCode())) {  // PayOS success code
                long orderCode = data.getOrderCode(); // orderCode = FeeID
                BigDecimal paidAmount = BigDecimal.valueOf(data.getAmount());

                // 4. Use PaymentService to handle
                paymentService.payFee((int) orderCode, paidAmount); // 👈 Xài service luôn
                System.out.println("Payment processed successfully for orderCode: " + orderCode);
            } else {
                System.out.println("Payment not successful, code: " + data.getCode());
            }

            // 5. Return OK
            responseJson.addProperty("error", 0);
            responseJson.addProperty("message", "Webhook handled successfully");
            responseJson.add("data", null);

        } catch (Exception e) {
            e.printStackTrace();
            responseJson.addProperty("error", -1);
            responseJson.addProperty("message", "Error handling webhook: " + e.getMessage());
            responseJson.add("data", null);
        }

        response.getWriter().write(gson.toJson(responseJson));
    }
}

