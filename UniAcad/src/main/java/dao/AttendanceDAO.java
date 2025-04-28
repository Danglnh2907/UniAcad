package dao;

import util.service.database.DBContext;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class AttendanceDAO extends DBContext {

    public AttendanceDAO() {
        super();
    }

    // Tạo mới điểm danh với trạng thái chưa điểm danh (NULL)
    public void createAttendanceRecord(String studentId, int courseId, int slotNumber) throws SQLException {
        String sql = "INSERT INTO Attendent (StudentID, CourseID, SlotNumber) VALUES (?, ?, ?)";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, studentId);
            ps.setInt(2, courseId);
            ps.setInt(3, slotNumber);
            ps.executeUpdate();
        }
    }

    // Cập nhật điểm danh: 1 = Có mặt, 0 = Vắng
    public void updateAttendanceStatus(String studentId, int courseId, int slotNumber, boolean isPresent) throws SQLException {
        String sql = "UPDATE Attendent SET Status = ? WHERE StudentID = ? AND CourseID = ? AND SlotNumber = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setBoolean(1, isPresent);
            ps.setString(2, studentId);
            ps.setInt(3, courseId);
            ps.setInt(4, slotNumber);
            ps.executeUpdate();
        }
    }
}
