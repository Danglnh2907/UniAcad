package dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import model.database.Payment;

public class PaymentDAO {

    private final EntityManager entityManager;

    public PaymentDAO(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public void save(Payment payment) {
        EntityTransaction transaction = entityManager.getTransaction();
        try {
            transaction.begin();
            entityManager.persist(payment);
            transaction.commit();
        } catch (Exception e) {
            transaction.rollback();
            throw e;
        }
    }
}
