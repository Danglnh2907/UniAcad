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
     * Trả về tất cả các cảnh báo học vụ (điểm, vắng mặt, học phí)
     */
    public List<WarningInfo> getWarnings() {
        List<WarningInfo> warnings = new ArrayList<>();

        warnings.addAll(getAcademicWarnings());
        warnings.addAll(getUnpaidFeeWarnings());

        logger.info("Tổng số cảnh báo: {}", warnings.size());
        return warnings;
    }

    /**
     * Lấy cảnh báo học lực, vắng học, cấm thi (JDBC)
     */
    private List<WarningInfo> getAcademicWarnings() {
        List<WarningInfo> list = new ArrayList<>();

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

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

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

        } catch (SQLException e) {
            logger.error("Lỗi khi lấy cảnh báo học lực bằng JDBC: {}", e.getMessage(), e);
        }

        return list;
    }

    /**
     * Lấy danh sách sinh viên còn nợ học phí (FeeStatus = 0)
     */
    private List<WarningInfo> getUnpaidFeeWarnings() {
        List<WarningInfo> list = new ArrayList<>();

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

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String studentId = rs.getString("StudentID");
                String studentName = rs.getString("StudentName");
                double totalUnpaid = rs.getDouble("TotalUnpaid");

                list.add(new WarningInfo(
                        studentId, studentName, totalUnpaid, "Unpaid Tuition"
                ));
            }

        } catch (SQLException e) {
            logger.error("Lỗi khi lấy cảnh báo học phí bằng JDBC: {}", e.getMessage(), e);
        }

        return list;
    }

    /**
     * Xác định loại cảnh báo dựa vào tỉ lệ vắng và điểm
     */
    private String determineWarning(double absentRate, double mark) {
        if (absentRate >= 0.2) return "Banned from Exam";
        if (absentRate >= 0.15) return "Absence Warning";
        if (mark < 5.0) return "Low Grade Warning";
        return null;
    }
}
