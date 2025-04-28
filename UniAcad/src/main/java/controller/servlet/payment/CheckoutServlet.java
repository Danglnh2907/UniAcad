package controller.servlet.payment;// ✨ Cleaned CheckoutServlet.java\package controller.servlet.payment;

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
import util.service.payment.PaymentService;
import util.service.paymentconfig.PayOSConfig;
import vn.payos.PayOS;
import vn.payos.type.CheckoutResponseData;
import vn.payos.type.ItemData;
import vn.payos.type.PaymentData;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;

@WebServlet(name = "CheckoutServlet", urlPatterns = {"/student/success", "/student/cancel", "/student/create-payment-link"}, description = "Handles payment link creation and result redirects.")
public class CheckoutServlet extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(CheckoutServlet.class);
    private final PayOS payOS = PayOSConfig.getPayOS();
    private final Gson gson = new GsonBuilder().create();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        PaymentService paymentService = new PaymentService();
        StudentDAO studentDAO = new StudentDAO();
        Student student = null;
        try {
            HttpSession session = request.getSession();
            student = studentDAO.getStudentByEmail(session.getAttribute("email").toString());
        } catch (Exception e) {
            logger.error("Error retrieving student information", e);
        }
        Fee fee = new FeeDAO().findUnpaidFeeByStudentId(student.getStudentID());
        switch (request.getServletPath()) {
            case "/student/success":
                paymentService.payFee(fee.getId(), fee.getAmount());
                request.getRequestDispatcher("/student/success.html").forward(request, response);
                break;
            case "/student/cancel":
                request.getRequestDispatcher("/student/cancel.html").forward(request, response);
            break;
            default: response.sendError(HttpServletResponse.SC_NOT_FOUND, "Resource not found");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!"/student/create-payment-link".equals(request.getServletPath())) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        JsonObject resJson = new JsonObject();
        try {
            HttpSession session = request.getSession();
            Student student = new StudentDAO().getStudentByEmail(session.getAttribute("email").toString());

            Fee fee = new FeeDAO().findUnpaidFeeByStudentId(student.getStudentID());
            if (fee == null) {
                writeJsonResponse(response, buildErrorResponse("No unpaid fee found."));
                return;
            }

            BigDecimal feeAmount = fee.getAmount();
            if (feeAmount == null || feeAmount.compareTo(BigDecimal.valueOf(1000)) < 0)
                throw new IllegalArgumentException("Invalid or too small fee amount.");

            String description = "Student ID: " + student.getStudentID();
            CheckoutResponseData checkoutData = payOS.createPaymentLink(buildPaymentData(feeAmount.intValue(), description, student.getStudentID(), getBaseUrl(request)));
            JsonObject dataJson = new JsonObject();
            dataJson.addProperty("checkoutUrl", checkoutData.getCheckoutUrl());
            resJson.addProperty("error", 0);
            resJson.addProperty("message", "success");
            resJson.add("data", dataJson);
        } catch (Exception e) {
            logger.error("Error creating payment link", e);
            resJson = buildErrorResponse(e.getMessage());
        }
        writeJsonResponse(response, resJson);
    }

    private PaymentData buildPaymentData(int price, String description, String studentId, String baseUrl) {
        String productName = "University Fee, Student ID: " + studentId;
        long orderCode = Long.parseLong(String.valueOf(new Date().getTime()).substring(7));
        ItemData item = ItemData.builder().name(productName).quantity(1).price(price).build();

        return PaymentData.builder()
                .orderCode(orderCode)
                .amount(price)
                .description(description)
                .returnUrl(baseUrl + "/student/success")
                .cancelUrl(baseUrl + "/student/cancel")
                .item(item)
                .build();
    }

    private String getBaseUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        String contextPath = request.getContextPath();

        return scheme + "://" + serverName + ((serverPort == 80 || serverPort == 443) ? "" : ":" + serverPort) + contextPath;
    }

    private void writeJsonResponse(HttpServletResponse response, JsonObject resJson) throws IOException {
        response.setContentType("application/json");
        response.getWriter().write(gson.toJson(resJson));
    }

    private JsonObject buildErrorResponse(String message) {
        JsonObject resJson = new JsonObject();
        resJson.addProperty("error", -1);
        resJson.addProperty("message", message);
        resJson.add("data", null);
        return resJson;
    }
}
