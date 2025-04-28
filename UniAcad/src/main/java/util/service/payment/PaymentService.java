package util.service.payment;

import dao.FeeDAO;
import dao.PaymentDAO;
import jakarta.persistence.EntityManager;
import model.database.Fee;
import model.database.Payment;

import java.math.BigDecimal;
import java.time.Instant;

public class PaymentService {

    private final FeeDAO feeDAO;
    private final PaymentDAO paymentDAO;
    private final EntityManager entityManager;

    public PaymentService(EntityManager entityManager) {
        this.entityManager = entityManager;
        this.feeDAO = new FeeDAO(entityManager);
        this.paymentDAO = new PaymentDAO(entityManager);
    }

    public void payFee(Integer feeId, BigDecimal amountToPay) {
        entityManager.getTransaction().begin();
        try {
            Fee fee = feeDAO.findById(feeId);
            if (fee == null) {
                throw new IllegalArgumentException("Fee not found for ID: " + feeId);
            }

            if (fee.getFeeStatus() != 0) {
                throw new IllegalStateException("Fee has already been paid.");
            }

            if (fee.getAmount().compareTo(amountToPay) != 0) {
                throw new IllegalArgumentException("Payment amount does not match the required fee amount.");
            }

            Payment payment = new Payment();
            payment.setFeeID(fee);
            payment.setAmountPaid(amountToPay);
            payment.setPaymentDate(Instant.now());
            payment.setPaymentStatus(1); // 1 = thành công

            paymentDAO.save(payment);

            fee.setFeeStatus(1); // cập nhật Fee thành đã thanh toán
            feeDAO.update(fee);

            entityManager.getTransaction().commit();
        } catch (Exception e) {
            entityManager.getTransaction().rollback();
            throw e;
        }
    }
}
