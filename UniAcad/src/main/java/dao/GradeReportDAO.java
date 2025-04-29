package dao;

import model.database.GradeReport;
import model.database.Student;
import model.database.Subject;
import model.database.Term;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.service.database.DBContext;

import java.sql.*;
import java.util.*;

public class GradeReportDAO extends DBContext {
    private static final Logger logger = LoggerFactory.getLogger(GradeReportDAO.class);

    public GradeReportDAO() {
        super();
    }

    private GradeReport mapResult(ResultSet rs) throws SQLException {
        GradeReport gradeReport = new GradeReport();
        Subject subject = new Subject();
        subject.setSubjectID(rs.getString("SubjectID"));
        gradeReport.setSubjectID(subject);

        Student student = new Student();
        student.setStudentID(rs.getString("StudentID"));
        gradeReport.setStudentID(student);

        gradeReport.setMark(rs.getBigDecimal("Mark"));
        gradeReport.setGradeReportStatus(rs.getObject("GradeReportStatus", Integer.class));

        String termId = rs.getString("TermID");
        if (termId != null) {
            Term term = new Term();
            term.setTermID(termId);
            gradeReport.setTermID(term);
        }

        return gradeReport;
    }

    public List<GradeReport> getAllGradeReports() {
        List<GradeReport> reports = new ArrayList<>();
        String query = "SELECT * FROM GradeReport";
        try (PreparedStatement ps = getConnection().prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                reports.add(mapResult(rs));
            }
        } catch (SQLException e) {
            logger.error("Error retrieving all grade reports", e);
        }
        return reports;
    }

    public boolean createGradeReport(GradeReport report) {
        String query = "INSERT INTO GradeReport (SubjectID, StudentID, TermID, Mark, GradeReportStatus) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setString(1, report.getSubjectID().getSubjectID());
            ps.setString(2, report.getStudentID().getStudentID());
            if (report.getTermID() != null) {
                ps.setString(3, report.getTermID().getTermID());
            } else {
                ps.setNull(3, Types.CHAR);
            }
            ps.setBigDecimal(4, report.getMark());
            ps.setObject(5, report.getGradeReportStatus());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error creating grade report", e);
        }
        return false;
    }

    public boolean calculateAndSaveAllReports() {
        String sql = """
            WITH CalculatedMarks AS (
                SELECT
                    st.StudentID,
                    su.SubjectID,
                    c.TermID,
                    ROUND(SUM(COALESCE(m.Mark, 0) * g.GradePercent / 100.0), 1) AS FinalMark
                FROM Student st
                JOIN Study s ON st.StudentID = s.StudentID
                JOIN Course c ON s.CourseID = c.CourseID
                JOIN [Subject] su ON c.SubjectID = su.SubjectID
                JOIN Grade g ON c.CourseID = g.CourseID
                LEFT JOIN Mark m ON g.GradeID = m.GradeID AND m.StudentID = st.StudentID
                GROUP BY st.StudentID, su.SubjectID, c.TermID
            )
            SELECT StudentID, SubjectID, TermID, FinalMark
            FROM CalculatedMarks
            WHERE FinalMark IS NOT NULL;
        """;

        String upsert = """
            MERGE INTO GradeReport AS target
            USING (SELECT ? AS SubjectID, ? AS StudentID) AS source
            ON target.SubjectID = source.SubjectID AND target.StudentID = source.StudentID
            WHEN MATCHED THEN
                UPDATE SET Mark = ?, TermID = ?, GradeReportStatus = ?
            WHEN NOT MATCHED THEN
                INSERT (SubjectID, StudentID, TermID, Mark, GradeReportStatus)
                VALUES (?, ?, ?, ?, ?);
        """;

        try (
                Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery();
                PreparedStatement up = conn.prepareStatement(upsert)
        ) {
            int count = 0;
            while (rs.next()) {
                String studentId = rs.getString("StudentID");
                String subjectId = rs.getString("SubjectID");
                String termId = rs.getString("TermID");
                double finalMark = rs.getDouble("FinalMark");
                int status = finalMark >= 5.0 ? 1 : 0;

                if (studentId == null || subjectId == null || termId == null) {
                    logger.warn("⚠️ Missing data: student={}, subject={}, term={}", studentId, subjectId, termId);
                    continue;
                }

                up.setString(1, subjectId);
                up.setString(2, studentId);
                up.setDouble(3, finalMark);
                up.setString(4, termId);
                up.setInt(5, status);
                up.setString(6, subjectId);
                up.setString(7, studentId);
                up.setString(8, termId);
                up.setDouble(9, finalMark);
                up.setInt(10, status);

                up.addBatch();
                count++;
            }
            up.executeBatch();
            logger.info("✅ Successfully updated {} rows in GradeReport", count);
            return true;

        } catch (SQLException e) {
            logger.error("❌ Error calculating and saving GradeReport", e);
        }
        return false;
    }

    public List<GradeReport> getGradeReportsByStudentAndTerm(String studentId, String termId) {
        List<GradeReport> reports = new ArrayList<>();
        String query = "SELECT * FROM GradeReport WHERE StudentID = ? AND TermID = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setString(1, studentId);
            ps.setString(2, termId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    reports.add(mapResult(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error retrieving grade reports for student {} and term {}", studentId, termId, e);
        }
        return reports;
    }

    public boolean deleteGradeReport(String subjectId, String studentId) {
        String query = "DELETE FROM GradeReport WHERE SubjectID = ? AND StudentID = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setString(1, subjectId);
            ps.setString(2, studentId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error deleting grade report for subject {} and student {}", subjectId, studentId, e);
        }
        return false;
    }

    public boolean updateGradeReport(GradeReport report) {
        String query = "UPDATE GradeReport SET Mark = ?, TermID = ?, GradeReportStatus = ? WHERE SubjectID = ? AND StudentID = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setBigDecimal(1, report.getMark());
            if (report.getTermID() != null) {
                ps.setString(2, report.getTermID().getTermID());
            } else {
                ps.setNull(2, Types.CHAR);
            }
            ps.setObject(3, report.getGradeReportStatus());
            ps.setString(4, report.getSubjectID().getSubjectID());
            ps.setString(5, report.getStudentID().getStudentID());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error updating grade report for subject {} and student {}",
                    report.getSubjectID().getSubjectID(), report.getStudentID().getStudentID(), e);
        }
        return false;
    }

    public static void main(String[] args) {
        GradeReportDAO dao = new GradeReportDAO();
        dao.calculateAndSaveAllReports();
    }
}
