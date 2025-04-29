package dao;
import model.datasupport.MarkReport;
import util.service.database.DBContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Logger;

public class MarkReportDAO extends DBContext {
    private static final Logger logger = Logger.getLogger(MarkReportDAO.class.getName());

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

    public List<MarkReport> getMarkReportsByTermId(String studentId, String termId) {
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
            INNER JOIN GradeReport gr ON c.SubjectID = gr.SubjectID AND c.TermID = gr.TermID
            INNER JOIN Mark m ON g.GradeID = m.GradeID
            INNER JOIN Student st ON gr.StudentID = st.StudentID AND m.StudentID = st.StudentID
            INNER JOIN Subject su ON c.SubjectID = su.SubjectID
            INNER JOIN Term t ON c.TermID = t.TermID
            WHERE st.StudentID = ? AND t.TermID = ?
        """;

        Map<String, MarkReport> reportMap = new LinkedHashMap<>();

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, studentId);
            ps.setString(2, termId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String subjectId = rs.getString("SubjectID");
                String subjectName = rs.getString("SubjectName");
                double averageMark = rs.getDouble("AverageMark");
                String gradeName = rs.getString("GradeName");
                double gradeMark = rs.getDouble("GradeMark");

                MarkReport report = reportMap.computeIfAbsent(subjectId, sid ->
                        new MarkReport(studentId, subjectId, subjectName, new HashMap<>(), averageMark)
                );

                if (gradeName != null) {
                    report.getMarks().put(gradeName, gradeMark);
                }
            }

        } catch (SQLException e) {
            logger.severe("❌ Error fetching mark reports by studentId=" + studentId + ", termId=" + termId + ": " + e.getMessage());
        }

        return new ArrayList<>(reportMap.values());
    }
    public static void main(String[] args) {
        MarkReportDAO dao = new MarkReportDAO();
        var report = dao.getMarkReportsByTermId("CE182286", "2310");

        if (report != null) {
            for (MarkReport r : report) {
                System.out.println("📘 Subject: " + r.getSubjectName());
                System.out.println("🎓 Average: " + r.getAverageMark());
                r.getMarks().forEach((k, v) -> System.out.println(" - " + k + ": " + v));
            }
        } else {
            System.out.println("❌ No report found.");
        }
    }

}
