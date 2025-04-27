package dao;

import model.database.Staff;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.service.database.DBContext;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class StaffDAO extends DBContext {
    private static final Logger logger = LoggerFactory.getLogger(StaffDAO.class);

    public StaffDAO() {
        super();
    }

    /**
     * Checks if a StaffID already exists in the database.
     */
    public boolean checkStaffExists(String staffId) {
        String query = "SELECT COUNT(*) FROM Staff WHERE StaffID = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, staffId);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getInt(1) > 0;
            }
        } catch (SQLException e) {
            logger.error("Error checking staff existence: {}", staffId, e);
        }
        return false;
    }

    /**
     * Checks if a StaffEmail already exists in the database.
     */
    public boolean checkEmailExists(String email) {
        String query = "SELECT COUNT(*) FROM Staff WHERE StaffEmail = ?";
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
     * Maps a ResultSet to a Staff object.
     */
    private Staff resultMap(ResultSet resultSet) throws SQLException {
        if (!resultSet.next()) {
            return null;
        }

        Staff staff = new Staff();
        staff.setStaffID(resultSet.getString("StaffID"));
        staff.setStaffName(resultSet.getString("StaffName"));
        staff.setStaffEmail(resultSet.getString("StaffEmail"));
        staff.setStaffPhone(resultSet.getString("StaffPhone"));
        staff.setStaffStatus(resultSet.getInt("StaffStatus"));

        return staff;
    }

    /**
     * Retrieves a staff member by StaffID.
     */
    public Staff getStaffById(String staffId) {
        String query = "SELECT * FROM Staff WHERE StaffID = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, staffId);
            ResultSet resultSet = statement.executeQuery();
            return resultMap(resultSet);
        } catch (SQLException e) {
            logger.error("Error retrieving staff by ID: {}", staffId, e);
        }
        return null;
    }

    /**
     * Retrieves a staff member by StaffEmail.
     */
    public Staff getStaffByEmail(String email) {
        String query = "SELECT * FROM Staff WHERE StaffEmail = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, email);
            ResultSet resultSet = statement.executeQuery();
            return resultMap(resultSet);
        } catch (SQLException e) {
            logger.error("Error retrieving staff by email: {}", email, e);
        }
        return null;
    }

    /**
     * Retrieves all staff members.
     */
    public List<Staff> getAllStaff() {
        List<Staff> staffList = new ArrayList<>();
        String query = "SELECT * FROM Staff";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                Staff staff = resultMap(resultSet);
                if (staff != null) {
                    staffList.add(staff);
                    resultSet.relative(-1); // Move back to read the next record
                }
            }
        } catch (SQLException e) {
            logger.error("Error retrieving all staff", e);
        }
        return staffList;
    }

    /**
     * Retrieves staff members by status.
     */
    public List<Staff> getStaffByStatus(int status) {
        List<Staff> staffList = new ArrayList<>();
        String query = "SELECT * FROM Staff WHERE StaffStatus = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, status);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                Staff staff = resultMap(resultSet);
                if (staff != null) {
                    staffList.add(staff);
                    resultSet.relative(-1); // Move back to read the next record
                }
            }
        } catch (SQLException e) {
            logger.error("Error retrieving staff by status: {}", status, e);
        }
        return staffList;
    }

    /**
     * Creates a new staff member.
     */
    public boolean createStaff(Staff staff) {
        String query = "INSERT INTO Staff (StaffID, StaffName, StaffEmail, StaffPhone, StaffStatus) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, staff.getStaffID());
            statement.setString(2, staff.getStaffName());
            statement.setString(3, staff.getStaffEmail());
            statement.setString(4, staff.getStaffPhone());
            statement.setInt(5, staff.getStaffStatus());

            int rowsAffected = statement.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            logger.error("Error creating staff: {}", staff.getStaffID(), e);
        }
        return false;
    }

    /**
     * Updates an existing staff member.
     */
    public boolean updateStaff(Staff staff) {
        String query = "UPDATE Staff SET StaffName = ?, StaffEmail = ?, StaffPhone = ?, StaffStatus = ? WHERE StaffID = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, staff.getStaffName());
            statement.setString(2, staff.getStaffEmail());
            statement.setString(3, staff.getStaffPhone());
            statement.setInt(4, staff.getStaffStatus());
            statement.setString(5, staff.getStaffID());

            int rowsAffected = statement.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            logger.error("Error updating staff: {}", staff.getStaffID(), e);
        }
        return false;
    }

    /**
     * Deletes a staff member by StaffID.
     */
    public boolean deleteStaff(String staffId) {
        String query = "DELETE FROM Staff WHERE StaffID = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, staffId);
            int rowsAffected = statement.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            logger.error("Error deleting staff: {}", staffId, e);
        }
        return false;
    }

    public static void main(String[] args) {
        StaffDAO staffDAO = new StaffDAO();
        String staffId = "STA001";
        Staff staff = staffDAO.getStaffById(staffId);
        if (staff != null) {
            System.out.println("Staff Name: " + staff.getStaffName());
        } else {
            System.out.println("No staff found with ID: " + staffId);
        }
    }
}