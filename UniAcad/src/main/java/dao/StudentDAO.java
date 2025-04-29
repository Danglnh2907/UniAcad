package dao;

import model.database.Curriculum;
import model.database.Student;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.service.database.DBContext;
import util.service.normalization.NormalizationService;
import util.service.security.VerifyChecking;

import java.sql.*;
import java.time.LocalDate;

public class StudentDAO extends DBContext {
    private static final Logger logger = LoggerFactory.getLogger(StudentDAO.class);
    private final VerifyChecking verifyChecking = new VerifyChecking();
    private final NormalizationService normalizationService = new NormalizationService();

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
                rs.getString("StudentName"),
                rs.getString("StudentSSN"),
                rs.getString("StudentEmail"),
                rs.getString("StudentPhone"),
                curriculum,
                rs.getBoolean("StudentGender"),
                rs.getString("Address"),
                rs.getDate("StudentDoB").toLocalDate(),
                rs.getObject("StudentStatus", Integer.class)
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

    public boolean addStudent(Student student) {
        if (student == null) {
            logger.error("Student is null.");
            return false;
        }

        // Chuẩn hóa dữ liệu đầu vào
        String email = normalizationService.normalizationEmail(student.getStudentEmail());
        String phone = student.getStudentPhone().trim();
        String name = normalizationService.normalizationFullName(student.getStudentName());
        String ssn = student.getStudentSSN().trim();
        String address = (student.getAddress() != null) ? student.getAddress().trim() : null;
        LocalDate dob = student.getStudentDoB();
        Integer status = student.getStudentStatus();
        Boolean gender = student.getStudentGender();
        Curriculum curriculum = student.getCurriculumID();

        // Validate dữ liệu sau khi chuẩn hóa
        if (!verifyChecking.verifyEmail(email)) {
            logger.error("Invalid email after normalization: {}", email);
            return false;
        }
        if (!verifyChecking.verifyPhoneNumber(phone)) {
            logger.error("Invalid phone number after normalization: {}", phone);
            return false;
        }
        if (!verifyChecking.verifyFullName(name)) {
            logger.error("Invalid full name after normalization: {}", name);
            return false;
        }

        String query = "INSERT INTO Student (StudentID, StudentName, StudentSSN, StudentEmail, StudentPhone, CurriculumID, StudentGender, Address, StudentDoB, StudentStatus) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setString(1, student.getStudentID());
            ps.setString(2, name);
            ps.setString(3, ssn);
            ps.setString(4, email);
            ps.setString(5, phone);
            ps.setString(6, curriculum.getCurriculumID());
            ps.setBoolean(7, gender);
            ps.setString(8, address);
            ps.setDate(9, Date.valueOf(dob));
            ps.setObject(10, status);

            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            logger.error("Error adding student: {}", student.getStudentID(), e);
        }
        return false;
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
