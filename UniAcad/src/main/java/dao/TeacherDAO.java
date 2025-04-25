package dao;

import model.Teacher;
import util.service.database.DBContext;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class TeacherDAO extends DBContext {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TeacherDAO.class);

    public TeacherDAO() {
        super();
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

    public static void main(String[] args) {

    }

    public Teacher getTeacherByEmail(String email) {
        String sql = "SELECT * " + "FROM Teacher WHERE TeacherEmail = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                logger.info("Find teacher with email {}", email);
                return resultMap(rs);
            }
        } catch (SQLException e) {
            logger.error("Error retrieving teacher by email", e);
        }
        logger.warn("Teacher with email {} does not exist", email);
        return null;
    }

    public Teacher resultMap(ResultSet rs) throws SQLException {
        Teacher teacher = new Teacher();
        teacher.setTeacherId(rs.getString("TeacherId"));
        teacher.setTeacherEmail(rs.getString("TeacherEmail"));
        teacher.setFirstName(rs.getString("FirstName"));
        teacher.setMidleName(rs.getString("MidleName"));
        teacher.setLastName(rs.getString("LastName"));
        teacher.setTeacherPhone(rs.getString("TeacherPhone"));
        teacher.setDepartmentId(rs.getString("DepartmentId"));
        return teacher;
    }
}