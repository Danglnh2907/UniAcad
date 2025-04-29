package dao;

import model.datasupport.MarkReport;
import util.service.database.DBContext;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MarkReportDAO extends DBContext {

    public MarkReportDAO() {
        super();
    }

    /**
     * Lấy bảng điểm tất cả sinh viên.
     */
    public List<MarkReport> getMarkReports() {
        List<MarkReport> reports = new ArrayList<>();
        String sql = """
            SELECT
                st.StudentID,
                st.StudentName,
                su.SubjectName,
                ISNULL(gr.Mark, 0) AS Mark
            FROM Student st
            JOIN Study st2 ON st.StudentID = st2.StudentID
            JOIN Course c ON st2.CourseID = c.CourseID
            JOIN [Subject] su ON c.SubjectID = su.SubjectID
            LEFT JOIN GradeReport gr ON gr.StudentID = st.StudentID AND gr.SubjectID = su.SubjectID
            """;

        try (PreparedStatement ps = getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                reports.add(mapResultSetToMarkReport(rs));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return reports;
    }

    /**
     * Lấy bảng điểm của sinh viên theo Email.
     */
    public List<MarkReport> getMarkReportsByEmail(String email) {
        List<MarkReport> reports = new ArrayList<>();
        String sql = """
            SELECT
                st.StudentID,
                st.StudentName,
                su.SubjectName,
                ISNULL(gr.Mark, 0) AS Mark
            FROM Student st
            JOIN Study st2 ON st.StudentID = st2.StudentID
            JOIN Course c ON st2.CourseID = c.CourseID
            JOIN [Subject] su ON c.SubjectID = su.SubjectID
            LEFT JOIN GradeReport gr ON gr.StudentID = st.StudentID AND gr.SubjectID = su.SubjectID
            WHERE st.StudentEmail = ?
            """;

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                reports.add(mapResultSetToMarkReport(rs));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return reports;
    }

    /**
     * Lấy bảng điểm theo StudentID.
     */
    public MarkReport getMarkReportByStudentId(String studentId) {
        String sql = """
            SELECT
                st.StudentID,
                st.StudentName,
                su.SubjectName,
                ISNULL(gr.Mark, 0) AS Mark
            FROM Student st
            JOIN Study st2 ON st.StudentID = st2.StudentID
            JOIN Course c ON st2.CourseID = c.CourseID
            JOIN [Subject] su ON c.SubjectID = su.SubjectID
            LEFT JOIN GradeReport gr ON gr.StudentID = st.StudentID AND gr.SubjectID = su.SubjectID
            WHERE st.StudentID = ?
            """;

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, studentId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapResultSetToMarkReport(rs);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private MarkReport mapResultSetToMarkReport(ResultSet rs) throws SQLException {
        String studentId = rs.getString("StudentID");
        String studentName = rs.getString("StudentName");
        String subjectName = rs.getString("SubjectName");
        double mark = rs.getDouble("Mark");
        String status = mark >= 5.0 ? "Pass" : "Fail";
        return new MarkReport(studentId, studentName, subjectName, mark, status);
    }
}
