package util.service.file;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import jakarta.servlet.ServletContext;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileService {
    private static final Logger logger = LogManager.getLogger(FileService.class);
    private final String filePath;
    private static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024; // 10MB, đồng bộ với MailService
    static final String EXCEL_FILE_PATH = "xlsx";
    static final String WORD_FILE_PATH = "docx";
    static final String PDF_FILE_PATH = "pdf";
    static final String IMAGE_FILE_PATH = "img";
    static final String TEMPLATE_FILE_PATH = "templates";

    // Constructor chính
    public FileService(String filePathInput) {
        if (filePathInput == null || filePathInput.isEmpty()) {
            String projectDir = System.getProperty("user.dir");
            this.filePath = projectDir + File.separator + "src" + File.separator + "main" + File.separator + "resources" + File.separator;
        } else {
            this.filePath = filePathInput.replaceAll("[/\\\\]+$", "") + File.separator;
        }
        logger.info("File path initialized: {}", filePath);
    }

    // Constructor cho Tomcat
    public FileService(ServletContext context, String relativePath) {
        if (context == null) {
            throw new IllegalArgumentException("ServletContext cannot be null");
        }
        this.filePath = context.getRealPath(relativePath != null ? relativePath : "/WEB-INF/resources") + File.separator;
        logger.info("File path initialized from ServletContext: {}", filePath);
    }

    // Getter cho filePath
    public String getFilePath() {
        return filePath;
    }

    // Lưu tệp DOCX
    public boolean saveFileDocx(String fileName, byte[] fileContent) {
        return saveFile(fileName, fileContent, WORD_FILE_PATH);
    }

    // Lưu tệp XLSX
    public boolean saveFileXlsx(String fileName, byte[] fileContent) {
        return saveFile(fileName, fileContent, EXCEL_FILE_PATH);
    }

    // Lưu tệp PDF
    public boolean saveFilePdf(String fileName, byte[] fileContent) {
        return saveFile(fileName, fileContent, PDF_FILE_PATH);
    }

    // Lưu tệp ảnh (bao gồm GIF)
    public boolean saveFileImg(String fileName, byte[] fileContent) {
        return saveFile(fileName, fileContent, IMAGE_FILE_PATH);
    }

    // Lưu tệp template
    public boolean saveFileTemplate(String fileName, byte[] fileContent) {
        return saveFile(fileName, fileContent, TEMPLATE_FILE_PATH);
    }

    // Phương thức lưu tệp chung
    private boolean saveFile(String fileName, byte[] fileContent, String subDir) {
        if (fileName == null || fileName.trim().isEmpty()) {
            logger.error("File name is null or empty");
            return false;
        }
        if (fileContent == null || fileContent.length == 0) {
            logger.error("File content is null or empty for file: {}", fileName);
            return false;
        }
        if (fileContent.length > MAX_FILE_SIZE_BYTES) {
            logger.error("File too large: {} ({} bytes, max {} bytes)", fileName, fileContent.length, MAX_FILE_SIZE_BYTES);
            return false;
        }
        if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
            logger.error("Invalid file name (potential path traversal): {}", fileName);
            return false;
        }

        // Kiểm tra định dạng tệp
        try {
            validateFileContent(fileName, fileContent, subDir);
        } catch (IOException e) {
            logger.error("Invalid file content for {}: {}", fileName, e.getMessage());
            return false;
        }

        Path path = Paths.get(filePath, subDir, fileName);
        try {
            Files.createDirectories(path.getParent());
            Files.write(path, fileContent);
            logger.info("File saved successfully: {}", path);
            return true;
        } catch (IOException e) {
            logger.error("Error saving file: {}", fileName, e);
            return false;
        }
    }

    // Kiểm tra định dạng tệp
    private void validateFileContent(String fileName, byte[] content, String subDir) throws IOException {
        String lowerCaseName = fileName.toLowerCase();
        if (subDir.equals(PDF_FILE_PATH)) {
            if (content.length < 4 || !new String(content, 0, 4, StandardCharsets.UTF_8).startsWith("%PDF")) {
                throw new IOException("Invalid PDF file: " + fileName);
            }
        } else if (subDir.equals(WORD_FILE_PATH) || subDir.equals(EXCEL_FILE_PATH)) {
            if (content.length < 2 || content[0] != 0x50 || content[1] != 0x4B) {
                throw new IOException("Invalid " + (subDir.equals(WORD_FILE_PATH) ? "DOCX" : "XLSX") + " file: " + fileName);
            }
        } else if (subDir.equals(IMAGE_FILE_PATH)) {
            if (lowerCaseName.endsWith(".gif")) {
                if (content.length < 6 || (!new String(content, 0, 6, StandardCharsets.US_ASCII).equals("GIF87a") &&
                        !new String(content, 0, 6, StandardCharsets.US_ASCII).equals("GIF89a"))) {
                    throw new IOException("Invalid GIF file: " + fileName);
                }
            } else if (lowerCaseName.matches(".*\\.(png|jpg|jpeg)$")) {
                try {
                    javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(content));
                } catch (Exception e) {
                    throw new IOException("Invalid image file: " + fileName, e);
                }
            } else {
                throw new IOException("Unsupported image format: " + fileName);
            }
        } else if (subDir.equals(TEMPLATE_FILE_PATH) && !lowerCaseName.endsWith(".html")) {
            throw new IOException("Template must be an HTML file: " + fileName);
        }
    }

    // Lấy đường dẫn tệp
    public String getFilePath(String fileName, String fileType) {
        if (fileName == null || fileName.trim().isEmpty()) {
            logger.error("File name is null or empty");
            throw new IllegalArgumentException("File name cannot be null or empty");
        }
        if (fileType == null) {
            logger.error("File type is null for file: {}", fileName);
            throw new IllegalArgumentException("File type cannot be null");
        }
        String subDir;
        switch (fileType) {
            case "xlsx":
                subDir = EXCEL_FILE_PATH;
                break;
            case "docx":
                subDir = WORD_FILE_PATH;
                break;
            case "pdf":
                subDir = PDF_FILE_PATH;
                break;
            case "img":
                subDir = IMAGE_FILE_PATH;
                break;
            case "template":
                subDir = TEMPLATE_FILE_PATH;
                break;
            default:
                logger.error("Unsupported file type: {} for file: {}", fileType, fileName);
                throw new IllegalArgumentException("Unsupported file type: " + fileType);
        }
        return Paths.get(filePath, subDir, fileName).toString();
    }

    // Lấy InputStream từ tệp
    public InputStream getFileInputStream(String fileName, String fileType) throws IOException {
        String filePath = getFilePath(fileName, fileType);
        File file = new File(filePath);
        if (!file.exists()) {
            logger.error("File not found: {}", filePath);
            throw new IOException("File not found: " + filePath);
        }
        logger.debug("Opening InputStream for file: {}", filePath);
        return Files.newInputStream(file.toPath());
    }

    // Xóa tệp
    public boolean deleteFile(String fileName, String fileType) {
        String filePath = getFilePath(fileName, fileType);
        File file = new File(filePath);
        if (!file.exists()) {
            logger.warn("File does not exist, cannot delete: {}", filePath);
            return false;
        }
        try {
            Files.delete(file.toPath());
            logger.info("File deleted successfully: {}", filePath);
            return true;
        } catch (IOException e) {
            logger.error("Error deleting file: {}", fileName, e);
            return false;
        }
    }
}