package dao;

import model.database.Course;
import model.database.Subject;
import model.database.Term;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.service.database.DBContext;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CourseDAO extends DBContext {
    private static final Logger logger = LoggerFactory.getLogger(CourseDAO.class);

    public CourseDAO() {
        super();
    }

    private Course mapResult(ResultSet rs) throws SQLException {
        Course course = new Course();
        course.setId(rs.getInt("CourseID"));
        course.setClassID(rs.getString("ClassID"));

        Subject subject = new Subject();
        subject.setSubjectID(rs.getString("SubjectID"));
        course.setSubjectID(subject);

        Term term = new Term();
        term.setTermID(rs.getString("TermID"));
        course.setTermID(term);

        course.setTotalSlot(rs.getInt("TotalSlot"));
        course.setCourseStatus(rs.getBoolean("CourseStatus"));
        return course;
    }

    public Course getCourseById(int courseId) {
        String query = "SELECT * FROM Course WHERE CourseID = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setInt(1, courseId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapResult(rs);
        } catch (SQLException e) {
            logger.error("Error retrieving course by ID: {}", courseId, e);
        }
        return null;
    }

    public List<Course> getAllCourses() {
        List<Course> courses = new ArrayList<>();
        String query = "SELECT * FROM Course";
        try (PreparedStatement ps = getConnection().prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                courses.add(mapResult(rs));
            }
        } catch (SQLException e) {
            logger.error("Error retrieving all courses", e);
        }
        return courses;
    }

    public boolean createCourse(Course course) {
        String query = "INSERT INTO Course (ClassID, SubjectID, TermID, TotalSlot, CourseStatus) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setString(1, course.getClassID());
            ps.setString(2, course.getSubjectID().getSubjectID());
            ps.setString(3, course.getTermID().getTermID());
            ps.setInt(4, course.getTotalSlot());
            ps.setBoolean(5, course.getCourseStatus());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error creating course", e);
        }
        return false;
    }

    public boolean updateCourse(Course course) {
        String query = "UPDATE Course SET ClassID = ?, SubjectID = ?, TermID = ?, TotalSlot = ?, CourseStatus = ? WHERE CourseID = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setString(1, course.getClassID());
            ps.setString(2, course.getSubjectID().getSubjectID());
            ps.setString(3, course.getTermID().getTermID());
            ps.setInt(4, course.getTotalSlot());
            ps.setBoolean(5, course.getCourseStatus());
            ps.setInt(6, course.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error updating course", e);
        }
        return false;
    }

    public boolean deleteCourse(int courseId) {
        String query = "DELETE FROM Course WHERE CourseID = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setInt(1, courseId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error deleting course", e);
        }
        return false;
    }
}
