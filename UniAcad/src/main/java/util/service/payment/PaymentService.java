package util.service.payment;

import dao.FeeDAO;
import dao.PaymentDAO;
import model.database.Fee;
import model.database.Payment;
import util.service.database.DBContext;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;

public class PaymentService {

    private final FeeDAO feeDAO;
    private final PaymentDAO paymentDAO;

    public PaymentService() {
        this.feeDAO = new FeeDAO();
        this.paymentDAO = new PaymentDAO();
    }

    public void payFee(Integer feeId, BigDecimal amountToPay) {
        Connection conn = null;
        try {
            conn = DBContext.getConnection();
            conn.setAutoCommit(false); // Bắt đầu transaction

            // 1. Find Fee theo feeId
            Fee fee = feeDAO.findById(feeId);
            if (fee == null) {
                throw new IllegalArgumentException("Fee not found for ID: " + feeId);
            }

            // 2. Check trạng thái và số tiền
            if (fee.getFeeStatus() != 0) {
                throw new IllegalStateException("Fee has already been paid.");
            }

            if (fee.getAmount().compareTo(amountToPay) != 0) {
                throw new IllegalArgumentException("Payment amount does not match the required fee amount.");
            }

            // 3. Insert Payment mới
            Payment payment = new Payment();
            payment.setFeeID(fee);
            payment.setAmountPaid(amountToPay);
            payment.setPaymentDate(Instant.now());
            payment.setPaymentStatus(1); // 1 = thành công

            paymentDAO.save(payment);

            // 4. Update Fee thành đã thanh toán
            fee.setFeeStatus(1);
            feeDAO.update(fee);

            conn.commit(); // Commit transaction
        } catch (Exception e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    throw new RuntimeException("Error during rollback", ex);
                }
            }
            throw new RuntimeException("Error processing payment", e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ex) {
                    throw new RuntimeException("Error closing connection", ex);
                }
            }
        }
    }
}
