package util.service.payment;

import dao.FeeDAO;
import dao.PaymentDAO;
import model.database.Fee;
import model.database.Payment;
import util.service.database.DBContext;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.Instant;

/**
 * PaymentService handles the business logic for processing student fee payments.
 */
public class PaymentService {

    public static void main(String[] args) {
        PaymentService paymentService = new PaymentService();
        try {
            paymentService.payFee(3, BigDecimal.valueOf(2000));
            System.out.println("Payment processed successfully.");
        } catch (Exception e) {
            System.err.println("Error processing payment: " + e.getMessage());
        }
    }

    public void payFee(int feeId, BigDecimal amountPaid) {
        FeeDAO feeDAO = new FeeDAO();
        PaymentDAO paymentDAO = new PaymentDAO();

        try (Connection conn = DBContext.getConnection()) {
            // 1. Find the fee by ID
            Fee fee = feeDAO.findById(feeId);
            if (fee == null) {
                throw new IllegalArgumentException("Fee not found for ID: " + feeId);
            }
            // 2. Create a new Payment object
            Payment payment = new Payment();
            payment.setFeeID(fee);
            payment.setAmountPaid(amountPaid);
            payment.setPaymentDate(Instant.now());
            payment.setPaymentStatus(1); // 1 means payment successful
            // 3. Save the payment
            paymentDAO.save(payment);

            // 4. Update the fee status
            fee.setFeeStatus(1); // 1 means fee paid
            feeDAO.update(fee);
        } catch (Exception e) {
            throw new RuntimeException("Error processing payment", e);
        }
    }
}
