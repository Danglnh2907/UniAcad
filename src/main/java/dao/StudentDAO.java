package dao;

import org.slf4j.Logger;
import util.service.database.DBContext;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class StudentDAO extends DBContext {
    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(StudentDAO.class);

    public StudentDAO() {
        super();
    }

    public boolean checkEmailExists(String email) {
        String sql = "SELECT COUNT(*) FROM Student WHERE StudentEmail = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            logger.error("Error checking email existence", e);
        }
        logger.warn("Email with email {} does not exist", email);
        return false;
    }

    public boolean checkStudentIdExists(String studentId) {
        String sql = "SELECT COUNT(*) FROM students WHERE student_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, studentId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                logger.warn("Student ID with ID {} exists", studentId);
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        logger.warn("Student ID with ID {} does not exist", studentId);
        return false;
    }
}
