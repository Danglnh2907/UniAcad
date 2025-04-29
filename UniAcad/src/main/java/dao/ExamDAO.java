package dao;

import model.database.Exam;
import model.database.Grade;
import model.database.Room;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.service.database.DBContext;

import java.sql.*;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class ExamDAO extends DBContext {
    private static final Logger logger = LoggerFactory.getLogger(ExamDAO.class);

    public ExamDAO() {
        super();
    }

    private Exam mapResult(ResultSet rs) throws SQLException {
        Exam exam = new Exam();
        exam.setId(rs.getInt("ExamID"));
        exam.setExamName(rs.getString("ExamName"));
        exam.setExamType(rs.getInt("ExamType"));

        Grade grade = new Grade();
        grade.setId(rs.getInt("GradeID"));
        exam.setGradeID(grade);

        Timestamp examDate = rs.getTimestamp("ExamDate");
        if (examDate != null) {
            exam.setExamDate(examDate.toInstant());
        }

        Time examDuration = rs.getTime("ExamDuration");
        if (examDuration != null) {
            exam.setExamDuration(LocalTime.parse(examDuration.toString()));
        }

        String roomId = rs.getString("RoomID");
        if (roomId != null) {
            Room room = new Room();
            room.setRoomID(roomId);
            exam.setRoomID(room);
        }

        return exam;
    }

    public List<Exam> getAllExams() {
        List<Exam> exams = new ArrayList<>();
        String query = "SELECT * FROM Exam";
        try (PreparedStatement ps = getConnection().prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                exams.add(mapResult(rs));
            }
        } catch (SQLException e) {
            logger.error("Error retrieving all exams", e);
        }
        return exams;
    }

    public boolean createExam(Exam exam) {
        String query = "INSERT INTO Exam (ExamName, GradeID, ExamDate, ExamDuration, RoomID, ExamType) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setString(1, exam.getExamName());
            ps.setInt(2, exam.getGradeID().getId());
            ps.setTimestamp(3, Timestamp.from(exam.getExamDate()));
            ps.setTime(4, Time.valueOf(exam.getExamDuration()));
            if (exam.getRoomID() != null) {
                ps.setString(5, exam.getRoomID().getRoomID());
            } else {
                ps.setNull(5, Types.CHAR);
            }
            ps.setInt(6, exam.getExamType());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error creating exam", e);
        }
        return false;
    }
}
