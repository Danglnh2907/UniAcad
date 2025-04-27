package dao;

import model.datasupport.ScheduleItem;
import util.service.database.DBContext;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ScheduleDAO extends DBContext {

    public ScheduleDAO(Connection conn) {
        super();
    }

    public List<ScheduleItem> getWeeklySchedule(String studentId, Date startDate, Date endDate) throws SQLException {
        List<ScheduleItem> schedule = new ArrayList<>();
        String sql = "SELECT su.SubjectName, r.RoomID, s.StartTime, s.Duration, a.Status " +
                "FROM Slot s " +
                "JOIN Course c ON s.CourseID = c.CourseID " +
                "JOIN Subject su ON c.SubjectID = su.SubjectID " +
                "JOIN Study st ON c.CourseID = st.CourseID " +
                "JOIN [Group] g ON st.ClassID = g.ClassID " +
                "JOIN Class cl ON g.ClassID = cl.ClassID " +
                "JOIN Room r ON s.RoomID = r.RoomID " +
                "LEFT JOIN Attendent a ON a.StudentID = g.StudentID AND a.CourseID = s.CourseID AND a.SlotNumber = s.SlotNumber " +
                "WHERE g.StudentID = ? " +
                "AND s.StartTime BETWEEN ? AND ? " +
                "AND c.CourseStatus = 0 " +
                "ORDER BY s.StartTime";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, studentId);
            ps.setTimestamp(2, new Timestamp(startDate.getTime()));
            ps.setTimestamp(3, new Timestamp(endDate.getTime()));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String subjectName = rs.getString("SubjectName");
                    String roomId = rs.getString("RoomID");
                    Timestamp startTime = rs.getTimestamp("StartTime");
                    Time duration = rs.getTime("Duration");
                    Boolean attendStatus = (rs.getObject("Status") != null) ? rs.getBoolean("Status") : null;

                    schedule.add(new ScheduleItem(subjectName, roomId, startTime, duration, attendStatus));
                }
            }
        }
        return schedule;
    }
}
