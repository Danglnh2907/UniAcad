package dao;

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

    /**
     * Checks if a MajorID already exists in the database.
     */
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

    /**
     * Maps a ResultSet to a Major object.
     */
    private Major resultMap(ResultSet resultSet) throws SQLException {
        if (!resultSet.next()) {
            return null;
        }

        Major major = new Major();
        major.setMajorID(resultSet.getString("MajorID"));
        major.setMajorName(resultSet.getString("MajorName"));

        return major;
    }

    /**
     * Retrieves a major by MajorID.
     */
    public Major getMajorById(String majorId) {
        String query = "SELECT * FROM Major WHERE MajorID = ?";
        try (PreparedStatement statement = getConnection().prepareStatement(query)) {
            statement.setString(1, majorId);
            ResultSet resultSet = statement.executeQuery();
            return resultMap(resultSet);
        } catch (SQLException e) {
            logger.error("Error retrieving major by ID: {}", majorId, e);
        }
        return null;
    }

    /**
     * Retrieves all majors.
     */
    public List<Major> getAllMajors() {
        List<Major> majors = new ArrayList<>();
        String query = "SELECT * FROM Major";
        try (PreparedStatement statement = getConnection().prepareStatement(query)) {
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                Major major = resultMap(resultSet);
                if (major != null) {
                    majors.add(major);
                    resultSet.relative(-1); // Move back to read the next record
                }
            }
        } catch (SQLException e) {
            logger.error("Error retrieving all majors", e);
        }
        return majors;
    }

    /**
     * Creates a new major.
     */
    public boolean createMajor(Major major) {
        String query = "INSERT INTO Major (MajorID, MajorName) VALUES (?, ?)";
        try (PreparedStatement statement = getConnection().prepareStatement(query)) {
            statement.setString(1, major.getMajorID());
            statement.setString(2, major.getMajorName());

            int rowsAffected = statement.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            logger.error("Error creating major: {}", major.getMajorID(), e);
        }
        return false;
    }

    /**
     * Updates an existing major.
     */
    public boolean updateMajor(Major major) {
        String query = "UPDATE Major SET MajorName = ? WHERE MajorID = ?";
        try (PreparedStatement statement = getConnection().prepareStatement(query)) {
            statement.setString(1, major.getMajorName());
            statement.setString(2, major.getMajorID());

            int rowsAffected = statement.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            logger.error("Error updating major: {}", major.getMajorID(), e);
        }
        return false;
    }

    /**
     * Deletes a major by MajorID.
     */
    public boolean deleteMajor(String majorId) {
        String query = "DELETE FROM Major WHERE MajorID = ?";
        try (PreparedStatement statement = getConnection().prepareStatement(query)) {
            statement.setString(1, majorId);
            int rowsAffected = statement.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            logger.error("Error deleting major: {}", majorId, e);
        }
        return false;
    }

    public static void main(String[] args) {
        MajorDAO majorDAO = new MajorDAO();
        String majorId = "MAJ01";
        Major major = majorDAO.getMajorById(majorId);
        if (major != null) {
            System.out.println("Major Name: " + major.getMajorName());
        } else {
            System.out.println("No major found with ID: " + majorId);
        }
    }
}