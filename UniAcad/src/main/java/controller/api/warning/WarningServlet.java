package controller.api.warning;

import model.datasupport.WarningInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import javax.sql.DataSource;
import jakarta.annotation.Resource;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet("/api/warnings")
public class WarningServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(WarningServlet.class.getName());

    @Resource(lookup = "jdbc/UniAcadDB")
    private DataSource dataSource;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        LOGGER.info("Received GET request for /api/warnings");

        resp.setContentType("application/json;charset=UTF-8");
        List<WarningInfo> warnings = new ArrayList<>();

        String sql = """
            SELECT
                st.StudentID,
                st.StudentName,
                su.SubjectName,
                SUM(CASE WHEN a.Status = 0 THEN 1 ELSE 0 END) AS AbsentCount,
                COUNT(s.SlotNumber) AS TotalSlots,
                ISNULL(gr.Mark, 0) AS Mark
            FROM Student st
            JOIN [Group] g ON st.StudentID = g.StudentID
            JOIN Study st2 ON g.ClassID = st2.ClassID
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

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

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
                    WarningInfo warning = new WarningInfo(
                            studentId, studentName, subjectName,
                            absentCount, totalSlots, absentRate, mark, warningType
                    );
                    warnings.add(warning);
                    LOGGER.fine("Warning detected: " + warning);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error while fetching warnings", e);
        }

        LOGGER.info("Returning " + warnings.size() + " warnings as JSON response.");

        ObjectMapper mapper = new ObjectMapper();
        try {
            mapper.writeValue(resp.getOutputStream(), warnings);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error writing JSON response", e);
        }
    }

    private String determineWarning(double absentRate, double mark) {
        if (absentRate >= 0.4) {
            return "Banned from Exam";
        } else if (absentRate >= 0.2 && mark < 5.0) {
            return "High Risk Fail";
        } else if (mark < 5.0) {
            return "Low Grade Warning";
        } else if (absentRate >= 0.2) {
            return "High Absence Warning";
        }
        return null;
    }
}
