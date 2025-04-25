package dao;

import model.Staff;
import util.service.database.DBContext;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class StaffDAO extends DBContext {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(StaffDAO.class);

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
            logger.error("Error checking email existence", e);
        }
        logger.warn("Email with email {} does not exist", email);
        return false;
    }

    public Staff getStaffByEmail(String email) {
        String sql = "SELECT * " + "FROM Staff WHERE StaffEmail = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                logger.info("Find staff with email {}", email);
                return resultMap(rs, new Staff());
            }
        } catch (SQLException e) {
            logger.error("Error retrieving staff by email", e);
        }
        logger.warn("Staff with email {} does not exist", email);
        return null;
    }

    public Staff resultMap(ResultSet rs, Staff staff) throws SQLException {
        staff.setStaffId(rs.getString("StaffId"));
        staff.setStaffEmail(rs.getString("StaffEmail"));
        staff.setLastName(rs.getString("LastName"));
        staff.setMidleName(rs.getString("MidleName"));
        staff.setFullName(rs.getString("FullName"));
        staff.setStaffPhone(rs.getString("StaffPhone"));
        return staff;
    }
}