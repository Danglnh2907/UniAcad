package dao;

import model.database.Curriculum;
import model.database.Student;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.service.database.DBContext;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class StudentDAO extends DBContext {
    private static final Logger logger = LoggerFactory.getLogger(StudentDAO.class);

    public StudentDAO() {
        super();
    }

    /**
     * Checks if an email already exists in the database.
     */
    public boolean checkEmailExists(String email) {
        String query = "SELECT COUNT(*) FROM Student WHERE StudentEmail = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, email);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getInt(1) > 0;
            }
        } catch (SQLException e) {
            logger.error("Error checking email existence: {}", email, e);
        }
        return false;
    }

    /**
     * Maps a ResultSet to a Student object.
     */
    private Student resultMap(ResultSet resultSet) throws SQLException {
        if (!resultSet.next()) {
            return null;
        }

        // Assume CurriculumDAO exists to fetch Curriculum details
        CurriculumDAO curriculumDAO = new CurriculumDAO();
        Curriculum curriculum = curriculumDAO.getCurriculumById(resultSet.getString("CurriculumID"));

        return new Student(
                resultSet.getString("StudentID"),
                resultSet.getString("StudentEmail"),
                resultSet.getString("StudentName"),
                resultSet.getDate("StudentDoB").toLocalDate(),
                resultSet.getBoolean("StudentGender"),
                resultSet.getString("StudentSSN"),
                resultSet.getString("Address"),
                resultSet.getInt("StudentStatus"),
                resultSet.getString("StudentPhone"),
                curriculum
        );
    }

    /**
     * Retrieves a student by email.
     */
    public Student getStudentByEmail(String email) {
        String query = "SELECT * FROM Student WHERE StudentEmail = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, email);
            ResultSet resultSet = statement.executeQuery();
            return resultMap(resultSet);
        } catch (SQLException e) {
            logger.error("Error retrieving student by email: {}", email, e);
        }
        return null;
    }

    /**
     * Retrieves a student's name by email.
     */
    public String getNameByEmail(String email) {
        String query = "SELECT StudentName FROM Student WHERE StudentEmail = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, email);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getString("StudentName");
            }
        } catch (SQLException e) {
            logger.error("Error retrieving student name by email: {}", email, e);
        }
        return null;
    }

    /**
     * Retrieves a student by StudentID.
     */
    public Student getStudentById(String studentId) {
        String query = "SELECT * FROM Student WHERE StudentID = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, studentId);
            ResultSet resultSet = statement.executeQuery();
            return resultMap(resultSet);
        } catch (SQLException e) {
            logger.error("Error retrieving student by ID: {}", studentId, e);
        }
        return null;
    }

    /**
     * Retrieves all students.
     */
    public List<Student> getAllStudents() {
        List<Student> students = new ArrayList<>();
        String query = "SELECT * FROM Student";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                Student student = resultMap(resultSet);
                if (student != null) {
                    students.add(student);
                    resultSet.relative(-1); // Move back to read the next record
                }
            }
        } catch (SQLException e) {
            logger.error("Error retrieving all students", e);
        }
        return students;
    }

    /**
     * Creates a new student.
     */
    public boolean createStudent(Student student) {
        String query = "INSERT INTO Student (StudentID, StudentEmail, StudentName, StudentDoB, StudentGender, " +
                "StudentSSN, Address, StudentStatus, StudentPhone, CurriculumID) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, student.getStudentID());
            statement.setString(2, student.getStudentEmail());
            statement.setString(3, student.getStudentName());
            statement.setDate(4, Date.valueOf(student.getStudentDoB()));
            statement.setBoolean(5, student.getStudentGender());
            statement.setString(6, student.getStudentSSN());
            statement.setString(7, student.getAddress());
            statement.setInt(8, student.getStudentStatus());
            statement.setString(9, student.getStudentPhone());
            statement.setString(10, student.getCurriculumID().getCurriculumID());

            int rowsAffected = statement.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            logger.error("Error creating student: {}", student.getStudentID(), e);
        }
        return false;
    }

    /**
     * Updates an existing student.
     */
    public boolean updateStudent(Student student) {
        String query = "UPDATE Student SET StudentEmail = ?, StudentName = ?, StudentDoB = ?, StudentGender = ?, " +
                "StudentSSN = ?, Address = ?, StudentStatus = ?, StudentPhone = ?, CurriculumID = ? " +
                "WHERE StudentID = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, student.getStudentEmail());
            statement.setString(2, student.getStudentName());
            statement.setDate(3, Date.valueOf(student.getStudentDoB()));
            statement.setBoolean(4, student.getStudentGender());
            statement.setString(5, student.getStudentSSN());
            statement.setString(6, student.getAddress());
            statement.setInt(7, student.getStudentStatus());
            statement.setString(8, student.getStudentPhone());
            statement.setString(9, student.getCurriculumID().getCurriculumID());
            statement.setString(10, student.getStudentID());

            int rowsAffected = statement.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            logger.error("Error updating student: {}", student.getStudentID(), e);
        }
        return false;
    }

    /**
     * Deletes a student by StudentID.
     */
    public boolean deleteStudent(String studentId) {
        String query = "DELETE FROM Student WHERE StudentID = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, studentId);
            int rowsAffected = statement.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            logger.error("Error deleting student: {}", studentId, e);
        }
        return false;
    }

    /**
     * Retrieves students by status.
     */
    public List<Student> getStudentsByStatus(int status) {
        List<Student> students = new ArrayList<>();
        String query = "SELECT * FROM Student WHERE StudentStatus = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, status);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                Student student = resultMap(resultSet);
                if (student != null) {
                    students.add(student);
                    resultSet.relative(-1); // Move back to read the next record
                }
            }
        } catch (SQLException e) {
            logger.error("Error retrieving students by status: {}", status, e);
        }
        return students;
    }

    /**
     * Retrieves students by CurriculumID.
     */
    public List<Student> getStudentsByCurriculum(String curriculumId) {
        List<Student> students = new ArrayList<>();
        String query = "SELECT * FROM Student WHERE CurriculumID = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, curriculumId);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                Student student = resultMap(resultSet);
                if (student != null) {
                    students.add(student);
                    resultSet.relative(-1); // Move back to read the next record
                }
            }
        } catch (SQLException e) {
            logger.error("Error retrieving students by CurriculumID: {}", curriculumId, e);
        }
        return students;
    }

    public static void main(String[] args) {
        StudentDAO studentDAO = new StudentDAO();
        String email = "abc@gmail.com";
        Student student = studentDAO.getStudentByEmail(email);
        if (student != null) {
            System.out.println("Student Name: " + student.getStudentName());
        } else {
            System.out.println("No student found with email: " + email);
        }
    }
}