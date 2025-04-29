package dao;

import model.database.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.service.database.DBContext;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class SlotDAO extends DBContext {
    private static final Logger logger = LoggerFactory.getLogger(SlotDAO.class);

    public SlotDAO() {
        super();
    }

    private Slot resultMap(ResultSet resultSet) throws SQLException {
        Slot slot = new Slot();

        SlotId slotId = new SlotId();
        slotId.setSlotNumber(resultSet.getInt("SlotNumber"));
        slotId.setCourseID(resultSet.getInt("CourseID"));
        slot.setId(slotId);

        slot.setStartTime(resultSet.getTimestamp("StartTime").toInstant());

        Time duration = resultSet.getTime("Duration");
        if (duration != null) {
            slot.setDuration(LocalTime.parse(duration.toString()));
        }

        Course course = new Course();
        course.setId(resultSet.getInt("CourseID"));
        slot.setCourseID(course);

        String teacherId = resultSet.getString("TeacherID");
        if (teacherId != null) {
            Teacher teacher = new Teacher();
            teacher.setTeacherID(teacherId);
            slot.setTeacherID(teacher);
        }

        String roomId = resultSet.getString("RoomID");
        if (roomId != null) {
            Room room = new Room();
            room.setRoomID(roomId);
            slot.setRoomID(room);
        }

        return slot;
    }

    public List<Slot> getSlotsByCourse(int courseId) {
        List<Slot> slots = new ArrayList<>();
        String query = "SELECT * FROM Slot WHERE CourseID = ?";
        try (PreparedStatement statement = getConnection().prepareStatement(query)) {
            statement.setInt(1, courseId);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                slots.add(resultMap(resultSet));
            }
            logger.debug("Retrieved {} slots for CourseID: {}", slots.size(), courseId);
        } catch (SQLException e) {
            logger.error("Error retrieving slots for CourseID: {}", courseId, e);
        }
        return slots;
    }

    public List<Slot> getStudentWeeklyTimetable(String studentId, String startDate, String endDate) {
        List<Slot> slots = new ArrayList<>();
        String query = """
            SELECT s.SlotNumber, s.StartTime, s.Duration, s.CourseID, s.RoomID, s.TeacherID
            FROM Slot s
            JOIN Course c ON s.CourseID = c.CourseID
            JOIN Study st ON c.CourseID = st.CourseID
            WHERE st.StudentID = ?
            AND s.StartTime BETWEEN ? AND ?
            AND c.CourseStatus = 0
            ORDER BY s.StartTime
            """;

        try (PreparedStatement statement = getConnection().prepareStatement(query)) {
            statement.setString(1, studentId);
            statement.setString(2, startDate);
            statement.setString(3, endDate);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                Slot slot = resultMap(resultSet);
                slots.add(slot);
            }
            logger.debug("Retrieved {} slots for StudentID: {}, from {} to {}",
                    slots.size(), studentId, startDate, endDate);
        } catch (SQLException e) {
            logger.error("Error retrieving weekly timetable for StudentID: {}, from {} to {}",
                    studentId, startDate, endDate, e);
        }
        return slots;
    }

    public List<String> checkTimetableConflicts(String studentId, String startDate, String endDate) {
        List<Slot> slots = getStudentWeeklyTimetable(studentId, startDate, endDate);
        List<String> conflicts = new ArrayList<>();
        for (int i = 0; i < slots.size(); i++) {
            for (int j = i + 1; j < slots.size(); j++) {
                Slot slot1 = slots.get(i);
                Slot slot2 = slots.get(j);
                if (isOverlapping(slot1.getStartTime(), slot1.getDuration(),
                        slot2.getStartTime(), slot2.getDuration())) {
                    conflicts.add(String.format("Conflict between Slot %d (Course %d) and Slot %d (Course %d)",
                            slot1.getId().getSlotNumber(), slot1.getId().getCourseID(),
                            slot2.getId().getSlotNumber(), slot2.getId().getCourseID()));
                }
            }
        }
        logger.debug("Found {} timetable conflicts for StudentID: {}, from {} to {}",
                conflicts.size(), studentId, startDate, endDate);
        return conflicts;
    }

    private boolean isOverlapping(Instant start1, LocalTime duration1,
                                  Instant start2, LocalTime duration2) {
        LocalDateTime startTime1 = LocalDateTime.ofInstant(start1, java.time.ZoneId.systemDefault());
        LocalDateTime endTime1 = startTime1.plusHours(duration1.getHour()).plusMinutes(duration1.getMinute());

        LocalDateTime startTime2 = LocalDateTime.ofInstant(start2, java.time.ZoneId.systemDefault());
        LocalDateTime endTime2 = startTime2.plusHours(duration2.getHour()).plusMinutes(duration2.getMinute());

        return startTime1.isBefore(endTime2) && startTime2.isBefore(endTime1);
    }
}
