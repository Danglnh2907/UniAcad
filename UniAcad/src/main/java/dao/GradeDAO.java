package dao;

import model.database.Grade;
import model.database.Course;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.service.database.DBContext;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GradeDAO extends DBContext {
    private static final Logger logger = LoggerFactory.getLogger(GradeDAO.class);

    public GradeDAO() {
        super();
    }

    private Grade mapResult(ResultSet rs) throws SQLException {
        Grade grade = new Grade();
        grade.setId(rs.getInt("GradeID"));
        grade.setGradeName(rs.getString("GradeName"));
        grade.setGradePercent(rs.getInt("GradePercent"));
        Course course = new Course();
        course.setId(rs.getInt("CourseID"));
        grade.setCourseID(course);
        return grade;
    }

    public Grade getGradeById(int gradeId) {
        String query = "SELECT * FROM Grade WHERE GradeID = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setInt(1, gradeId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapResult(rs);
        } catch (SQLException e) {
            logger.error("Error retrieving grade by ID: {}", gradeId, e);
        }
        return null;
    }

    public List<Grade> getAllGrades() {
        List<Grade> grades = new ArrayList<>();
        String query = "SELECT * FROM Grade";
        try (PreparedStatement ps = getConnection().prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                grades.add(mapResult(rs));
            }
        } catch (SQLException e) {
            logger.error("Error retrieving all grades", e);
        }
        return grades;
    }

    public boolean createGrade(Grade grade) {
        String query = "INSERT INTO Grade (GradeName, CourseID, GradePercent) VALUES (?, ?, ?)";
        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setString(1, grade.getGradeName());
            ps.setInt(2, grade.getCourseID().getId());
            ps.setInt(3, grade.getGradePercent());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error creating grade", e);
        }
        return false;
    }

    public boolean deleteGrade(int gradeId) {
        String query = "DELETE FROM Grade WHERE GradeID = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setInt(1, gradeId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error deleting grade", e);
        }
        return false;
    }
}
