package controller.auth;

import util.excel.ExcelService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

@WebServlet("/UploadServlet")
@MultipartConfig
public class ExcelServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(ExcelServlet.class);
    private final ExcelService excelService = new ExcelService();

    @Override
    public void init() throws ServletException {
        // Initialize upload directory if needed
        String uploadDir = getServletContext().getRealPath("/") + "uploads/photos";
        excelService.initializeUploadDirectory(uploadDir);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            // Get Excel file from request
            Part filePart = request.getPart("file");
            String fileName = filePart.getSubmittedFileName();
            if (!fileName.endsWith(".xlsx")) {
                logger.warn("Invalid file format: {}", fileName);
                request.setAttribute("message", "Error: Only .xlsx format is supported!");
                request.getRequestDispatcher("upload.jsp").forward(request, response);
                return;
            }

            try (InputStream fileContent = filePart.getInputStream()) {
                // Process Excel file using ExcelService
                String resultMessage = excelService.processExcelFile(fileContent, getServletContext().getRealPath("/"));
                request.setAttribute("message", resultMessage);
            } catch (Exception e) {
                logger.error("Error processing file: {}", e.getMessage(), e);
                request.setAttribute("message", "Error processing file: " + e.getMessage());
            }
        } catch (Exception e) {
            logger.error("Error handling request: {}", e.getMessage(), e);
            request.setAttribute("message", "Error handling request: " + e.getMessage());
        }
        request.getRequestDispatcher("upload.jsp").forward(request, response);
    }
}