package dao;

import model.database.Payment;
import util.service.database.DBContext;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;

/**
 * PaymentDAO handles database operations related to the Payment entity.
 */
public class PaymentDAO {

    /**
     * Saves a new Payment record into the database.
     *
     * @param payment the Payment object containing data to be inserted
     */
    public void save(Payment payment) {
        String sql = "INSERT INTO Payment (FeeID, AmountPaid, PaymentDate, PaymentStatus) VALUES (?, ?, ?, ?)";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, payment.getFeeID().getId());
            ps.setBigDecimal(2, payment.getAmountPaid());
            ps.setTimestamp(3, Timestamp.from(payment.getPaymentDate()));
            ps.setInt(4, payment.getPaymentStatus());

            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error saving Payment", e);
        }
    }

    public static void main(String[] args) {
        PaymentDAO paymentDAO = new PaymentDAO();
        Payment payment = new Payment();
        payment.setFeeID(new FeeDAO().findById(3)); // Example FeeID
        payment.setAmountPaid(BigDecimal.valueOf(2000)); // Example amount
        payment.setPaymentDate(Instant.now());
        payment.setPaymentStatus(1); // Assuming 1 means successful
        paymentDAO.save(payment);
    }
}
