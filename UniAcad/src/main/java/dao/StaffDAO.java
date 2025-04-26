package dao;

import model.Staff;
import util.service.database.DBContext;
import util.service.normalization.NormalizationService;
import util.service.security.VerifyChecking;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class StaffDAO extends DBContext {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(StaffDAO.class);
    private NormalizationService normalizationService = new NormalizationService();
    private VerifyChecking verifyChecking = new VerifyChecking();

    public StaffDAO() {
        super();
    }

    public boolean checkEmailExists(String email) {
        String sql = "SELECT COUNT(*) FROM Staff WHERE StaffEmail = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            logger.error("Error checking email existence: {}", e.getMessage(), e);
        }
        logger.warn("Email {} does not exist", email);
        return false;
    }

    public boolean checkStaffID(String staffId) {
        String sql = "SELECT COUNT(*) FROM Staff WHERE StaffID = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, staffId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            logger.error("Error checking staff ID existence: {}", e.getMessage(), e);
        }
        logger.warn("Staff with ID {} does not exist", staffId);
        return false;
    }

    public Staff getStaffByEmail(String email) {
        String sql = "SELECT * FROM Staff WHERE StaffEmail = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                logger.info("Found staff with email {}", email);
                return resultMap(rs, new Staff());
            }
        } catch (SQLException e) {
            logger.error("Error retrieving staff by email: {}", e.getMessage(), e);
        }
        logger.warn("Staff with email {} does not exist", email);
        return null;
    }

    public boolean addStaff(String staffId, String email, String lastName, String middleName, String staffPhone) {
        // Kiểm tra độ dài StaffID
        if (staffId.length() > 10) {
            logger.warn("StaffID {} exceeds maximum length of 10 characters", staffId);
            return false;
        }

        if (checkEmailExists(email)) {
            logger.warn("Email {} already exists", email);
            return false;
        }
        if (!verifyChecking.verifyEmail(email)) {
            logger.warn("Invalid email format: {}", email);
            return false;
        }
        if (!verifyChecking.verifyPhoneNumber(staffPhone)) {
            logger.warn("Invalid phone number format: {}", staffPhone);
            return false;
        }
        if (checkStaffID(staffId)) {
            logger.warn("StaffID {} already exists", staffId);
            return false;
        }

        String sql = "INSERT INTO Staff (StaffID, LastName, MiddleName, FullName, StaffEmail, StaffPhone) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            logger.debug("Inserting staff: StaffID={}, Email={}, LastName={}, MiddleName={}, Phone={}",
                    staffId, email, lastName, middleName, staffPhone);
            ps.setString(1, staffId);
            ps.setString(2, lastName);
            ps.setString(3, middleName);
            ps.setString(4, lastName + " " + middleName);
            ps.setString(5, normalizationService.normaliizationEmail(email));
            ps.setString(6, staffPhone);
            int rowsAffected = ps.executeUpdate();
            logger.info("Staff added successfully: StaffID={}", staffId);
            return rowsAffected > 0;
        } catch (SQLException e) {
            logger.error("Error adding staff with StaffID={}: {}", staffId, e.getMessage(), e);
            return false;
        }
    }

    public Staff resultMap(ResultSet rs, Staff staff) throws SQLException {
        staff.setStaffId(rs.getString("StaffID"));
        staff.setStaffEmail(rs.getString("StaffEmail"));
        staff.setLastName(rs.getString("LastName"));
        staff.setMiddleName(rs.getString("MiddleName"));
        staff.setFullName(rs.getString("FullName"));
        staff.setStaffPhone(rs.getString("StaffPhone"));
        return staff;
    }

    public static void main(String[] args) {
        StaffDAO staffDAO = new StaffDAO();
        System.out.println(staffDAO.getStaffByEmail("uiniacad.dev@gmail.com"));
    }
}