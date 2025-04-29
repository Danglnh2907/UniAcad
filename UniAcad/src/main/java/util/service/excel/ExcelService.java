package util.service.excel;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.service.file.FileService;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

/**
 * ExcelService: Advanced Excel file processor with template generation, multi-sheet reading,
 * image handling, custom validation, and header inference. Optimized for large files using streaming.
 */
public class ExcelService {
    private static final Logger logger = LoggerFactory.getLogger(ExcelService.class);
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final int MAX_ROWS_PER_BATCH = 1000;
    private static final int MAX_SHEET_COLUMNS = 16384;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy");

    private final FileService fileService;

    public ExcelService(FileService fileService) {
        if (fileService == null) throw new IllegalArgumentException("FileService cannot be null");
        this.fileService = fileService;
    }

    public ExcelProcessingResult processExcelFile(String fileName, List<ColumnConfig> columnConfigs, int startRow, String sheetName, int headerRow) throws IOException {
        Objects.requireNonNull(fileName, "File name cannot be null or empty");
        if (fileName.trim().isEmpty()) throw new IllegalArgumentException("File name cannot be empty");

        try (InputStream is = fileService.getFileInputStream(fileName, FileService.FileType.EXCEL);
             XSSFWorkbook workbook = new XSSFWorkbook(is)) {

            XSSFSheet sheet = sheetName != null ? workbook.getSheet(sheetName) : workbook.getSheetAt(0);
            if (sheet == null) throw new IllegalArgumentException("Sheet not found: " + (sheetName != null ? sheetName : "first sheet"));

            startRow = startRow <= 0 ? 2 : startRow;
            headerRow = columnConfigs == null ? (headerRow <= 0 ? startRow - 1 : headerRow) : 0;
            if (startRow > sheet.getLastRowNum() + 1 || (headerRow > 0 && headerRow > sheet.getLastRowNum() + 1)) {
                throw new IllegalArgumentException("Start/Header row exceeds sheet size.");
            }
            if (headerRow > 0 && headerRow >= startRow) {
                throw new IllegalArgumentException("Header row must be before start row.");
            }

            List<ColumnConfig> effectiveConfigs = columnConfigs != null && !columnConfigs.isEmpty()
                    ? columnConfigs
                    : inferColumnConfigs(sheet.getRow(headerRow - 1));
            validateColumnConfigs(effectiveConfigs, sheet);

            Map<String, XSSFPictureData> pictureMap = preloadPictures(sheet);

            List<Map<String, Object>> data = new ArrayList<>();
            List<ProcessingError> errors = new ArrayList<>();
            int rowCount = 0;

            for (Row row : sheet) {
                if (row.getRowNum() < startRow - 1 || isRowEmpty(row)) continue;
                Map<String, Object> rowData = processRow(row, effectiveConfigs, pictureMap, errors);
                if (rowData != null) data.add(rowData);

                if (++rowCount % MAX_ROWS_PER_BATCH == 0) {
                    logger.debug("Processed {} rows, current memory: {} MB", rowCount, getMemoryUsage());
                    data = new ArrayList<>(data);
                }
            }

            logger.info("Processed {} rows with {} errors", rowCount, errors.size());
            return new ExcelProcessingResult(data, errors);
        }
    }

    public void generateExcelTemplate(String fileName, List<ColumnConfig> columnConfigs) throws IOException {
        if (columnConfigs == null || columnConfigs.isEmpty()) {
            throw new IllegalArgumentException("Column configurations cannot be null or empty");
        }
        validateColumnConfigs(columnConfigs, null);

        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

            SXSSFSheet sheet = workbook.createSheet("Template");

            // 🎨 Tạo style header
            XSSFCellStyle headerStyle = workbook.getXSSFWorkbook().createCellStyle();
            XSSFFont headerFont = workbook.getXSSFWorkbook().createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerFont.setFontHeightInPoints((short) 12);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            Drawing<?> drawing = sheet.createDrawingPatriarch();
            CreationHelper creationHelper = workbook.getXSSFWorkbook().getCreationHelper();

            // 📝 Tạo các dòng
            Row headerRow = sheet.createRow(0);
            Row instructionRow = sheet.createRow(1);

            for (ColumnConfig config : columnConfigs) {
                int colIdx = config.columnIndex;

                // Tên cột
                Cell headerCell = headerRow.createCell(colIdx);
                headerCell.setCellValue(config.name + (config.required ? " *" : ""));
                headerCell.setCellStyle(headerStyle);

                // Gán comment nếu có
                if (config.description != null && !config.description.isEmpty()) {
                    ClientAnchor anchor = creationHelper.createClientAnchor();
                    anchor.setCol1(colIdx);
                    anchor.setCol2(colIdx + 3);
                    anchor.setRow1(0);
                    anchor.setRow2(3);
                    Comment comment = drawing.createCellComment(anchor);
                    comment.setString(creationHelper.createRichTextString(config.description));
                    headerCell.setCellComment(comment);
                }

                // Ví dụ dữ liệu
                Cell sampleCell = instructionRow.createCell(colIdx);
                sampleCell.setCellValue("e.g. " + getSampleValue(config.type));
                sheet.trackColumnsForAutoSizing(List.of(colIdx));
                sheet.autoSizeColumn(colIdx);

                // 📋 Dropdown nếu có
                if (config.hasDropdown()) {
                    DataValidationHelper helper = sheet.getDataValidationHelper();
                    DataValidationConstraint constraint = helper.createExplicitListConstraint(
                            config.dropdownValues.toArray(new String[0])
                    );
                    CellRangeAddressList range = new CellRangeAddressList(2, 1000, colIdx, colIdx);
                    DataValidation validation = helper.createValidation(constraint, range);
                    validation.setShowErrorBox(true);
                    sheet.addValidationData(validation);
                }
            }

            // 📌 Freeze dòng tiêu đề
            sheet.createFreezePane(0, 2);

            // 💾 Ghi file
            workbook.write(bos);
            if (!fileService.saveFile(fileName, bos.toByteArray(), FileService.FileType.EXCEL, false)) {
                throw new IOException("Failed to save template via FileService");
            }

            logger.info("Template created: {}", fileName);
        }
    }

    private List<ColumnConfig> inferColumnConfigs(Row headerRow) {
        if (headerRow == null) {
            logger.warn("No header row found, returning empty configs");
            return Collections.emptyList();
        }

        List<ColumnConfig> configs = new ArrayList<>();
        for (Cell cell : headerRow) {
            String rawName = cell.getCellType() == CellType.STRING ? cell.getStringCellValue().trim() : "Column" + cell.getColumnIndex();
            if (rawName.isEmpty()) rawName = "Column" + cell.getColumnIndex();
            // Loại bỏ ký tự '*' và khoảng trắng thừa trong tên cột
            String name = rawName.replaceAll("[\\s\\*]+", "");
            DataType type = inferDataType(cell, name);
            // Đánh dấu các cột có '*' là required
            boolean required = rawName.contains("*");
            configs.add(new ColumnConfig(cell.getColumnIndex(), name, type, required, null));
        }
        logger.info("Inferred {} column configurations from header", configs.size());
        return configs;
    }

    private DataType inferDataType(Cell cell, String columnName) {
        if (cell == null) return DataType.STRING;

        // Dựa vào tên cột để suy ra kiểu dữ liệu nếu có gợi ý
        String normalizedName = columnName.toLowerCase();
        if (normalizedName.contains("email")) return DataType.EMAIL;
        if (normalizedName.contains("gender") || normalizedName.contains("sex")) return DataType.BOOLEAN;
        if (normalizedName.contains("dob") || normalizedName.contains("date") || normalizedName.contains("birth")) return DataType.DATE;

        // Dựa vào giá trị ô
        switch (cell.getCellType()) {
            case STRING:
                String value = cell.getStringCellValue().trim();
                if (EMAIL_PATTERN.matcher(value).matches()) return DataType.EMAIL;
                if (value.equalsIgnoreCase("TRUE") || value.equalsIgnoreCase("FALSE")) return DataType.BOOLEAN;
                try {
                    DATE_FORMAT.parse(value);
                    return DataType.DATE;
                } catch (ParseException e) {
                    return DataType.STRING;
                }
            case NUMERIC:
                return DateUtil.isCellDateFormatted(cell) ? DataType.DATE :
                        cell.getNumericCellValue() == Math.floor(cell.getNumericCellValue()) ? DataType.INTEGER : DataType.DOUBLE;
            case BOOLEAN:
                return DataType.BOOLEAN;
            default:
                return DataType.STRING;
        }
    }

    private Map<String, XSSFPictureData> preloadPictures(XSSFSheet sheet) {
        Map<String, XSSFPictureData> map = new HashMap<>();
        XSSFDrawing drawing = sheet.getDrawingPatriarch();
        if (drawing != null) {
            for (XSSFShape shape : drawing.getShapes()) {
                if (shape instanceof XSSFPicture) {
                    XSSFPicture pic = (XSSFPicture) shape;
                    XSSFClientAnchor anchor = pic.getClientAnchor();
                    for (int r = anchor.getRow1(); r <= anchor.getRow2(); r++) {
                        for (int c = anchor.getCol1(); c <= anchor.getCol2(); c++) {
                            map.put(r + "_" + c, pic.getPictureData());
                        }
                    }
                }
            }
        }
        logger.debug("Preloaded {} pictures", map.size());
        return map;
    }

    private boolean isRowEmpty(Row row) {
        if (row == null) return true;
        for (Cell cell : row) {
            if (cell != null && (cell.getCellType() != CellType.BLANK || (cell.getCellType() == CellType.STRING && !cell.getStringCellValue().trim().isEmpty()))) {
                return false;
            }
        }
        return true;
    }

    private Map<String, Object> processRow(Row row, List<ColumnConfig> configs, Map<String, XSSFPictureData> pictures, List<ProcessingError> errors) {
        Map<String, Object> rowData = new LinkedHashMap<>();
        boolean valid = false;

        for (ColumnConfig config : configs) {
            try {
                Cell cell = row.getCell(config.columnIndex);
                Object value = extractCellValue(cell, config, row.getRowNum(), pictures);
                if (config.validator != null) config.validator.validate(value);
                if (value != null || !config.required) {
                    rowData.put(config.name, value);
                    valid = true;
                } else {
                    errors.add(new ProcessingError(row.getRowNum() + 1, config.columnIndex, "Missing required value for: " + config.name));
                }
            } catch (ValidationException ve) {
                errors.add(new ProcessingError(row.getRowNum() + 1, config.columnIndex, ve.getMessage()));
            } catch (Exception ex) {
                errors.add(new ProcessingError(row.getRowNum() + 1, config.columnIndex, "Error processing field: " + config.name + " - " + ex.getMessage()));
            }
        }
        return valid ? rowData : null;
    }

    private Object extractCellValue(Cell cell, ColumnConfig config, int rowIdx, Map<String, XSSFPictureData> pictures) throws IOException {
        if (cell == null) return null;

        switch (config.type) {
            case STRING:
                return cell.getCellType() == CellType.STRING ? cell.getStringCellValue().trim() : null;
            case EMAIL:
                String email = cell.getCellType() == CellType.STRING ? cell.getStringCellValue().trim() : null;
                return email != null && EMAIL_PATTERN.matcher(email).matches() ? email : null;
            case INTEGER:
                return cell.getCellType() == CellType.NUMERIC ? (int) cell.getNumericCellValue() : null;
            case DOUBLE:
                return cell.getCellType() == CellType.NUMERIC ? cell.getNumericCellValue() : null;
            case DATE:
                if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue();
                } else if (cell.getCellType() == CellType.STRING) {
                    String dateStr = cell.getStringCellValue().trim();
                    try {
                        return DATE_FORMAT.parse(dateStr);
                    } catch (ParseException e) {
                        logger.debug("Invalid date format at row {}, column {}: {}", rowIdx + 1, config.columnIndex, dateStr);
                        return null;
                    }
                }
                return null;
            case BOOLEAN:
                if (cell.getCellType() == CellType.BOOLEAN) {
                    return cell.getBooleanCellValue();
                } else if (cell.getCellType() == CellType.STRING) {
                    String boolStr = cell.getStringCellValue().trim().toLowerCase();
                    if ("true".equals(boolStr) || "1".equals(boolStr)) return true;
                    if ("false".equals(boolStr) || "0".equals(boolStr)) return false;
                    logger.debug("Invalid boolean format at row {}, column {}: {}", rowIdx + 1, config.columnIndex, boolStr);
                    return null;
                }
                return null;
            case IMAGE:
                String key = rowIdx + "_" + cell.getColumnIndex();
                XSSFPictureData picData = pictures.get(key);
                if (picData != null) {
                    String ext = getImageExtension(picData.getMimeType());
                    String imgName = "img_" + rowIdx + "_" + System.currentTimeMillis() + "." + ext;
                    if (fileService.saveFile(imgName, picData.getData(), FileService.FileType.IMAGE, false)) {
                        return fileService.getAbsolutePath(imgName, FileService.FileType.IMAGE);
                    }
                    throw new IOException("Failed to save image: " + imgName);
                }
                return null;
            default:
                return null;
        }
    }

    private String getImageExtension(String mimeType) {
        switch (mimeType) {
            case "image/png": return "png";
            case "image/jpeg": return "jpg";
            case "image/bmp": return "bmp";
            case "image/gif": return "gif";
            default:
                logger.warn("Unsupported MIME type: {}, defaulting to png", mimeType);
                return "png";
        }
    }

    private void validateColumnConfigs(List<ColumnConfig> configs, Sheet sheet) {
        if (configs == null || configs.isEmpty()) return;
        Set<String> names = new HashSet<>();
        Set<Integer> indices = new HashSet<>();
        for (ColumnConfig config : configs) {
            if (!names.add(config.name)) throw new IllegalArgumentException("Duplicate column name: " + config.name);
            if (!indices.add(config.columnIndex) || config.columnIndex < 0 || config.columnIndex >= MAX_SHEET_COLUMNS) {
                throw new IllegalArgumentException("Invalid column index: " + config.columnIndex);
            }
        }
    }

    private String getSampleValue(DataType type) {
        switch (type) {
            case STRING: return "Sample Text";
            case EMAIL: return "example@domain.com";
            case INTEGER: return "123";
            case DOUBLE: return "123.45";
            case DATE: return "01/01/2023";
            case BOOLEAN: return "TRUE";
            case IMAGE: return "Insert image here";
            default: return "";
        }
    }

    private long getMemoryUsage() {
        Runtime rt = Runtime.getRuntime();
        return (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
    }

    // Inner classes
    public static class ColumnConfig {
        public final int columnIndex;
        public final String name;
        public final DataType type;
        public final boolean required;
        public final ColumnValidator validator;
        public final List<String> dropdownValues;
        public final String description; // 💬 Mô tả (tooltip)

        public ColumnConfig(int columnIndex, String name, DataType type, boolean required, ColumnValidator validator) {
            this(columnIndex, name, type, required, validator, null, null);
        }

        public ColumnConfig(int columnIndex, String name, DataType type, boolean required, ColumnValidator validator, List<String> dropdownValues) {
            this(columnIndex, name, type, required, validator, dropdownValues, null);
        }

        public ColumnConfig(int columnIndex, String name, DataType type, boolean required, ColumnValidator validator, List<String> dropdownValues, String description) {
            this.columnIndex = columnIndex;
            this.name = name;
            this.type = type;
            this.required = required;
            this.validator = validator;
            this.dropdownValues = dropdownValues;
            this.description = description;
        }

        public boolean hasDropdown() {
            return dropdownValues != null && !dropdownValues.isEmpty();
        }
    }

    public enum DataType {
        STRING, INTEGER, DOUBLE, DATE, BOOLEAN, EMAIL, IMAGE
    }

    @FunctionalInterface
    public interface ColumnValidator {
        void validate(Object value) throws ValidationException;
    }

    public static class ValidationException extends Exception {
        public ValidationException(String message) {
            super(message);
        }
    }

    public static class ProcessingError {
        public final int rowIndex;
        public final int columnIndex;
        public final String message;

        public ProcessingError(int rowIndex, int columnIndex, String message) {
            this.rowIndex = rowIndex;
            this.columnIndex = columnIndex;
            this.message = message;
        }
    }

    public static class ExcelProcessingResult {
        public final List<Map<String, Object>> data;
        public final List<ProcessingError> errors;

        public ExcelProcessingResult(List<Map<String, Object>> data, List<ProcessingError> errors) {
            this.data = data;
            this.errors = errors;
        }
    }
}