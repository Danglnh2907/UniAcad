package dao;

import model.database.Mark;
import model.database.Grade;
import model.database.Student;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.service.database.DBContext;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MarkDAO extends DBContext {
    private static final Logger logger = LoggerFactory.getLogger(MarkDAO.class);

    public MarkDAO() {
        super();
    }

    private Mark mapResult(ResultSet rs) throws SQLException {
        Mark mark = new Mark();
        Grade grade = new Grade();
        grade.setId(rs.getInt("GradeID"));
        mark.setGradeID(grade);

        Student student = new Student();
        student.setStudentID(rs.getString("StudentID"));
        mark.setStudentID(student);

        mark.setMark(rs.getBigDecimal("Mark"));
        return mark;
    }

    public List<Mark> getAllMarks() {
        List<Mark> marks = new ArrayList<>();
        String query = "SELECT * FROM Mark";
        try (PreparedStatement ps = getConnection().prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                marks.add(mapResult(rs));
            }
        } catch (SQLException e) {
            logger.error("Error retrieving all marks", e);
        }
        return marks;
    }

    public boolean createMark(Mark mark) {
        String query = "INSERT INTO Mark (GradeID, StudentID, Mark) VALUES (?, ?, ?)";
        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setInt(1, mark.getGradeID().getId());
            ps.setString(2, mark.getStudentID().getStudentID());
            ps.setBigDecimal(3, mark.getMark());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error creating mark", e);
        }
        return false;
    }

    public boolean deleteMark(int gradeId, String studentId) {
        String query = "DELETE FROM Mark WHERE GradeID = ? AND StudentID = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setInt(1, gradeId);
            ps.setString(2, studentId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error deleting mark", e);
        }
        return false;
    }
}
