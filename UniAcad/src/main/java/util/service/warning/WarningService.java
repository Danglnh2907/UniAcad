package util.service.warning;

import model.datasupport.WarningInfo;
import util.service.database.DBContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class WarningService extends DBContext {
    private static final Logger logger = LoggerFactory.getLogger(WarningService.class);

    public WarningService() {
        super();
    }

    /**
     * Lấy toàn bộ cảnh báo cho mọi sinh viên
     */
    public List<WarningInfo> getWarnings() {
        List<WarningInfo> warnings = new ArrayList<>();
        warnings.addAll(getAcademicWarnings());
        warnings.addAll(getUnpaidFeeWarnings());
        logger.info("Tổng số cảnh báo: {}", warnings.size());
        return warnings;
    }

    /**
     * Lấy cảnh báo học vụ + học phí theo email sinh viên
     */
    public List<WarningInfo> getWarningsByEmail(String email) {
        List<WarningInfo> warnings = new ArrayList<>();
        warnings.addAll(getAcademicWarnings(email));
        warnings.addAll(getUnpaidFeeWarnings(email));
        logger.info("Cảnh báo cho {}: {}", email, warnings.size());
        return warnings;
    }

    private List<WarningInfo> getAcademicWarnings() {
        String sql = """
            SELECT
                st.StudentID,
                st.StudentName,
                su.SubjectName,
                SUM(CASE WHEN a.Status = 0 THEN 1 ELSE 0 END) AS AbsentCount,
                COUNT(s.SlotNumber) AS TotalSlots,
                ISNULL(gr.Mark, 0) AS Mark
            FROM Student st
            JOIN Study st2 ON st.StudentID = st2.StudentID
            JOIN Course c ON st2.CourseID = c.CourseID
            JOIN [Subject] su ON c.SubjectID = su.SubjectID
            JOIN Slot s ON c.CourseID = s.CourseID
            LEFT JOIN Attendent a ON a.StudentID = st.StudentID 
                                  AND a.CourseID = c.CourseID 
                                  AND a.SlotNumber = s.SlotNumber
            LEFT JOIN GradeReport gr ON gr.StudentID = st.StudentID 
                                     AND gr.SubjectID = su.SubjectID
            GROUP BY st.StudentID, st.StudentName, su.SubjectName, gr.Mark
        """;

        return fetchAcademicWarnings(sql, null);
    }

    // ------------------- Học vụ theo email -----------------------
    private List<WarningInfo> getAcademicWarnings(String email) {
        String sql = """
            SELECT
                st.StudentID,
                st.StudentName,
                su.SubjectName,
                SUM(CASE WHEN a.Status = 0 THEN 1 ELSE 0 END) AS AbsentCount,
                COUNT(s.SlotNumber) AS TotalSlots,
                ISNULL(gr.Mark, 0) AS Mark
            FROM Student st
            JOIN Study st2 ON st.StudentID = st2.StudentID
            JOIN Course c ON st2.CourseID = c.CourseID
            JOIN [Subject] su ON c.SubjectID = su.SubjectID
            JOIN Slot s ON c.CourseID = s.CourseID
            LEFT JOIN Attendent a ON a.StudentID = st.StudentID 
                                  AND a.CourseID = c.CourseID 
                                  AND a.SlotNumber = s.SlotNumber
            LEFT JOIN GradeReport gr ON gr.StudentID = st.StudentID 
                                     AND gr.SubjectID = su.SubjectID
            WHERE st.StudentEmail = ?
            GROUP BY st.StudentID, st.StudentName, su.SubjectName, gr.Mark
        """;

        return fetchAcademicWarnings(sql, email);
    }

    private List<WarningInfo> getUnpaidFeeWarnings() {
        String sql = """
            SELECT
                f.StudentID,
                st.StudentName,
                SUM(f.Amount) AS TotalUnpaid
            FROM Fee f
            JOIN Student st ON f.StudentID = st.StudentID
            WHERE f.FeeStatus = 0
            GROUP BY f.StudentID, st.StudentName
        """;

        return fetchUnpaidWarnings(sql, null);
    }

    private List<WarningInfo> getUnpaidFeeWarnings(String email) {
        String sql = """
            SELECT
                f.StudentID,
                st.StudentName,
                SUM(f.Amount) AS TotalUnpaid
            FROM Fee f
            JOIN Student st ON f.StudentID = st.StudentID
            WHERE f.FeeStatus = 0 AND st.StudentEmail = ?
            GROUP BY f.StudentID, st.StudentName
        """;

        return fetchUnpaidWarnings(sql, email);
    }

    private List<WarningInfo> fetchAcademicWarnings(String sql, String email) {
        List<WarningInfo> list = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (email != null) ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String studentId = rs.getString("StudentID");
                    String studentName = rs.getString("StudentName");
                    String subjectName = rs.getString("SubjectName");
                    int absentCount = rs.getInt("AbsentCount");
                    int totalSlots = rs.getInt("TotalSlots");
                    double mark = rs.getDouble("Mark");

                    double absentRate = totalSlots == 0 ? 0 : (double) absentCount / totalSlots;
                    String warningType = determineWarning(absentRate, mark);

                    if (warningType != null) {
                        list.add(new WarningInfo(
                                studentId, studentName, subjectName,
                                absentCount, totalSlots, absentRate, mark, warningType
                        ));
                    }
                }
            }

        } catch (SQLException e) {
            logger.error("Lỗi học vụ: {}", e.getMessage(), e);
        }

        return list;
    }

    private List<WarningInfo> fetchUnpaidWarnings(String sql, String email) {
        List<WarningInfo> list = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (email != null) ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String studentId = rs.getString("StudentID");
                    String studentName = rs.getString("StudentName");
                    double totalUnpaid = rs.getDouble("TotalUnpaid");

                    WarningInfo warning = new WarningInfo();
                    warning.setStudentId(studentId);
                    warning.setStudentName(studentName);
                    warning.setFeeMoney(totalUnpaid);
                    warning.setWarningType("Unpaid Tuition");

                    list.add(warning);
                }
            }

        } catch (SQLException e) {
            logger.error("Lỗi học phí: {}", e.getMessage(), e);
        }

        return list;
    }

    private String determineWarning(double absentRate, double mark) {
        if (absentRate >= 0.2) return "Banned from Exam";
        if (absentRate >= 0.15) return "Absence Warning";
        if (mark < 5.0) return "Low Grade Warning";
        return null;
    }
}
