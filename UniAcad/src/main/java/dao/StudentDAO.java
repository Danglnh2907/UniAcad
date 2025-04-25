package dao;

import model.Student;
import org.slf4j.Logger;
import util.service.database.DBContext;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

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
        String email = "khainhce182286@gmail.com";
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

    public String getNameByEmail(String email) {
        String sql = "SELECT CONCAT(LastName, ' ',MiddleName,' ', FirstName) AS FullName FROM Student WHERE StudentEmail = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("FullName");
            }
        } catch (SQLException e) {
            logger.error("Error retrieving name by email", e);
        }
        return null;
    }

    public List<Student> getAllStudents() {
        String sql = "SELECT * FROM Student";
        List<Student> students = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                students.add(resultMap(rs, new Student()));
            }
        } catch (SQLException e) {
            logger.error("Error retrieving all students", e);
        }
        return students;
    }

    public boolean addStudent(Student student) {
        String sql = "INSERT INTO Student (StudentID, StudentEmail, LastName, FirstName, StudentDoB, StudentGender, StudentSSN, District, Province, Detail, Town, StudentPhone, CurriculumID) VALUES (?,?,?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, student.getStudentId());
            ps.setString(2, student.getStudentEmail());
            ps.setString(3, student.getLastName());
            ps.setString(4, student.getFirstName());
            ps.setDate(5, new java.sql.Date(student.getStudentDoB().getTime()));
            ps.setInt(6, student.getStudentGender());
            ps.setString(7, student.getStudentSSN());
            ps.setString(8, student.getDistrict());
            ps.setString(9, student.getProvince());
            ps.setString(10, student.getDetail());
            ps.setString(11, student.getTown());
            ps.setString(12, student.getStudentPhone());
            ps.setString(13, student.getCurriculumId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error adding new student", e);
        }
        return false;
    }
}