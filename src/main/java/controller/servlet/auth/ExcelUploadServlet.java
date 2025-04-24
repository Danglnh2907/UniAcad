package controller.servlet.auth;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.service.excel.ExcelService;
import util.service.excel.ExcelService.ColumnConfig;
import util.service.excel.ExcelService.DataType;
import util.service.excel.ExcelService.ExcelProcessingResult;
import util.service.file.FileService;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

@WebServlet("/uploadExcel")
@MultipartConfig(maxFileSize = 10 * 1024 * 1024) // 10MB max file size
public class ExcelUploadServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(ExcelUploadServlet.class);
    private FileService fileService;
    private ExcelService excelService;

    @Override
    public void init() throws ServletException {
        ServletContext context = getServletContext();
        fileService = new FileService(context, "/WEB-INF/resources");
        LoggerFactory.getLogger(fileService.getFilePath());
        excelService = new ExcelService(fileService);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Forward to JSP for file upload form
        request.getRequestDispatcher("excelUpload.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            // Get the uploaded file
            Part filePart = request.getPart("excelFile");
            if (filePart == null || filePart.getSize() == 0) {
                request.setAttribute("errorMessage", "No file uploaded.");
                request.getRequestDispatcher("excelUpload.jsp").forward(request, response);
                return;
            }

            String fileName = filePart.getSubmittedFileName();
            if (!fileName.toLowerCase().endsWith(".xlsx")) {
                request.setAttribute("errorMessage", "Only .xlsx files are supported.");
                request.getRequestDispatcher("excelUpload.jsp").forward(request, response);
                return;
            }

            // Save the file using FileService
            try (InputStream fileContent = filePart.getInputStream()) {
                byte[] fileBytes = fileContent.readAllBytes();
                boolean saved = fileService.saveFileXlsx(fileName, fileBytes);
                if (!saved) {
                    request.setAttribute("errorMessage", "Failed to save the file.");
                    request.getRequestDispatcher("excelUpload.jsp").forward(request, response);
                    return;
                }
            }

            // Define column configurations (can be dynamic or configured externally)
            List<ColumnConfig> columnConfigs = Arrays.asList(
                    new ColumnConfig(0, "Name", DataType.STRING, true),
                    new ColumnConfig(1, "Email", DataType.EMAIL, true),
                    new ColumnConfig(2, "Address", DataType.STRING, false),
                    new ColumnConfig(3, "Salary", DataType.INTEGER, false),
                    new ColumnConfig(4, "Description", DataType.STRING, false)
            );

            // Process the Excel file
            ExcelProcessingResult result = excelService.processExcelFile(fileName, columnConfigs);

            // Set attributes for JSP
            request.setAttribute("excelData", result.getData());
            request.setAttribute("errors", result.getErrors());
            request.setAttribute("fileName", fileName);
            // Forward to JSP to display results
            request.getRequestDispatcher("excelUpload.jsp").forward(request, response);

        } catch (Exception e) {
            logger.error("Error processing Excel upload: {}", e.getMessage(), e);
            request.setAttribute("errorMessage", "Error processing file: " + e.getMessage());
            request.getRequestDispatcher("excelUpload.jsp").forward(request, response);
        }
    }
}