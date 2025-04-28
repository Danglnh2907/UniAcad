package controller.api.student.timetable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dao.ScheduleDAO;
import dao.StudentDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import model.database.Student;
import model.datasupport.ScheduleItem;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@WebServlet(name = "StudentWeeklyTimetableApiServlet", urlPatterns = {"/api/student/timetable"})
public class StudentWeeklyTimetableApiServlet extends HttpServlet {

    private final ScheduleDAO scheduleDAO = new ScheduleDAO();
    private final Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss").create();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");

        try {
            StudentDAO studentDAO = new StudentDAO();
            String email = (String) request.getSession().getAttribute("email");

            if (email == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"error\": -1, \"message\": \"Unauthorized: Please login first\"}");
                return;
            }

            Student student = studentDAO.getStudentByEmail(email);
            if (student == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"error\": -1, \"message\": \"Student not found\"}");
                return;
            }

            String studentId = student.getStudentID(); // Lấy từ DB chứ không lấy từ session nữa!

            // --- XỬ LÝ PARAM start + end nếu có ---
            String startParam = request.getParameter("start");
            String endParam = request.getParameter("end");

            Date startDate;
            Date endDate;

            if (startParam != null && endParam != null) {
                startDate = java.sql.Date.valueOf(startParam);
                endDate = java.sql.Date.valueOf(endParam);
            } else {
                // Nếu không có param ➔ mặc định tuần hiện tại
                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
                startDate = new Date(cal.getTimeInMillis());

                cal.add(Calendar.DATE, 7);
                endDate = new Date(cal.getTimeInMillis());
            }

            List<ScheduleItem> schedules = scheduleDAO.getWeeklySchedule(studentId, startDate, endDate);

            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(gson.toJson(schedules));

        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\": -1, \"message\": \"Server error: " + e.getMessage() + "\"}");
        }
    }
}
