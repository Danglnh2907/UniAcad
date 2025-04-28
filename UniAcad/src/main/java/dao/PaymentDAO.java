package dao;

import model.database.Payment;
import util.service.database.DBContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

public class PaymentDAO extends DBContext {
    public PaymentDAO() {
        super();
    }

    public void save(Payment payment) {
        String sql = "INSERT INTO Payment (FeeID, PaymentDate, AmountPaid, PaymentStatus) VALUES (?, ?, ?, ?)";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, payment.getFeeID().getId()); // Lấy ID của Fee
            if (payment.getPaymentDate() != null) {
                ps.setTimestamp(2, Timestamp.from(payment.getPaymentDate()));
            } else {
                ps.setNull(2, java.sql.Types.TIMESTAMP);
            }
            ps.setBigDecimal(3, payment.getAmountPaid());
            ps.setInt(4, payment.getPaymentStatus());

            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error saving Payment", e);
        }
    }
}
