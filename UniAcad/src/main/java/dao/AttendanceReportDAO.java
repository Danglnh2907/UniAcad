package dao;

import model.datasupport.AttendanceReport;
import util.service.database.DBContext;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AttendanceReportDAO extends DBContext {

    public AttendanceReportDAO() {
        super();
    }

    /**
     * Lấy báo cáo điểm danh theo tất cả sinh viên và môn.
     */
    public List<AttendanceReport> getAttendanceReports() {
        List<AttendanceReport> reports = new ArrayList<>();
        String sql = """
            SELECT
                st.StudentID,
                st.StudentName,
                su.SubjectName,
                COUNT(s.SlotNumber) AS TotalSlots,
                SUM(CASE WHEN a.Status = 0 THEN 1 ELSE 0 END) AS AbsentSlots
            FROM Student st
            JOIN [Group] g ON st.StudentID = g.StudentID
            JOIN Study st2 ON g.ClassID = st2.ClassID
            JOIN Course c ON st2.CourseID = c.CourseID
            JOIN [Subject] su ON c.SubjectID = su.SubjectID
            JOIN Slot s ON c.CourseID = s.CourseID
            LEFT JOIN Attendent a ON a.StudentID = st.StudentID 
                                   AND a.CourseID = c.CourseID 
                                   AND a.SlotNumber = s.SlotNumber
            GROUP BY st.StudentID, st.StudentName, su.SubjectName
            """;

        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String studentId = rs.getString("StudentID");
                String studentName = rs.getString("StudentName");
                String subjectName = rs.getString("SubjectName");
                int totalSlots = rs.getInt("TotalSlots");
                int absentSlots = rs.getInt("AbsentSlots");

                double absentRate = (totalSlots == 0) ? 0.0 : (double) absentSlots / totalSlots;

                reports.add(new AttendanceReport(studentId, studentName, subjectName, totalSlots, absentSlots, absentRate));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return reports;
    }

    /**
     * Lấy báo cáo điểm danh theo Email sinh viên
     */
    public List<AttendanceReport> getAttendanceReportsByEmail(String email) {
        List<AttendanceReport> reports = new ArrayList<>();
        String sql = """
            SELECT
                st.StudentID,
                st.StudentName,
                su.SubjectName,
                COUNT(s.SlotNumber) AS TotalSlots,
                SUM(CASE WHEN a.Status = 0 THEN 1 ELSE 0 END) AS AbsentSlots
            FROM Student st
            JOIN [Group] g ON st.StudentID = g.StudentID
            JOIN Study st2 ON g.ClassID = st2.ClassID
            JOIN Course c ON st2.CourseID = c.CourseID
            JOIN [Subject] su ON c.SubjectID = su.SubjectID
            JOIN Slot s ON c.CourseID = s.CourseID
            LEFT JOIN Attendent a ON a.StudentID = st.StudentID 
                                   AND a.CourseID = c.CourseID 
                                   AND a.SlotNumber = s.SlotNumber
            WHERE st.StudentEmail = ?
            GROUP BY st.StudentID, st.StudentName, su.SubjectName
            """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String studentId = rs.getString("StudentID");
                    String studentName = rs.getString("StudentName");
                    String subjectName = rs.getString("SubjectName");
                    int totalSlots = rs.getInt("TotalSlots");
                    int absentSlots = rs.getInt("AbsentSlots");
                    double absentRate = (totalSlots == 0) ? 0.0 : (double) absentSlots / totalSlots;
                    reports.add(new AttendanceReport(studentId, studentName, subjectName, totalSlots, absentSlots, absentRate));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return reports;
    }
}
