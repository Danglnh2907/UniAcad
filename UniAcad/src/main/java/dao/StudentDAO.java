package dao;

import model.Student;
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

    public static void main(String[] args) {
        StudentDAO studentDAO = new StudentDAO();
        String email = "abc@gmail.com";
        studentDAO.getStudentByEmail(email);
    }

    public Student getStudentByEmail(String email) {
        String sql = "SELECT * " + "FROM Student WHERE StudentEmail = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                logger.info("Find student with email {}", email);
                return resultMap(rs, new Student());
            }
        } catch (SQLException e) {
            logger.error("Error retrieving student by email", e);
        }
        logger.warn("Student with email {} does not exist", email);
        return null;
    }

    public Student resultMap(ResultSet rs, Student student) {
        try {
            student.setStudentId(rs.getString("StudentID"));
            student.setStudentEmail(rs.getString("StudentEmail"));
            student.setLastName(rs.getString("LastName"));
            student.setMidleName(rs.getString("MiddleName"));
            student.setFirstName(rs.getString("FirstName"));
            student.setStudentDoB(rs.getDate("StudentDoB"));
            student.setStudentGender(rs.getInt("StudentGender"));
            student.setStudentSSN(rs.getString("StudentSSN"));
            student.setDistrict(rs.getString("District"));
            student.setProvince(rs.getString("Province"));
            student.setDetail(rs.getString("Detail"));
            student.setTown(rs.getString("Town"));
            student.setStudentPhone(rs.getString("StudentPhone"));
            student.setCurriculumId(rs.getString("CurriculumID"));
            return student;
        } catch (SQLException e) {
            logger.error("Error mapping result set to Student object", e);
        }
        return null;
    }
}