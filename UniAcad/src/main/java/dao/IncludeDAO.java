package dao;

import model.database.Include;
import model.database.Curriculum;
import model.database.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.service.database.DBContext;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class IncludeDAO extends DBContext {
    private static final Logger logger = LoggerFactory.getLogger(IncludeDAO.class);

    public IncludeDAO() {
        super();
    }

    private Include mapResult(ResultSet rs) throws SQLException {
        Include include = new Include();
        Curriculum curriculum = new Curriculum();
        curriculum.setCurriculumID(rs.getString("CurriculumID"));
        include.setCurriculumID(curriculum);

        Subject subject = new Subject();
        subject.setSubjectID(rs.getString("SubjectID"));
        include.setSubjectID(subject);

        include.setSemester(rs.getInt("Semester"));
        return include;
    }

    public List<Include> getAllIncludes() {
        List<Include> includes = new ArrayList<>();
        String query = "SELECT * FROM [Include]";
        try (PreparedStatement ps = getConnection().prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                includes.add(mapResult(rs));
            }
        } catch (SQLException e) {
            logger.error("Error retrieving all includes", e);
        }
        return includes;
    }

    public boolean createInclude(Include include) {
        String query = "INSERT INTO [Include] (Semester, CurriculumID, SubjectID) VALUES (?, ?, ?)";
        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setInt(1, include.getSemester());
            ps.setString(2, include.getCurriculumID().getCurriculumID());
            ps.setString(3, include.getSubjectID().getSubjectID());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error creating include", e);
        }
        return false;
    }

    public boolean deleteInclude(String curriculumId, String subjectId) {
        String query = "DELETE FROM [Include] WHERE CurriculumID = ? AND SubjectID = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setString(1, curriculumId);
            ps.setString(2, subjectId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error deleting include", e);
        }
        return false;
    }
}
