package util.service.excel;

import dao.CurriculumDAO;
import model.database.Curriculum;
import model.database.Student;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.service.file.FileService;
import util.service.excel.ExcelService.ColumnConfig;
import util.service.excel.ExcelService.DataType;
import util.service.excel.ExcelService.ExcelProcessingResult;
import util.service.excel.ExcelService.ProcessingError;
import util.service.normalization.NormalizationService;
import util.service.security.VerifyChecking;

import java.io.IOException;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for generating Excel templates and reading student data from Excel files.
 */
public class ExcelTemplateService {
    private static final Logger logger = LoggerFactory.getLogger(ExcelTemplateService.class);
    private static final String[] REQUIRED_FIELDS = {
            "studentid", "studentname", "studentssn", "studentemail",
            "studentphone", "curriculumid", "studentgender", "studentdob"
    };

    private final ExcelService excelService;
    private final VerifyChecking verifyChecking;
    private final NormalizationService normalizationService;
    private final CurriculumDAO curriculumDAO;

    public ExcelTemplateService(FileService fileService) {
        if (fileService == null) {
            throw new IllegalArgumentException("FileService cannot be null");
        }
        this.excelService = new ExcelService(fileService);
        this.verifyChecking = new VerifyChecking();
        this.normalizationService = new NormalizationService();
        this.curriculumDAO = new CurriculumDAO();
    }

    /**
     * Generates an Excel template for adding students with dropdowns and column descriptions.
     *
     * @param fileName Name of the template file to generate
     * @throws IOException If file generation fails
     */
    public void generateTemplateAddStudent(String fileName) throws IOException {
        List<String> curriculumIds = curriculumDAO.getAllCurriculumID();
        if (curriculumIds == null || curriculumIds.isEmpty()) {
            throw new IllegalStateException("No Curriculum IDs found in database");
        }

        List<ColumnConfig> configs = Arrays.asList(
                new ColumnConfig(0, "StudentID", DataType.STRING, true, null, null, "Unique student ID, max 8 characters"),
                new ColumnConfig(1, "StudentName", DataType.STRING, true, null, null, "Full name, capitalize first letters"),
                new ColumnConfig(2, "StudentSSN", DataType.STRING, true, null, null, "National ID or identifier"),
                new ColumnConfig(3, "StudentEmail", DataType.EMAIL, true, null, null, "Valid email, e.g., abc@example.com"),
                new ColumnConfig(4, "StudentPhone", DataType.STRING, true, null, null, "Phone number, 10–15 digits"),
                new ColumnConfig(5, "CurriculumID", DataType.STRING, true, null, curriculumIds, "Select from curriculum list"),
                new ColumnConfig(6, "StudentGender", DataType.BOOLEAN, true, null, null, "TRUE for male, FALSE for female"),
                new ColumnConfig(7, "Address", DataType.STRING, false, null, null, "Residence address (optional)"),
                new ColumnConfig(8, "StudentDoB", DataType.DATE, true, null, null, "Date of birth (dd/MM/yyyy)")
        );

        excelService.generateExcelTemplate(fileName, configs);
        logger.info("Generated student template: {}", fileName);
    }

    /**
     * Reads student data from an Excel file, inferring columns from the header.
     *
     * @param fileName Name of the Excel file to read
     * @return List of Student objects
     * @throws IOException If file reading fails
     */
    public List<Student> readStudentsFromExcel(String fileName) throws IOException {
        ExcelProcessingResult result = excelService.processExcelFile(
                fileName,
                null,  // Infer columns from header
                3,     // Start from row 3 (0-based index)
                null,  // First sheet
                1      // Header at row 1 (0-based index)
        );

        if (result == null || result.data == null || result.data.isEmpty()) {
            logger.warn("No student data found in file: {}", fileName);
            return Collections.emptyList();
        }

        // Log any processing errors from ExcelService
        if (!result.errors.isEmpty()) {
            logger.error("Excel processing errors ({}):", result.errors.size());
            result.errors.forEach(err -> logger.error("  - Row {}, Col {}: {}", err.rowIndex, err.columnIndex, err.message));
        }

        List<Student> students = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (Map<String, Object> rawRow : result.data) {
            try {
                // Normalize keys: remove spaces, asterisks, and convert to lowercase
                Map<String, Object> row = normalizeRow(rawRow);

                // Validate required fields
                validateRequiredFields(row);

                Student student = createStudentFromRow(row);
                students.add(student);

            } catch (Exception e) {
                String studentId = rawRow.getOrDefault("StudentID", "unknown").toString();
                String errorMsg = String.format("Error processing student (ID=%s): %s", studentId, e.getMessage());
                errors.add(errorMsg);
                logger.error(errorMsg);
                rawRow.forEach((k, v) -> logger.error("  - {} = {}", k, v));
                logger.debug("Stack trace:", e);
            }
        }

        if (!errors.isEmpty()) {
            logger.error("Total processing errors: {}", errors.size());
        }

        logger.info("Processed {} students successfully", students.size());
        return students;
    }

    private Map<String, Object> normalizeRow(Map<String, Object> rawRow) {
        return rawRow.entrySet().stream()
                .filter(e -> e.getKey() != null && e.getValue() != null)
                .collect(Collectors.toMap(
                        e -> e.getKey().trim().replaceAll("[\\s\\*]+", "").toLowerCase(),
                        Map.Entry::getValue,
                        (v1, v2) -> v1,
                        LinkedHashMap::new
                ));
    }

    private void validateRequiredFields(Map<String, Object> row) {
        for (String field : REQUIRED_FIELDS) {
            if (!row.containsKey(field) || row.get(field) == null) {
                throw new IllegalArgumentException("Missing or null required field: " + field);
            }
        }
    }

    private Student createStudentFromRow(Map<String, Object> row) {
        Student student = new Student();

        student.setStudentID(String.valueOf(row.get("studentid")));
        student.setStudentName(normalizationService.normalizationFullName(String.valueOf(row.get("studentname"))));
        student.setStudentSSN(String.valueOf(row.get("studentssn")));
        student.setStudentEmail(normalizationService.normalizationEmail(String.valueOf(row.get("studentemail"))));
        student.setStudentPhone(String.valueOf(row.get("studentphone")));

        Curriculum curriculum = new Curriculum();
        curriculum.setCurriculumID(String.valueOf(row.get("curriculumid")));
        student.setCurriculumID(curriculum);

        // Handle gender
        Object genderRaw = row.get("studentgender");
        if (genderRaw instanceof Boolean) {
            student.setStudentGender((Boolean) genderRaw);
        } else {
            String genderStr = String.valueOf(genderRaw).trim().toLowerCase();
            if ("true".equals(genderStr) || "1".equals(genderStr)) {
                student.setStudentGender(true);
            } else if ("false".equals(genderStr) || "0".equals(genderStr)) {
                student.setStudentGender(false);
            } else {
                throw new IllegalArgumentException("Invalid StudentGender value: " + genderRaw);
            }
        }

        // Handle optional address
        student.setAddress(row.containsKey("address") ? String.valueOf(row.get("address")) : null);

        // Handle date of birth
        Object dobRaw = row.get("studentdob");
        if (dobRaw instanceof Date dob) {
            student.setStudentDoB(dob.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
        } else {
            throw new IllegalArgumentException("Invalid date of birth for student " + student.getStudentID() + ": " + dobRaw);
        }

        student.setStudentStatus(0); // Default: enrolled
        return student;
    }

    public static void main(String[] args) {
        FileService fileService = new FileService(""); // Specify root if needed
        ExcelTemplateService service = new ExcelTemplateService(fileService);

        try {
             service.generateTemplateAddStudent("StudentTemplate.xlsx");

            List<Student> students = service.readStudentsFromExcel("StudentTemplate1.xlsx");
            if (students.isEmpty()) {
                logger.warn("No students read from file");
            } else {
                logger.info("Successfully read {} students:", students.size());
                for (Student student : students) {
                    logger.info("  - [{}] {} ({})", student.getStudentID(), student.getStudentName(), student.getStudentEmail());
                }
            }
        } catch (IOException e) {
            logger.error("Failed to read Excel file: {}", e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error: {}", e.getMessage(), e);
        }
    }
}