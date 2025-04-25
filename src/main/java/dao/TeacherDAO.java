package dao;

import util.service.database.DBContext;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class TeacherDAO extends DBContext {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TeacherDAO.class);

    public TeacherDAO() {
        super();
    }

    public static void main(String[] args) {
        TeacherDAO teacherDAO = new TeacherDAO();
        System.out.println(teacherDAO.checkEmailExists("abc@gmail.com"));
    }

    public boolean checkEmailExists(String email) {
        String sql = "SELECT COUNT(*) FROM Teacher WHERE TeacherEmail = ?";
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
}
