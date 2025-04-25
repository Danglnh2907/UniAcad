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
import util.service.file.FileService;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.Gson;

@WebServlet("/api/uploadImage")
@MultipartConfig(maxFileSize = 5 * 1024 * 1024) // 5MB max file size
public class ImageUploadAPI extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(ImageUploadAPI.class);
    private FileService fileService;
    private Gson gson;

    @Override
    public void init() throws ServletException {
        ServletContext context = getServletContext();
        fileService = new FileService(context);
        gson = new Gson();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        try {
            // Get the uploaded file
            Part filePart = request.getPart("imageFile");
            if (filePart == null || filePart.getSize() == 0) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.println(gson.toJson(createErrorResponse("No file uploaded.")));
                return;
            }

            String fileName = filePart.getSubmittedFileName();
            if (!fileName.toLowerCase().endsWith(".png") && !fileName.toLowerCase().endsWith(".jpg")) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.println(gson.toJson(createErrorResponse("Only .png or .jpg files are supported.")));
                return;
            }

            // Save the image using FileService
            try (InputStream fileContent = filePart.getInputStream()) {
                byte[] fileBytes = fileContent.readAllBytes();
                boolean saved = fileService.saveFileImg(fileName, fileBytes);
                if (!saved) {
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    out.println(gson.toJson(createErrorResponse("Failed to save the image.")));
                    return;
                }
            }

            // Create success response
            Map<String, Object> responseData = new LinkedHashMap<>();
            responseData.put("message", "Image uploaded successfully.");
            responseData.put("fileName", fileName);
            responseData.put("imageUrl", request.getContextPath() + "/resources/img/" + fileName);

            out.println(gson.toJson(responseData));

        } catch (Exception e) {
            logger.error("Error processing image upload: {}", e.getMessage(), e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.println(gson.toJson(createErrorResponse("Error processing image: " + e.getMessage())));
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