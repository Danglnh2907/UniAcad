package dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import model.database.Fee;

public class FeeDAO {

    private final EntityManager entityManager;

    public FeeDAO(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Fee findById(Integer id) {
        return entityManager.find(Fee.class, id);
    }

    public void save(Fee fee) {
        EntityTransaction transaction = entityManager.getTransaction();
        try {
            transaction.begin();
            entityManager.persist(fee);
            transaction.commit();
        } catch (Exception e) {
            transaction.rollback();
            throw e;
        }
    }

    public void update(Fee fee) {
        EntityTransaction transaction = entityManager.getTransaction();
        try {
            transaction.begin();
            entityManager.merge(fee);
            transaction.commit();
        } catch (Exception e) {
            transaction.rollback();
            throw e;
        }
    }
}
