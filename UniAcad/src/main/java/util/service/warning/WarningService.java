package util.service.warning;

import model.datasupport.WarningInfo;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
public class WarningService {

    private static final Logger LOGGER = Logger.getLogger(WarningService.class.getName());

    @PersistenceContext(unitName = "UniAcadPU")
    private EntityManager em;

    public List<WarningInfo> getWarnings() {
        List<WarningInfo> warnings = new ArrayList<>();
        try {
            LOGGER.info("Starting to fetch student warnings...");

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

            Query query = em.createNativeQuery(sql);
            List<Object[]> resultList = query.getResultList();

            for (Object[] row : resultList) {
                String studentId = (String) row[0];
                String studentName = (String) row[1];
                String subjectName = (String) row[2];
                int absentCount = ((Number) row[3]).intValue();
                int totalSlots = ((Number) row[4]).intValue();
                Double mark = ((Number) row[5]).doubleValue();
                double absentRate = totalSlots == 0 ? 0 : (double) absentCount / totalSlots;

                String warningType = determineWarning(absentRate, mark);

                if (warningType != null) {
                    warnings.add(new WarningInfo(studentId, studentName, subjectName,
                            absentCount, totalSlots, absentRate, mark, warningType));
                }
            }

            LOGGER.info("Fetched " + warnings.size() + " warning(s) successfully.");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching warnings", e);
        }

        return warnings;
    }

    private String determineWarning(double absentRate, Double mark) {
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
