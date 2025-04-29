package dao;

import model.database.Major;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.service.database.DBContext;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MajorDAO extends DBContext {
    private static final Logger logger = LoggerFactory.getLogger(MajorDAO.class);

    public MajorDAO() {
        super();
    }

    public boolean checkMajorExists(String majorId) {
        String query = "SELECT COUNT(*) FROM Major WHERE MajorID = ?";
        try (PreparedStatement statement = getConnection().prepareStatement(query)) {
            statement.setString(1, majorId);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getInt(1) > 0;
            }
        } catch (SQLException e) {
            logger.error("Error checking major existence: {}", majorId, e);
        }
        return false;
    }

    private Major resultMap(ResultSet resultSet) throws SQLException {
        Major major = new Major();
        major.setMajorID(resultSet.getString("MajorID"));
        major.setMajorName(resultSet.getString("MajorName"));
        return major;
    }

    public Major getMajorById(String majorId) {
        String query = "SELECT * FROM Major WHERE MajorID = ?";
        try (PreparedStatement statement = getConnection().prepareStatement(query)) {
            statement.setString(1, majorId);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultMap(resultSet);
            }
        } catch (SQLException e) {
            logger.error("Error retrieving major by ID: {}", majorId, e);
        }
        return null;
    }

    public List<Major> getAllMajors() {
        List<Major> majors = new ArrayList<>();
        String query = "SELECT * FROM Major";
        try (PreparedStatement statement = getConnection().prepareStatement(query)) {
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                Major major = resultMap(resultSet);
                majors.add(major);
            }
        } catch (SQLException e) {
            logger.error("Error retrieving all majors", e);
        }
        return majors;
    }

    public boolean createMajor(Major major) {
        String query = "INSERT INTO Major (MajorID, MajorName) VALUES (?, ?)";
        try (PreparedStatement statement = getConnection().prepareStatement(query)) {
            statement.setString(1, major.getMajorID());
            statement.setString(2, major.getMajorName());
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error creating major: {}", major.getMajorID(), e);
        }
        return false;
    }

    public boolean updateMajor(Major major) {
        String query = "UPDATE Major SET MajorName = ? WHERE MajorID = ?";
        try (PreparedStatement statement = getConnection().prepareStatement(query)) {
            statement.setString(1, major.getMajorName());
            statement.setString(2, major.getMajorID());
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error updating major: {}", major.getMajorID(), e);
        }
        return false;
    }

    public boolean deleteMajor(String majorId) {
        String query = "DELETE FROM Major WHERE MajorID = ?";
        try (PreparedStatement statement = getConnection().prepareStatement(query)) {
            statement.setString(1, majorId);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error deleting major: {}", majorId, e);
        }
        return false;
    }

    public static void main(String[] args) {
        MajorDAO majorDAO = new MajorDAO();
        String majorId = "SE";
        Major major = majorDAO.getMajorById(majorId);
        if (major != null) {
            System.out.println("Major Name: " + major.getMajorName());
        } else {
            System.out.println("No major found with ID: " + majorId);
        }
    }
}
