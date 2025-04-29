package dao;

import model.database.GradeReport;
import model.database.Student;
import model.database.Subject;
import model.database.Term;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.service.database.DBContext;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

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
}
