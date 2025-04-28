package dao;

import model.database.Curriculum;
import model.database.Student;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.service.database.DBContext;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class StudentDAO extends DBContext {
    private static final Logger logger = LoggerFactory.getLogger(StudentDAO.class);

    public StudentDAO() {
        super();
    }

    public boolean checkEmailExists(String email) {
        String query = "SELECT 1 FROM Student WHERE StudentEmail = ?";
        try (PreparedStatement statement = getConnection().prepareStatement(query)) {
            statement.setString(1, email);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            logger.error("Error checking email existence: {}", email, e);
        }
        return false;
    }

    public String getNameByEmail(String email) {
        String query = "SELECT StudentName FROM Student WHERE StudentEmail = ?";
        try (PreparedStatement statement = getConnection().prepareStatement(query)) {
            statement.setString(1, email);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) return rs.getString("StudentName");
            }
        } catch (SQLException e) {
            logger.error("Error retrieving student name by email: {}", email, e);
        }
        return null;
    }

    public String getEmailById(String studentId) {
        String query = "SELECT StudentEmail FROM Student WHERE StudentID = ?";
        try (PreparedStatement statement = getConnection().prepareStatement(query)) {
            statement.setString(1, studentId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) return rs.getString("StudentEmail");
            }
        } catch (SQLException e) {
            logger.error("Error retrieving email by StudentID: {}", studentId, e);
        }
        return null;
    }

    private Student mapResult(ResultSet rs) throws SQLException {
        CurriculumDAO curriculumDAO = new CurriculumDAO();
        Curriculum curriculum = curriculumDAO.getCurriculumById(rs.getString("CurriculumID"));

        return new Student(
                rs.getString("StudentID"),
                rs.getString("StudentEmail"),
                rs.getString("StudentName"),
                rs.getDate("StudentDoB").toLocalDate(),
                rs.getBoolean("StudentGender"),
                rs.getString("StudentSSN"),
                rs.getString("Address"),
                rs.getInt("StudentStatus"),
                rs.getString("StudentPhone"),
                curriculum
        );
    }

    public Student getStudentByEmail(String email) {
        String query = "SELECT * FROM Student WHERE StudentEmail = ?";
        try (PreparedStatement statement = getConnection().prepareStatement(query)) {
            statement.setString(1, email);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? mapResult(rs) : null;
            }
        } catch (SQLException e) {
            logger.error("Error retrieving student by email: {}", email, e);
        }
        return null;
    }

    public Student getStudentById(String studentId) {
        String query = "SELECT * FROM Student WHERE StudentID = ?";
        try (PreparedStatement statement = getConnection().prepareStatement(query)) {
            statement.setString(1, studentId);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? mapResult(rs) : null;
            }
        } catch (SQLException e) {
            logger.error("Error retrieving student by ID: {}", studentId, e);
        }
        return null;
    }

    public static void main(String[] args) {
        StudentDAO studentDAO = new StudentDAO();
        String email = "khai1234sd@gmail.com";
        Student student = studentDAO.getStudentByEmail(email);
        if (student != null) {
            System.out.println("Student found: " + student.getStudentID());
        } else {
            System.out.println("No student found with email: " + email);
        }
    }
}
