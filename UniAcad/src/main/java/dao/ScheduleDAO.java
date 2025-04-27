package dao;

import model.datasupport.ScheduleItem;
import util.service.database.DBContext;

import java.sql.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class ScheduleDAO extends DBContext {

    public ScheduleDAO() {
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
                "AND s.StartTime >= ? AND s.StartTime < ? " +
                "AND c.CourseStatus = 0 " +
                "ORDER BY s.StartTime";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, studentId);
            ps.setDate(2, new java.sql.Date(startDate.getTime()));
            ps.setDate(3, new java.sql.Date(endDate.getTime()));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String subjectName = rs.getString("SubjectName");
                    String roomId = rs.getString("RoomID");
                    Timestamp startTime = rs.getTimestamp("StartTime");
                    Time duration = rs.getTime("Duration");

                    Boolean attendStatus = null;
                    boolean statusValue = rs.getBoolean("Status");
                    if (!rs.wasNull()) {
                        attendStatus = statusValue;
                    }

                    schedule.add(new ScheduleItem(subjectName, roomId, startTime, duration, attendStatus));
                }
            }
        }
        return schedule;
    }

    public static void main(String[] args) {
        try {
            ScheduleDAO scheduleDAO = new ScheduleDAO();

            // 🔥 Set lấy lịch 2 tuần: từ thứ 2 tuần này đến Chủ nhật tuần sau
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
            Date startOfWeek = new Date(cal.getTimeInMillis());

            cal.add(Calendar.DATE, 14); // +14 ngày
            Date endOfWeek = new Date(cal.getTimeInMillis());

            System.out.println("StartOfWeek = " + startOfWeek);
            System.out.println("EndOfWeek = " + endOfWeek);

            List<ScheduleItem> schedules = scheduleDAO.getWeeklySchedule("HE176001", startOfWeek, endOfWeek);

            System.out.println("Số lịch học tìm được: " + schedules.size());

            for (ScheduleItem item : schedules) {
                System.out.println(
                        "Môn: " + item.getSubjectName() +
                                ", Phòng: " + item.getRoomId() +
                                ", Bắt đầu: " + item.getStartTime() +
                                ", Thời lượng: " + item.getDuration() +
                                ", Điểm danh: " + (item.getAttendStatus() == null ? "Chưa" : (item.getAttendStatus() ? "Có mặt" : "Vắng"))
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
