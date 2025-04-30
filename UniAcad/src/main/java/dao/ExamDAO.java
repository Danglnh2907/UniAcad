package dao;

import model.database.Exam;
import model.database.Grade;
import model.database.Room;
import model.datasupport.ExamSchedule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.service.database.DBContext;

import java.sql.*;
import java.time.Duration;
import java.time.LocalDateTime;
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

    public boolean createExamForSubject(String subjectId, String examName, Timestamp examDate,
                                        Time examDuration, String roomId, int examType) {
        String findGradesQuery = """
        SELECT g.GradeID
        FROM Grade g
        JOIN Course c ON g.CourseID = c.CourseID
        WHERE c.SubjectID = ?
    """;

        String insertExamQuery = """
        INSERT INTO Exam (ExamName, GradeID, ExamDate, ExamDuration, RoomID, ExamType)
        VALUES (?, ?, ?, ?, ?, ?)
    """;

        try (Connection conn = getConnection();
             PreparedStatement findGrades = conn.prepareStatement(findGradesQuery);
             PreparedStatement insertExam = conn.prepareStatement(insertExamQuery)) {

            conn.setAutoCommit(false); // dùng transaction

            findGrades.setString(1, subjectId);
            ResultSet rs = findGrades.executeQuery();

            int count = 0;
            while (rs.next()) {
                int gradeId = rs.getInt("GradeID");

                insertExam.setString(1, examName);
                insertExam.setInt(2, gradeId);
                insertExam.setTimestamp(3, examDate);
                insertExam.setTime(4, examDuration);

                if (roomId != null) {
                    insertExam.setString(5, roomId);
                } else {
                    insertExam.setNull(5, Types.CHAR);
                }

                insertExam.setInt(6, examType);
                insertExam.addBatch();
                count++;
            }

            if (count == 0) {
                logger.warn("⚠️ No grades found for subjectId: {}", subjectId);
                return false;
            }

            insertExam.executeBatch();
            conn.commit();

            logger.info("✅ Created {} exams for subject {}", count, subjectId);
            return true;

        } catch (SQLException e) {
            logger.error("❌ Error creating exams for subject " + subjectId, e);
            return false;
        }
    }

    public boolean createExamForCourse(String courseId, String examName, Timestamp examDate,
                                       Time examDuration, String roomId, int examType) {
        String findGradesQuery = "SELECT GradeID FROM Grade WHERE CourseID = ?";
        String insertExamQuery = """
        INSERT INTO Exam (ExamName, GradeID, ExamDate, ExamDuration, RoomID, ExamType)
        VALUES (?, ?, ?, ?, ?, ?)
    """;

        try (Connection conn = getConnection();
             PreparedStatement findGrades = conn.prepareStatement(findGradesQuery);
             PreparedStatement insertExam = conn.prepareStatement(insertExamQuery)) {

            conn.setAutoCommit(false);

            findGrades.setString(1, courseId);
            ResultSet rs = findGrades.executeQuery();

            int count = 0;
            while (rs.next()) {
                int gradeId = rs.getInt("GradeID");

                insertExam.setString(1, examName);
                insertExam.setInt(2, gradeId);
                insertExam.setTimestamp(3, examDate);
                insertExam.setTime(4, examDuration);
                if (roomId != null) {
                    insertExam.setString(5, roomId);
                } else {
                    insertExam.setNull(5, Types.CHAR);
                }
                insertExam.setInt(6, examType);
                insertExam.addBatch();
                count++;
            }

            if (count == 0) {
                logger.warn("No grades found for courseId: {}", courseId);
                return false;
            }

            insertExam.executeBatch();
            conn.commit();
            logger.info("reated {} exams for course {}", count, courseId);
            return true;

        } catch (SQLException e) {
            logger.error("Error creating exams for course " + courseId, e);
            return false;
        }
    }

    public List<ExamSchedule> getExamScheduleByStudentId(String studentId) {
        List<ExamSchedule> exams = new ArrayList<>();
        String sql = """
        SELECT
            c.SubjectID,
            c.TermID,
            g.GradeName,
            e.ExamName,
            e.ExamDate,
            e.ExamDuration,
            e.RoomID,
            e.ExamType
        FROM Course c
        JOIN Grade g ON c.CourseID = g.CourseID
        JOIN Exam e ON g.GradeID = e.GradeID
        JOIN Study s ON c.CourseID = s.CourseID
        JOIN Student st ON s.StudentID = st.StudentID
        WHERE st.StudentID = ?
    """;

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, studentId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                LocalDateTime examDate = rs.getTimestamp("ExamDate").toLocalDateTime();
                Duration examDuration = Duration.between(LocalTime.MIN, rs.getTime("ExamDuration").toLocalTime());

                exams.add(new ExamSchedule(
                        rs.getString("SubjectID"),
                        rs.getString("TermID"),
                        rs.getString("GradeName"),
                        rs.getString("ExamName"),
                        examDate,
                        examDuration,
                        rs.getString("RoomID"),
                        rs.getInt("ExamType")
                ));
            }

        } catch (SQLException e) {
            logger.error("Error retrieving exam schedule for studentId={}", studentId, e);
        }

        return exams;
    }
}
