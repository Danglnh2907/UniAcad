package controller.api.auth;

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
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;

@WebServlet("/api/uploadExcel")
@MultipartConfig(maxFileSize = 10 * 1024 * 1024) // 10MB max file size
public class ExcelUploadAPI extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(ExcelUploadAPI.class);
    private FileService fileService;
    private ExcelService excelService;
    private Gson gson;

    @Override
    public void init() throws ServletException {
        ServletContext context = getServletContext();
        fileService = new FileService(context);
        excelService = new ExcelService(fileService);
        gson = new Gson();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        try {
            // Get the uploaded file
            Part filePart = request.getPart("excelFile");
            if (filePart == null || filePart.getSize() == 0) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.write(gson.toJson(createErrorResponse("No file uploaded.")));
                return;
            }

            String fileName = filePart.getSubmittedFileName();
            if (!fileName.toLowerCase().endsWith(".xlsx")) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.write(gson.toJson(createErrorResponse("Only .xlsx files are supported.")));
                return;
            }

            // Save the file using FileService
            try (InputStream fileContent = filePart.getInputStream()) {
                byte[] fileBytes = fileContent.readAllBytes();
                boolean saved = fileService.saveFileXlsx(fileName, fileBytes);
                if (!saved) {
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    out.write(gson.toJson(createErrorResponse("Failed to save the file.")));
                    return;
                }
            }

            // Define column configurations
            List<ColumnConfig> columnConfigs = Arrays.asList(
                    new ColumnConfig(0, "Name", DataType.STRING, true),
                    new ColumnConfig(1, "Email", DataType.EMAIL, true),
                    new ColumnConfig(2, "Address", DataType.STRING, false),
                    new ColumnConfig(3, "Salary", DataType.INTEGER, false),
                    new ColumnConfig(4, "Description", DataType.STRING, false),
                    new ColumnConfig(5, "Image", DataType.IMAGE, false)
            );

            // Process the Excel file
            ExcelProcessingResult result = excelService.processExcelFile(fileName, columnConfigs);

            // Create response
            Map<String, Object> responseData = new LinkedHashMap<>();
            responseData.put("fileName", fileName);
            responseData.put("columnNames", columnConfigs.stream().map(ColumnConfig::getName).toList());
            responseData.put("excelData", result.getData());
            responseData.put("errors", result.getErrors());

            out.println(gson.toJson(responseData));

        } catch (Exception e) {
            logger.error("Error processing Excel upload: {}", e.getMessage(), e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.println(gson.toJson(createErrorResponse("Error processing file: " + e.getMessage())));
        } finally {
            out.flush();
        }
    }

    private Map<String, String> createErrorResponse(String message) {
        Map<String, String> error = new LinkedHashMap<>();
        error.put("errorMessage", message);
        return error;
    }
}
