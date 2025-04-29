package dao;

import model.database.Subject;
import model.database.Department;
import model.datasupport.MarkReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.service.database.DBContext;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SubjectDAO extends DBContext {
    private static final Logger logger = LoggerFactory.getLogger(SubjectDAO.class);

    public SubjectDAO() {
        super();
    }

    private Subject mapResult(ResultSet rs) throws SQLException {
        Subject subject = new Subject();
        subject.setSubjectID(rs.getString("SubjectID"));
        subject.setSubjectName(rs.getString("SubjectName"));
        subject.setCredits(rs.getInt("Credits"));
        subject.setIsDeleted(rs.getBoolean("IsDeleted"));

        String departmentId = rs.getString("DepartmentID");
        if (departmentId != null) {
            Department department = new Department();
            department.setDepartmentID(departmentId);
            subject.setDepartmentID(department);
        }

        return subject;
    }

    public Subject getSubjectById(String subjectId) {
        String query = "SELECT * FROM [Subject] WHERE SubjectID = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setString(1, subjectId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResult(rs);
                }
            }
        } catch (SQLException e) {
            logger.error("Error retrieving subject by ID: {}", subjectId, e);
        }
        return null;
    }

    public List<Subject> getAllSubjects() {
        List<Subject> subjects = new ArrayList<>();
        String query = "SELECT * FROM [Subject]";
        try (PreparedStatement ps = getConnection().prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                subjects.add(mapResult(rs));
            }
        } catch (SQLException e) {
            logger.error("Error retrieving all subjects", e);
        }
        return subjects;
    }

    public boolean createSubject(Subject subject) {
        String query = "INSERT INTO [Subject] (SubjectID, SubjectName, Credits, DepartmentID, IsDeleted) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setString(1, subject.getSubjectID());
            ps.setString(2, subject.getSubjectName());
            ps.setInt(3, subject.getCredits());
            if (subject.getDepartmentID() != null) {
                ps.setString(4, subject.getDepartmentID().getDepartmentID());
            } else {
                ps.setNull(4, Types.CHAR);
            }
            ps.setBoolean(5, subject.getIsDeleted());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error creating subject: {}", subject.getSubjectID(), e);
        }
        return false;
    }

    public boolean updateSubject(Subject subject) {
        String query = "UPDATE [Subject] SET SubjectName = ?, Credits = ?, DepartmentID = ?, IsDeleted = ? WHERE SubjectID = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setString(1, subject.getSubjectName());
            ps.setInt(2, subject.getCredits());
            if (subject.getDepartmentID() != null) {
                ps.setString(3, subject.getDepartmentID().getDepartmentID());
            } else {
                ps.setNull(3, Types.CHAR);
            }
            ps.setBoolean(4, subject.getIsDeleted());
            ps.setString(5, subject.getSubjectID());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error updating subject: {}", subject.getSubjectID(), e);
        }
        return false;
    }

    public boolean deleteSubject(String subjectId) {
        String query = "DELETE FROM [Subject] WHERE SubjectID = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setString(1, subjectId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error deleting subject: {}", subjectId, e);
        }
        return false;
    }

    public static class MarkReportDAO extends DBContext {
        private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(MarkReportDAO.class.getName());

        public MarkReportDAO() {
            super();
        }

        public MarkReport getMarkReport(String studentId, String subjectId) {
            String sql = """
                SELECT 
                    st.StudentID, 
                    su.SubjectID, 
                    su.SubjectName, 
                    gr.Mark AS AverageMark, 
                    g.GradeName, 
                    m.Mark AS GradeMark
                FROM Course c
                INNER JOIN Grade g ON c.CourseID = g.CourseID
                INNER JOIN GradeReport gr ON c.SubjectID = gr.SubjectID
                INNER JOIN Mark m ON g.GradeID = m.GradeID
                INNER JOIN Student st ON gr.StudentID = st.StudentID AND m.StudentID = st.StudentID
                INNER JOIN Subject su ON c.SubjectID = su.SubjectID AND gr.SubjectID = su.SubjectID
                INNER JOIN Term t ON c.TermID = t.TermID AND gr.TermID = t.TermID
                WHERE st.StudentID = ? AND su.SubjectID = ?
            """;

            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, studentId);
                ps.setString(2, subjectId);

                ResultSet rs = ps.executeQuery();
                Map<String, Double> marks = new HashMap<>();
                String subjectName = null;
                double averageMark = 0.0;
                boolean found = false;

                while (rs.next()) {
                    found = true;
                    subjectName = rs.getString("SubjectName");
                    averageMark = rs.getDouble("AverageMark");  // From GradeReport.Mark
                    String gradeName = rs.getString("GradeName");
                    double gradeMark = rs.getDouble("GradeMark");

                    if (gradeName != null) {
                        marks.put(gradeName, gradeMark);
                    }
                }

                if (found) {
                    return new MarkReport(studentId, subjectId, subjectName, marks, averageMark);
                }

            } catch (SQLException e) {
                logger.severe("❌ Error fetching mark report for studentId=" + studentId + ", subjectId=" + subjectId + ": " + e.getMessage());
            }

            return null;
        }
        public static void main(String[] args) {
            MarkReportDAO dao = new MarkReportDAO();
            MarkReport report = dao.getMarkReport("ST0001", "SUBJ101");

            if (report != null) {
                System.out.println("📘 Subject: " + report.getSubjectName());
                System.out.println("🎓 Average: " + report.getAverageMark());
                report.getMarks().forEach((k, v) -> System.out.println(" - " + k + ": " + v));
            } else {
                System.out.println("❌ No report found.");
            }
        }

    }
}
