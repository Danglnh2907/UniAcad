package dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.service.database.DBContext;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DepartmentDAO extends DBContext {
    private static final Logger logger = LoggerFactory.getLogger(DepartmentDAO.class);

    public DepartmentDAO() {
        super();
    }

    /**
     * Checks if a DepartmentID already exists in the database.
     */
    public boolean checkDepartmentExists(String departmentId) {
        String query = "SELECT COUNT(*) FROM Department WHERE DepartmentID = ?";
        try (PreparedStatement statement = getConnection().prepareStatement(query)) {
            statement.setString(1, departmentId);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getInt(1) > 0;
            }
        } catch (SQLException e) {
            logger.error("Error checking department existence: {}", departmentId, e);
        }
        return false;
    }

    /**
     * Maps a ResultSet to a Department object.
     */
    private Department resultMap(ResultSet resultSet) throws SQLException {
        if (!resultSet.next()) {
            return null;
        }

        Department department = new Department();
        department.setDepartmentID(resultSet.getString("DepartmentID"));
        department.setDepartmentName(resultSet.getString("DepartmentName"));

        return department;
    }

    /**
     * Retrieves a department by DepartmentID.
     */
    public Department getDepartmentById(String departmentId) {
        String query = "SELECT * FROM Department WHERE DepartmentID = ?";
        try (PreparedStatement statement = getConnection().prepareStatement(query)) {
            statement.setString(1, departmentId);
            ResultSet resultSet = statement.executeQuery();
            return resultMap(resultSet);
        } catch (SQLException e) {
            logger.error("Error retrieving department by ID: {}", departmentId, e);
        }
        return null;
    }

    /**
     * Retrieves all departments.
     */
    public List<Department> getAllDepartments() {
        List<Department> departments = new ArrayList<>();
        String query = "SELECT * FROM Department";
        try (PreparedStatement statement = getConnection().prepareStatement(query)) {
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                Department department = resultMap(resultSet);
                if (department != null) {
                    departments.add(department);
                    resultSet.relative(-1); // Move back to read the next record
                }
            }
        } catch (SQLException e) {
            logger.error("Error retrieving all departments", e);
        }
        return departments;
    }

    /**
     * Creates a new department.
     */
    public boolean createDepartment(Department department) {
        String query = "INSERT INTO Department (DepartmentID, DepartmentName) VALUES (?, ?)";
        try (PreparedStatement statement = getConnection().prepareStatement(query)) {
            statement.setString(1, department.getDepartmentID());
            statement.setString(2, department.getDepartmentName());

            int rowsAffected = statement.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            logger.error("Error creating department: {}", department.getDepartmentID(), e);
        }
        return false;
    }

    /**
     * Updates an existing department.
     */
    public boolean updateDepartment(Department department) {
        String query = "UPDATE Department SET DepartmentName = ? WHERE DepartmentID = ?";
        try (PreparedStatement statement = getConnection().prepareStatement(query)) {
            statement.setString(1, department.getDepartmentName());
            statement.setString(2, department.getDepartmentID());

            int rowsAffected = statement.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            logger.error("Error updating department: {}", department.getDepartmentID(), e);
        }
        return false;
    }

    /**
     * Deletes a department by DepartmentID.
     */
    public boolean deleteDepartment(String departmentId) {
        String query = "DELETE FROM Department WHERE DepartmentID = ?";
        try (PreparedStatement statement = getConnection().prepareStatement(query)) {
            statement.setString(1, departmentId);
            int rowsAffected = statement.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            logger.error("Error deleting department: {}", departmentId, e);
        }
        return false;
    }

    public static void main(String[] args) {
        DepartmentDAO departmentDAO = new DepartmentDAO();
        String departmentId = "DEP01";
        Department department = departmentDAO.getDepartmentById(departmentId);
        if (department != null) {
            System.out.println("Department Name: " + department.getDepartmentName());
        } else {
            System.out.println("No department found with ID: " + departmentId);
        }
    }
}