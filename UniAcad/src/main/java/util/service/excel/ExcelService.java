package util.service.excel;

import jakarta.servlet.ServletContext;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.service.file.FileService;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * A service for processing Excel files with flexible column configurations and multiple data types.
 * Supports extracting and saving images, handling various data types, and processing multiple sheets.
 * Integrates with {@link FileService} for file operations.
 *
 * @author [Your Name]
 */
public class ExcelService {
    private static final Logger logger = LoggerFactory.getLogger(ExcelService.class);
    private static final String UPLOAD_DIR = "xlsx";

    private final FileService fileService;

    /**
     * Constructs an ExcelService with a FileService instance for handling file operations.
     *
     * @param fileService the FileService instance for saving images and accessing files
     * @throws IllegalArgumentException if fileService is null
     */
    public ExcelService(FileService fileService) {
        if (fileService == null) {
            throw new IllegalArgumentException("FileService cannot be null");
        }
        this.fileService = fileService;
        initializeUploadDirectory();
    }

    /**
     * Initializes the upload directory for storing images extracted from Excel files.
     */
    private void initializeUploadDirectory() {
        fileService.saveFileImg("dummy.txt", new byte[0]); // Trigger directory creation
        logger.info("Thread {}: Initialized upload directory: {}",
                Thread.currentThread().getName(), UPLOAD_DIR);
    }

    /**
     * Processes an Excel file and extracts data based on the provided column configurations.
     * Supports multiple data types (STRING, INTEGER, DOUBLE, DATE, IMAGE) and saves images using FileService.
     *
     * @param fileName      the input stream of the Excel file
     * @param columnConfigs the list of column configurations, or null to infer from header row
     * @return an ExcelProcessingResult containing the processed data and any errors
     * @throws IOException              if the Excel file cannot be read
     * @throws IllegalArgumentException if fileContent is null or columnConfigs are invalid
     */
    public ExcelProcessingResult processExcelFile(String fileName, List<ColumnConfig> columnConfigs) throws IOException {
        InputStream fileContent = new FileInputStream(fileService.getFilePath(fileName, "xlsx"));
        validateColumnConfigs(columnConfigs);

        XSSFWorkbook workbook = new XSSFWorkbook(fileContent);
        Sheet sheet = workbook.getSheetAt(0);
        List<XSSFPictureData> pictures = workbook.getAllPictures();
        logger.info("Thread {}: Processing sheet '{}': {} rows, {} pictures found",
                Thread.currentThread().getName(), sheet.getSheetName(), sheet.getLastRowNum(), pictures.size());

        ExcelProcessingResult result = processSheet(sheet, pictures, columnConfigs);

        workbook.close();
        logger.info("Thread {}: Processed {} rows successfully with {} errors",
                Thread.currentThread().getName(), result.getData().size(), result.getErrors().size());
        return result;
    }

    /**
     * Processes a specific sheet in the Excel file.
     *
     * @param sheet         the sheet to process
     * @param pictures      the list of pictures in the Excel file
     * @param columnConfigs the column configurations to apply
     * @return an ExcelProcessingResult containing the processed data and errors
     */
    private ExcelProcessingResult processSheet(Sheet sheet, List<XSSFPictureData> pictures, List<ColumnConfig> columnConfigs) throws IOException {
        List<ColumnConfig> effectiveConfigs = columnConfigs != null && !columnConfigs.isEmpty()
                ? columnConfigs
                : inferColumnConfigs(sheet.getRow(0));

        List<Map<String, Object>> data = new ArrayList<>();
        List<ProcessingError> errors = new ArrayList<>();

        for (Row row : sheet) {
            if (row.getRowNum() == 0) continue; // Skip header row
            Map<String, Object> rowData = processRow(row, effectiveConfigs, pictures, errors);
            if (rowData != null) {
                data.add(rowData);
            }
        }

        return new ExcelProcessingResult(data, errors);
    }

    /**
     * Processes a single row and extracts data based on column configurations.
     *
     * @param row      the row to process
     * @param configs  the column configurations
     * @param pictures the list of pictures in the Excel file
     * @param errors   the list to store processing errors
     * @return a map of column names to values, or null if no valid data
     * @throws IOException if an image cannot be saved
     */
    private Map<String, Object> processRow(Row row, List<ColumnConfig> configs, List<XSSFPictureData> pictures, List<ProcessingError> errors) throws IOException {
        Map<String, Object> rowData = new LinkedHashMap<>();
        boolean hasValidData = false;

        for (ColumnConfig config : configs) {
            Cell cell = row.getCell(config.getColumnIndex());
            try {
                Object value = getCellValue(cell, config.getType(), pictures, row.getRowNum());
                if (value != null || !config.isRequired()) {
                    rowData.put(config.getName(), value);
                    hasValidData = true;
                } else {
                    errors.add(new ProcessingError(row.getRowNum() + 1, config.getColumnIndex(),
                            "Missing required value for " + config.getName(), ProcessingError.ErrorType.MISSING_VALUE));
                    logger.warn("Thread {}: Row {}: Missing required value for column '{}'",
                            Thread.currentThread().getName(), row.getRowNum() + 1, config.getName());
                }
            } catch (Exception e) {
                errors.add(new ProcessingError(row.getRowNum() + 1, config.getColumnIndex(),
                        "Invalid value for " + config.getName() + ": " + e.getMessage(), ProcessingError.ErrorType.INVALID_TYPE));
                logger.warn("Thread {}: Row {}: Invalid value for column '{}': {}",
                        Thread.currentThread().getName(), row.getRowNum() + 1, config.getName(), e.getMessage());
            }
        }

        return hasValidData ? rowData : null;
    }

    /**
     * Validates the provided column configurations to ensure no duplicate names or invalid indices.
     *
     * @param columnConfigs the list of column configurations to validate
     * @throws IllegalArgumentException if configurations are invalid
     */
    private void validateColumnConfigs(List<ColumnConfig> columnConfigs) {
        if (columnConfigs == null) return;
        Set<String> names = new HashSet<>();
        Set<Integer> indices = new HashSet<>();
        for (ColumnConfig config : columnConfigs) {
            if (!names.add(config.getName())) {
                logger.error("Thread {}: Duplicate column name: {}",
                        Thread.currentThread().getName(), config.getName());
                throw new IllegalArgumentException("Duplicate column name: " + config.getName());
            }
            if (!indices.add(config.getColumnIndex()) || config.getColumnIndex() < 0) {
                logger.error("Thread {}: Invalid column index: {}",
                        Thread.currentThread().getName(), config.getColumnIndex());
                throw new IllegalArgumentException("Invalid column index: " + config.getColumnIndex());
            }
        }
    }

    /**
     * Infers column configurations from the header row if no explicit configuration is provided.
     *
     * @param headerRow the header row of the Excel sheet
     * @return a list of inferred column configurations
     */
    private List<ColumnConfig> inferColumnConfigs(Row headerRow) {
        if (headerRow == null) {
            logger.warn("Thread {}: No header row found, using default column names",
                    Thread.currentThread().getName());
            return Collections.emptyList();
        }

        List<ColumnConfig> configs = new ArrayList<>();
        for (Cell cell : headerRow) {
            String name = cell.getCellType() == CellType.STRING ? cell.getStringCellValue().trim() : "Column" + cell.getColumnIndex();
            if (name.isEmpty()) name = "Column" + cell.getColumnIndex();
            DataType type = inferDataType(cell);
            configs.add(new ColumnConfig(cell.getColumnIndex(), name, type, false));
        }
        logger.info("Thread {}: Inferred {} column configurations from header row",
                Thread.currentThread().getName(), configs.size());
        return configs;
    }

    /**
     * Infers the data type of a cell based on its content.
     *
     * @param cell the cell to analyze
     * @return the inferred data type
     */
    private DataType inferDataType(Cell cell) {
        if (cell == null) return DataType.STRING;
        switch (cell.getCellType()) {
            case STRING:
                return isValidEmail(cell.getStringCellValue()) ? DataType.EMAIL : DataType.STRING;
            case NUMERIC:
                return DateUtil.isCellDateFormatted(cell) ? DataType.DATE :
                        cell.getNumericCellValue() == Math.floor(cell.getNumericCellValue()) ? DataType.INTEGER : DataType.DOUBLE;
            case BOOLEAN:
                return DataType.BOOLEAN;
            default:
                return DataType.STRING;
        }
    }

    /**
     * Retrieves and converts the value of a cell based on the specified data type.
     * For IMAGE type, saves the image using FileService and returns the file path.
     *
     * @param cell     the cell to process
     * @param type     the expected data type
     * @param pictures the list of pictures in the Excel file
     * @param rowIndex the row index for naming image files
     * @return the cell value, or null if invalid
     * @throws IOException if an image cannot be saved
     */
    private Object getCellValue(Cell cell, DataType type, List<XSSFPictureData> pictures, int rowIndex) throws IOException {
        if (cell == null) return null;
        switch (type) {
            case STRING:
                return cell.getCellType() == CellType.STRING ? cell.getStringCellValue().trim() : null;
            case EMAIL:
                if (cell.getCellType() == CellType.STRING && isValidEmail(cell.getStringCellValue())) {
                    return cell.getStringCellValue().trim();
                }
                return null;
            case INTEGER:
                return cell.getCellType() == CellType.NUMERIC ? (int) cell.getNumericCellValue() : null;
            case DOUBLE:
                return cell.getCellType() == CellType.NUMERIC ? cell.getNumericCellValue() : null;
            case DATE:
                return cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell) ? cell.getDateCellValue() : null;
            case BOOLEAN:
                return cell.getCellType() == CellType.BOOLEAN ? cell.getBooleanCellValue() : null;
            case IMAGE:
                XSSFPictureData pic = findPictureForCell(cell, pictures);
                if (pic != null) {
                    String extension = getExtensionFromMimeType(pic.getMimeType());
                    String fileName = "photo_row_" + rowIndex + "_" + System.currentTimeMillis() + "." + extension;
                    if (fileService.saveFileImg(fileName, pic.getData())) {
                        logger.debug("Thread {}: Row {}: Saved image: {}",
                                Thread.currentThread().getName(), rowIndex + 1, fileName);
                        return fileService.getFilePath(fileName, FileService.IMAGE_FILE_PATH);
                    }
                    logger.warn("Thread {}: Row {}: Failed to save image: {}",
                            Thread.currentThread().getName(), rowIndex + 1, fileName);
                } else {
                    logger.debug("Thread {}: Row {}: No image found for cell at column {}",
                            Thread.currentThread().getName(), rowIndex + 1, cell.getColumnIndex());
                }
                return null;
            default:
                return null;
        }
    }

    /**
     * Finds the picture associated with a specific cell based on its anchor.
     *
     * @param cell     the cell to check for an associated picture
     * @param pictures the list of pictures in the Excel file
     * @return the associated XSSFPictureData, or null if none found
     */
    private XSSFPictureData findPictureForCell(Cell cell, List<XSSFPictureData> pictures) {
        Sheet sheet = cell.getSheet();
        if (sheet instanceof XSSFSheet) {
            XSSFDrawing drawing = ((XSSFSheet) sheet).getDrawingPatriarch();
            if (drawing != null) {
                for (XSSFShape shape : drawing) {
                    if (shape instanceof XSSFPicture) {
                        XSSFPicture picture = (XSSFPicture) shape;
                        XSSFClientAnchor anchor = picture.getClientAnchor();
                        if (anchor.getRow1() == cell.getRowIndex() && anchor.getCol1() == cell.getColumnIndex()) {
                            return picture.getPictureData();
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Determines the file extension based on the MIME type of the picture.
     *
     * @param mimeType the MIME type of the picture
     * @return the file extension (e.g., "png", "jpg")
     */
    private String getExtensionFromMimeType(String mimeType) {
        switch (mimeType) {
            case "image/png":
                return "png";
            case "image/jpeg":
                return "jpg";
            case "image/bmp":
                return "bmp";
            default:
                logger.warn("Thread {}: Unsupported MIME type: {}, defaulting to png",
                        Thread.currentThread().getName(), mimeType);
                return "png";
        }
    }

    /**
     * Validates an email address using a regex pattern.
     *
     * @param email the email address to validate
     * @return true if the email is valid, false otherwise
     */
    private boolean isValidEmail(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
        return email != null && email.matches(emailRegex);
    }

    /**
     * Represents the configuration for a column in the Excel file.
     */
    public static class ColumnConfig {
        private final int columnIndex;
        private final String name;
        private final DataType type;
        private final boolean required;

        /**
         * Constructs a ColumnConfig with the specified parameters.
         *
         * @param columnIndex the index of the column
         * @param name        the name of the column (used as key in result map)
         * @param type        the data type of the column
         * @param required    whether the column is required
         */
        public ColumnConfig(int columnIndex, String name, DataType type, boolean required) {
            this.columnIndex = columnIndex;
            this.name = name;
            this.type = type;
            this.required = required;
        }

        public int getColumnIndex() {
            return columnIndex;
        }

        public String getName() {
            return name;
        }

        public DataType getType() {
            return type;
        }

        public boolean isRequired() {
            return required;
        }
    }

    /**
     * Represents the data type of a column.
     */
    public enum DataType {
        STRING, INTEGER, DOUBLE, DATE, IMAGE, BOOLEAN, EMAIL
    }

    /**
     * Represents an error encountered during Excel processing.
     */
    public static class ProcessingError {
        private final int rowIndex;
        private final int columnIndex;
        private final String errorMessage;
        private final ErrorType errorType;

        /**
         * The type of processing error.
         */
        public enum ErrorType {
            MISSING_VALUE, INVALID_TYPE, IO_ERROR, OTHER
        }

        /**
         * Constructs a ProcessingError with the specified parameters.
         *
         * @param rowIndex     the row index (1-based)
         * @param columnIndex  the column index
         * @param errorMessage the error message
         * @param errorType    the type of error
         */
        public ProcessingError(int rowIndex, int columnIndex, String errorMessage, ErrorType errorType) {
            this.rowIndex = rowIndex;
            this.columnIndex = columnIndex;
            this.errorMessage = errorMessage;
            this.errorType = errorType;
        }

        public int getRowIndex() {
            return rowIndex;
        }

        public int getColumnIndex() {
            return columnIndex;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public ErrorType getErrorType() {
            return errorType;
        }
    }

    /**
     * Represents the result of processing an Excel file.
     */
    public static class ExcelProcessingResult {
        private final List<Map<String, Object>> data;
        private final List<ProcessingError> errors;

        /**
         * Constructs an ExcelProcessingResult with the specified data and errors.
         *
         * @param data   the processed data
         * @param errors the list of processing errors
         */
        public ExcelProcessingResult(List<Map<String, Object>> data, List<ProcessingError> errors) {
            this.data = data;
            this.errors = errors;
        }

        public List<Map<String, Object>> getData() {
            return data;
        }

        public List<ProcessingError> getErrors() {
            return errors;
        }
    }

    /**
     * Demo main method for testing ExcelService functionality.
     * Reads a sample Excel file with specified file name and directory.
     *
     * @param args command-line arguments: [fileName, uploadDir]
     */
    public static void main(String[] args) {
        String filename = "data2.xlsx";
        FileService fileService = new FileService((ServletContext) null);
        ExcelService excelService = new ExcelService(fileService);
        List<ColumnConfig> columnConfigs = Arrays.asList(
                new ColumnConfig(0, "Name", DataType.STRING, true),
                new ColumnConfig(1, "Email", DataType.EMAIL, true),
                new ColumnConfig(2, "Address", DataType.STRING, false),
                new ColumnConfig(3, "Salary", DataType.INTEGER, false),
                new ColumnConfig(4, "Description", DataType.STRING, false)
        );
        try {
            ExcelProcessingResult result = excelService.processExcelFile(filename, columnConfigs);
            System.out.println("Processed data: " + result.getData());
            System.out.println("Processing errors: " + result.getErrors());
        } catch (IOException e) {
            logger.error("Error processing Excel file: {}", e.getMessage());
        }
    }
}