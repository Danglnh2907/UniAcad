package dao;

import model.database.Attendent;
import model.database.AttendentId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.service.database.DBContext;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AttendantDAO extends DBContext {
    private static final Logger logger = LoggerFactory.getLogger(AttendantDAO.class);

    public AttendantDAO() {
        super();
    }

    /**
     * Maps a ResultSet to an Attendant object.
     */
    private Attendent resultMap(ResultSet resultSet) throws SQLException {
        Attendent attendant = new Attendent();
        attendant.setId(new AttendentId(
                resultSet.getString("StudentID"),
                resultSet.getInt("CourseID"),
                resultSet.getInt("SlotNumber")
        ));
        Boolean status = resultSet.getObject("Status") != null ? resultSet.getBoolean("Status") : null;
        attendant.setStatus(status);
        return attendant;
    }

    /**
     * Retrieves attendance records for a specific course and slot.
     */
    public List<Attendent> getAttendanceByCourseAndSlot(int courseId, int slotNumber) {
        List<Attendent> attendants = new ArrayList<>();
        String query = "SELECT * FROM Attendant WHERE CourseID = ? AND SlotNumber = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, courseId);
            statement.setInt(2, slotNumber);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                attendants.add(resultMap(resultSet));
            }
            logger.debug("Retrieved {} attendance records for CourseID: {}, SlotNumber: {}",
                    attendants.size(), courseId, slotNumber);
        } catch (SQLException e) {
            logger.error("Error retrieving attendance for CourseID: {}, SlotNumber: {}",
                    courseId, slotNumber, e);
        }
        return attendants;
    }

    /**
     * Updates or inserts attendance status for a student in a slot.
     */
    public boolean updateAttendance(String studentId, int courseId, int slotNumber, Boolean status) {
        String query = "MERGE INTO Attendant AS target " +
                "USING (SELECT ? AS StudentID, ? AS CourseID, ? AS SlotNumber, ? AS Status) AS source " +
                "ON target.StudentID = source.StudentID AND target.CourseID = source.CourseID AND target.SlotNumber = source.SlotNumber " +
                "WHEN MATCHED THEN " +
                "    UPDATE SET Status = source.Status " +
                "WHEN NOT MATCHED THEN " +
                "    INSERT (StudentID, CourseID, SlotNumber, Status) " +
                "    VALUES (source.StudentID, source.CourseID, source.SlotNumber, source.Status);";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, studentId);
            statement.setInt(2, courseId);
            statement.setInt(3, slotNumber);
            if (status == null) {
                statement.setNull(4, java.sql.Types.BIT);
            } else {
                statement.setBoolean(4, status);
            }
            int rowsAffected = statement.executeUpdate();
            logger.debug("Updated attendance for StudentID: {}, CourseID: {}, SlotNumber: {}, Status: {}, Rows affected: {}",
                    studentId, courseId, slotNumber, status, rowsAffected);
            return rowsAffected > 0;
        } catch (SQLException e) {
            logger.error("Error updating attendance for StudentID: {}, CourseID: {}, SlotNumber: {}",
                    studentId, courseId, slotNumber, e);
            return false;
        }
    }

    /**
     * Batch updates attendance for multiple students in a slot.
     */
    public boolean batchUpdateAttendance(List<Attendent> attendants) {
        String query = "MERGE INTO Attendant AS target " +
                "USING (SELECT ? AS StudentID, ? AS CourseID, ? AS SlotNumber, ? AS Status) AS source " +
                "ON target.StudentID = source.StudentID AND target.CourseID = source.CourseID AND target.SlotNumber = source.SlotNumber " +
                "WHEN MATCHED THEN " +
                "    UPDATE SET Status = source.Status " +
                "WHEN NOT MATCHED THEN " +
                "    INSERT (StudentID, CourseID, SlotNumber, Status) " +
                "    VALUES (source.StudentID, source.CourseID, source.SlotNumber, source.Status);";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            for (Attendent attendant : attendants) {
                statement.setString(1, attendant.getId().getStudentID());
                statement.setInt(2, attendant.getId().getCourseID());
                statement.setInt(3, attendant.getId().getSlotNumber());
                if (attendant.getStatus() == null) {
                    statement.setNull(4, java.sql.Types.BIT);
                } else {
                    statement.setBoolean(4, attendant.getStatus());
                }
                statement.addBatch();
            }
            int[] results = statement.executeBatch();
            int totalRowsAffected = 0;
            for (int result : results) {
                totalRowsAffected += result;
            }
            logger.debug("Batch updated {} attendance records", totalRowsAffected);
            return totalRowsAffected > 0;
        } catch (SQLException e) {
            logger.error("Error batch updating attendance", e);
            return false;
        }
    }

    /**
     * Retrieves attendance records for a student in a specific course.
     */
    public List<Attendent> getStudentAttendance(String studentId, int courseId) {
        List<Attendent> attendants = new ArrayList<>();
        String query = "SELECT * FROM Attendant WHERE StudentID = ? AND CourseID = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, studentId);
            statement.setInt(2, courseId);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                attendants.add(resultMap(resultSet));
            }
            logger.debug("Retrieved {} attendance records for StudentID: {}, CourseID: {}",
                    attendants.size(), studentId, courseId);
        } catch (SQLException e) {
            logger.error("Error retrieving attendance for StudentID: {}, CourseID: {}",
                    studentId, courseId, e);
        }
        return attendants;
    }

    /**
     * Calculates attendance statistics for a course and slot.
     * Returns a map with counts of present, absent, and not yet.
     */
    public Map<String, Integer> getAttendanceStatistics(int courseId, int slotNumber) {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("present", 0);
        stats.put("absent", 0);
        stats.put("notYet", 0);

        String query = "SELECT Status, COUNT(*) AS count FROM Attendant " +
                "WHERE CourseID = ? AND SlotNumber = ? " +
                "GROUP BY Status";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, courseId);
            statement.setInt(2, slotNumber);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                Boolean status = resultSet.getObject("Status") != null ? resultSet.getBoolean("Status") : null;
                int count = resultSet.getInt("count");
                if (status == null) {
                    stats.put("notYet", count);
                } else if (status) {
                    stats.put("present", count);
                } else {
                    stats.put("absent", count);
                }
            }
            logger.debug("Attendance statistics for CourseID: {}, SlotNumber: {}: {}",
                    courseId, slotNumber, stats);
        } catch (SQLException e) {
            logger.error("Error calculating attendance statistics for CourseID: {}, SlotNumber: {}",
                    courseId, slotNumber, e);
        }
        return stats;
    }

    /**
     * Checks for duplicate attendance records.
     */
    public boolean hasDuplicateAttendance(String studentId, int courseId, int slotNumber) {
        String query = "SELECT COUNT(*) FROM Attendant WHERE StudentID = ? AND CourseID = ? AND SlotNumber = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, studentId);
            statement.setInt(2, courseId);
            statement.setInt(3, slotNumber);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                int count = resultSet.getInt(1);
                if (count > 1) {
                    logger.warn("Duplicate attendance found for StudentID: {}, CourseID: {}, SlotNumber: {}",
                            studentId, courseId, slotNumber);
                    return true;
                }
            }
        } catch (SQLException e) {
            logger.error("Error checking duplicate attendance for StudentID: {}, CourseID: {}, SlotNumber: {}",
                    studentId, courseId, slotNumber, e);
        }
        return false;
    }
}