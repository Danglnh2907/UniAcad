package util.excel;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFPictureData;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class ExcelService {
    private static final Logger logger = LoggerFactory.getLogger(ExcelService.class);
    private static final String UPLOAD_DIR = "uploads/photos";

    public void initializeUploadDirectory(String uploadDirPath) {
        File uploadDir = new File(uploadDirPath);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }
    }

    public String processExcelFile(InputStream fileContent, String contextPath) throws IOException {
        Workbook workbook = new XSSFWorkbook(fileContent);
        Sheet sheet = workbook.getSheetAt(0);
        List<XSSFPictureData> pictures = ((XSSFWorkbook) workbook).getAllPictures();
        logger.info("Found {} pictures in Excel file", pictures.size());

        int processedRows = 0;

        for (Row row : sheet) {
            if (row.getRowNum() == 0) continue; // Skip header row

            Cell emailCell = row.getCell(0);
            if (emailCell == null || emailCell.getCellType() != CellType.STRING || emailCell.getStringCellValue().trim().isEmpty()) {
                logger.warn("Empty email in row: {}", row.getRowNum() + 1);
                continue;
            }

            String email = emailCell.getStringCellValue().trim();
            if (!isValidEmail(email)) {
                logger.warn("Invalid email in row {}: {}", row.getRowNum() + 1, email);
                continue;
            }
            Cell fullNameCell = row.getCell(1);
            String fullName = fullNameCell != null ? fullNameCell.getStringCellValue().trim() : "";

            Cell addressCell = row.getCell(2);
            String address = addressCell != null ? addressCell.getStringCellValue().trim() : "";

            // Log to console instead of saving to database
            logger.info("Processed user - Email: {}, Full Name: {}, Address: {}", email, fullName, address);
            processedRows++;
        }

        workbook.close();
        return "Processed " + processedRows + " students successfully!";
    }

    private boolean isValidEmail(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
        return email.matches(emailRegex);
    }
}