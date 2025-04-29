package dao;

import model.database.Curriculum;
import model.database.Major;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.service.database.DBContext;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CurriculumDAO extends DBContext {
    private static final Logger logger = LoggerFactory.getLogger(CurriculumDAO.class);

    public CurriculumDAO() {
        super();
    }

    /**
     * Checks if a CurriculumID already exists in the database.
     */
    public boolean checkCurriculumExists(String curriculumId) {
        String query = "SELECT COUNT(*) FROM Curriculum WHERE CurriculumID = ?";
        try (PreparedStatement statement = getConnection().prepareStatement(query)) {
            statement.setString(1, curriculumId);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getInt(1) > 0;
            }
        } catch (SQLException e) {
            logger.error("Error checking curriculum existence: {}", curriculumId, e);
        }
        return false;
    }

    /**
     * Maps a ResultSet to a Curriculum object.
     */
    private Curriculum resultMap(ResultSet resultSet) throws SQLException {
        // Không .next() ở đây
        MajorDAO majorDAO = new MajorDAO();
        Major major = majorDAO.getMajorById(resultSet.getString("MajorID"));
        Curriculum curriculum = new Curriculum();
        curriculum.setCurriculumID(resultSet.getString("CurriculumID"));
        curriculum.setCurriculumName(resultSet.getString("CurriculumName"));
        curriculum.setMajorID(major);
        return curriculum;
    }

    /**
     * Retrieves a curriculum by CurriculumID.
     */
    public Curriculum getCurriculumById(String curriculumId) {
        String query = "SELECT * FROM Curriculum WHERE CurriculumID = ?";
        try (PreparedStatement statement = getConnection().prepareStatement(query)) {
            statement.setString(1, curriculumId);
            ResultSet resultSet = statement.executeQuery();
            return resultMap(resultSet);
        } catch (SQLException e) {
            logger.error("Error retrieving curriculum by ID: {}", curriculumId, e);
        }
        return null;
    }

    /**
     * Retrieves all curricula.
     */
    public List<Curriculum> getAllCurricula() {
        List<Curriculum> curricula = new ArrayList<>();
        String query = "SELECT * FROM Curriculum";
        try (PreparedStatement statement = getConnection().prepareStatement(query)) {
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                Curriculum curriculum = resultMap(resultSet);
                if (curriculum != null) {
                    curricula.add(curriculum);
                    resultSet.relative(-1); // Move back to read the next record
                }
            }
        } catch (SQLException e) {
            logger.error("Error retrieving all curricula", e);
        }
        return curricula;
    }

    /**
     * Retrieves curricula by MajorID.
     */
    public List<Curriculum> getCurriculaByMajor(String majorId) {
        List<Curriculum> curricula = new ArrayList<>();
        String query = "SELECT * FROM Curriculum WHERE MajorID = ?";
        try (PreparedStatement statement = getConnection().prepareStatement(query)) {
            statement.setString(1, majorId);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                Curriculum curriculum = resultMap(resultSet);
                if (curriculum != null) {
                    curricula.add(curriculum);
                    resultSet.relative(-1); // Move back to read the next record
                }
            }
        } catch (SQLException e) {
            logger.error("Error retrieving curricula by MajorID: {}", majorId, e);
        }
        return curricula;
    }

    /**
     * Creates a new curriculum.
     */
    public boolean createCurriculum(Curriculum curriculum) {
        String query = "INSERT INTO Curriculum (CurriculumID, CurriculumName, MajorID) VALUES (?, ?, ?)";
        try (PreparedStatement statement = getConnection().prepareStatement(query)) {
            statement.setString(1, curriculum.getCurriculumID());
            statement.setString(2, curriculum.getCurriculumName());
            statement.setString(3, curriculum.getMajorID().getMajorID());

            int rowsAffected = statement.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            logger.error("Error creating curriculum: {}", curriculum.getCurriculumID(), e);
        }
        return false;
    }

    /**
     * Updates an existing curriculum.
     */
    public boolean updateCurriculum(Curriculum curriculum) {
        String query = "UPDATE Curriculum SET CurriculumName = ?, MajorID = ? WHERE CurriculumID = ?";
        try (PreparedStatement statement = getConnection().prepareStatement(query)) {
            statement.setString(1, curriculum.getCurriculumName());
            statement.setString(2, curriculum.getMajorID().getMajorID());
            statement.setString(3, curriculum.getCurriculumID());

            int rowsAffected = statement.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            logger.error("Error updating curriculum: {}", curriculum.getCurriculumID(), e);
        }
        return false;
    }

    /**
     * Deletes a curriculum by CurriculumID.
     */
    public boolean deleteCurriculum(String curriculumId) {
        String query = "DELETE FROM Curriculum WHERE CurriculumID = ?";
        try (PreparedStatement statement = getConnection().prepareStatement(query)) {
            statement.setString(1, curriculumId);
            int rowsAffected = statement.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            logger.error("Error deleting curriculum: {}", curriculumId, e);
        }
        return false;
    }

    public static void main(String[] args) {
        CurriculumDAO curriculumDAO = new CurriculumDAO();
        String curriculumId = "CURR001";
        Curriculum curriculum = curriculumDAO.getCurriculumById(curriculumId);
        if (curriculum != null) {
            System.out.println("Curriculum Name: " + curriculum.getCurriculumName());
        } else {
            System.out.println("No curriculum found with ID: " + curriculumId);
        }
    }
}