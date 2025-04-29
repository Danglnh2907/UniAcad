package dao;

import model.database.Study;
import model.database.Student;
import model.database.Course;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.service.database.DBContext;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class StudyDAO extends DBContext {
    private static final Logger logger = LoggerFactory.getLogger(StudyDAO.class);

    public StudyDAO() {
        super();
    }

    private Study mapResult(ResultSet rs) throws SQLException {
        Study study = new Study();
        Student student = new Student();
        student.setStudentID(rs.getString("StudentID"));
        Course course = new Course();
        course.setId(rs.getInt("CourseID"));
        study.setStudentID(student);
        study.setCourseID(course);
        return study;
    }

    public List<Study> getAllStudies() {
        List<Study> studies = new ArrayList<>();
        String query = "SELECT * FROM Study";
        try (PreparedStatement ps = getConnection().prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                studies.add(mapResult(rs));
            }
        } catch (SQLException e) {
            logger.error("Error retrieving all studies", e);
        }
        return studies;
    }

    public boolean createStudy(Study study) {
        String query = "INSERT INTO Study (StudentID, CourseID) VALUES (?, ?)";
        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setString(1, study.getStudentID().getStudentID());
            ps.setInt(2, study.getCourseID().getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error creating study record", e);
        }
        return false;
    }

    public boolean deleteStudy(String studentId, int courseId) {
        String query = "DELETE FROM Study WHERE StudentID = ? AND CourseID = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setString(1, studentId);
            ps.setInt(2, courseId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error deleting study record", e);
        }
        return false;
    }
}
