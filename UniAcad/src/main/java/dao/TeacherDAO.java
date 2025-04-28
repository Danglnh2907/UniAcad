package dao;

import model.database.Department;
import model.database.Teacher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.service.database.DBContext;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TeacherDAO extends DBContext {
    private static final Logger logger = LoggerFactory.getLogger(TeacherDAO.class);

    public TeacherDAO() {
        super();
    }

    /**
     * Checks if a TeacherID already exists in the database.
     */
    public boolean checkTeacherExists(String teacherId) {
        String query = "SELECT COUNT(*) FROM Teacher WHERE TeacherID = ?";
        try (PreparedStatement statement = getConnection().prepareStatement(query)) {
            statement.setString(1, teacherId);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getInt(1) > 0;
            }
        } catch (SQLException e) {
            logger.error("Error checking teacher existence: {}", teacherId, e);
        }
        return false;
    }

    /**
     * Checks if a TeacherEmail already exists in the database.
     */
    public boolean checkEmailExists(String email) {
        String query = "SELECT COUNT(*) FROM Teacher WHERE TeacherEmail = ?";
        try (PreparedStatement statement = getConnection().prepareStatement(query)) {
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
     * Maps a ResultSet to a Teacher object.
     */
    private Teacher resultMap(ResultSet resultSet) throws SQLException {
        if (!resultSet.next()) {
            return null;
        }

        // Assume DepartmentDAO exists to fetch Department details
        DepartmentDAO departmentDAO = new DepartmentDAO();
        String departmentId = resultSet.getString("DepartmentID");
        Department department = departmentId != null ? departmentDAO.getDepartmentById(departmentId) : null;

        Teacher teacher = new Teacher();
        teacher.setTeacherID(resultSet.getString("TeacherID"));
        teacher.setTeacherEmail(resultSet.getString("TeacherEmail"));
        teacher.setTeacherName(resultSet.getString("TeacherName"));
        teacher.setTeacherPhone(resultSet.getString("TeacherPhone"));
        teacher.setTeacherStatus(resultSet.getInt("TeacherStatus"));
        teacher.setDepartmentID(department);

        return teacher;
    }

    /**
     * Retrieves a teacher by TeacherID.
     */
    public Teacher getTeacherById(String teacherId) {
        String query = "SELECT * FROM Teacher WHERE TeacherID = ?";
        try (PreparedStatement statement = getConnection().prepareStatement(query)) {
            statement.setString(1, teacherId);
            ResultSet resultSet = statement.executeQuery();
            return resultMap(resultSet);
        } catch (SQLException e) {
            logger.error("Error retrieving teacher by ID: {}", teacherId, e);
        }
        return null;
    }

    /**
     * Retrieves a teacher by TeacherEmail.
     */
    public Teacher getTeacherByEmail(String email) {
        String query = "SELECT * FROM Teacher WHERE TeacherEmail = ?";
        try (PreparedStatement statement = getConnection().prepareStatement(query)) {
            statement.setString(1, email);
            ResultSet resultSet = statement.executeQuery();
            return resultMap(resultSet);
        } catch (SQLException e) {
            logger.error("Error retrieving teacher by email: {}", email, e);
        }
        return null;
    }

    /**
     * Retrieves all teachers.
     */
    public List<Teacher> getAllTeachers() {
        List<Teacher> teachers = new ArrayList<>();
        String query = "SELECT * FROM Teacher";
        try (PreparedStatement statement = getConnection().prepareStatement(query)) {
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                Teacher teacher = resultMap(resultSet);
                if (teacher != null) {
                    teachers.add(teacher);
                    resultSet.relative(-1); // Move back to read the next record
                }
            }
        } catch (SQLException e) {
            logger.error("Error retrieving all teachers", e);
        }
        return teachers;
    }

    /**
     * Retrieves teachers by DepartmentID.
     */
    public List<Teacher> getTeachersByDepartment(String departmentId) {
        List<Teacher> teachers = new ArrayList<>();
        String query = "SELECT * FROM Teacher WHERE DepartmentID = ?";
        try (PreparedStatement statement = getConnection().prepareStatement(query)) {
            statement.setString(1, departmentId);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                Teacher teacher = resultMap(resultSet);
                if (teacher != null) {
                    teachers.add(teacher);
                    resultSet.relative(-1); // Move back to read the next record
                }
            }
        } catch (SQLException e) {
            logger.error("Error retrieving teachers by DepartmentID: {}", departmentId, e);
        }
        return teachers;
    }

    /**
     * Retrieves teachers by status.
     */
    public List<Teacher> getTeachersByStatus(int status) {
        List<Teacher> teachers = new ArrayList<>();
        String query = "SELECT * FROM Teacher WHERE TeacherStatus = ?";
        try (PreparedStatement statement = getConnection().prepareStatement(query)) {
            statement.setInt(1, status);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                Teacher teacher = resultMap(resultSet);
                if (teacher != null) {
                    teachers.add(teacher);
                    resultSet.relative(-1); // Move back to read the next record
                }
            }
        } catch (SQLException e) {
            logger.error("Error retrieving teachers by status: {}", status, e);
        }
        return teachers;
    }

    /**
     * Creates a new teacher.
     */
    public boolean createTeacher(Teacher teacher) {
        String query = "INSERT INTO Teacher (TeacherID, TeacherEmail, TeacherName, TeacherPhone, TeacherStatus, DepartmentID) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = getConnection().prepareStatement(query)) {
            statement.setString(1, teacher.getTeacherID());
            statement.setString(2, teacher.getTeacherEmail());
            statement.setString(3, teacher.getTeacherName());
            statement.setString(4, teacher.getTeacherPhone());
            statement.setInt(5, teacher.getTeacherStatus());
            statement.setString(6, teacher.getDepartmentID() != null ? teacher.getDepartmentID().getDepartmentID() : null);

            int rowsAffected = statement.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            logger.error("Error creating teacher: {}", teacher.getTeacherID(), e);
        }
        return false;
    }

    /**
     * Updates an existing teacher.
     */
    public boolean updateTeacher(Teacher teacher) {
        String query = "UPDATE Teacher SET TeacherEmail = ?, TeacherName = ?, TeacherPhone = ?, TeacherStatus = ?, " +
                "DepartmentID = ? WHERE TeacherID = ?";
        try (PreparedStatement statement = getConnection().prepareStatement(query)) {
            statement.setString(1, teacher.getTeacherEmail());
            statement.setString(2, teacher.getTeacherName());
            statement.setString(3, teacher.getTeacherPhone());
            statement.setInt(4, teacher.getTeacherStatus());
            statement.setString(5, teacher.getDepartmentID() != null ? teacher.getDepartmentID().getDepartmentID() : null);
            statement.setString(6, teacher.getTeacherID());

            int rowsAffected = statement.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            logger.error("Error updating teacher: {}", teacher.getTeacherID(), e);
        }
        return false;
    }

    /**
     * Deletes a teacher by TeacherID.
     */
    public boolean deleteTeacher(String teacherId) {
        String query = "DELETE FROM Teacher WHERE TeacherID = ?";
        try (PreparedStatement statement = getConnection().prepareStatement(query)) {
            statement.setString(1, teacherId);
            int rowsAffected = statement.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            logger.error("Error deleting teacher: {}", teacherId, e);
        }
        return false;
    }

    public static void main(String[] args) {
        TeacherDAO teacherDAO = new TeacherDAO();
        String teacherId = "TEA001";
        Teacher teacher = teacherDAO.getTeacherById(teacherId);
        if (teacher != null) {
            System.out.println("Teacher Name: " + teacher.getTeacherName());
        } else {
            System.out.println("No teacher found with ID: " + teacherId);
        }
    }
}