package util.service.excel;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.service.file.FileService;

import java.io.*;
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

        SXSSFWorkbook workbook = null;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            workbook = new SXSSFWorkbook(100);
            SXSSFSheet sheet = workbook.createSheet("Template");

            for (ColumnConfig config : columnConfigs) {
                sheet.trackColumnsForAutoSizing(Arrays.asList(config.columnIndex));
            }

            Row headerRow = sheet.createRow(0);
            for (ColumnConfig config : columnConfigs) {
                Cell cell = headerRow.createCell(config.columnIndex);
                cell.setCellValue(config.name);
                sheet.autoSizeColumn(config.columnIndex);
            }

            Row sampleRow = sheet.createRow(1);
            for (ColumnConfig config : columnConfigs) {
                Cell cell = sampleRow.createCell(config.columnIndex);
                cell.setCellValue(getSampleValue(config.type));
                sheet.autoSizeColumn(config.columnIndex);
            }

            workbook.write(bos);
            if (!fileService.saveFile(fileName, bos.toByteArray(), FileService.FileType.EXCEL, true)) {
                throw new IOException("Failed to save template via FileService");
            }
            logger.info("Template generated: {}", fileName);
        } finally {
            if (workbook != null) {
                workbook.dispose();
            }
        }
    }

    private List<ColumnConfig> inferColumnConfigs(Row headerRow) {
        if (headerRow == null) {
            logger.warn("No header row found, returning empty configs");
            return Collections.emptyList();
        }

        List<ColumnConfig> configs = new ArrayList<>();
        for (Cell cell : headerRow) {
            String name = cell.getCellType() == CellType.STRING ? cell.getStringCellValue().trim() : "Column" + cell.getColumnIndex();
            if (name.isEmpty()) name = "Column" + cell.getColumnIndex();
            DataType type = inferDataType(cell);
            configs.add(new ColumnConfig(cell.getColumnIndex(), name, type, false, null));
        }
        logger.info("Inferred {} column configurations from header", configs.size());
        return configs;
    }

    private DataType inferDataType(Cell cell) {
        if (cell == null) return DataType.STRING;
        switch (cell.getCellType()) {
            case STRING:
                return EMAIL_PATTERN.matcher(cell.getStringCellValue()).matches() ? DataType.EMAIL : DataType.STRING;
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
                return cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell) ? cell.getDateCellValue() : null;
            case BOOLEAN:
                return cell.getCellType() == CellType.BOOLEAN ? cell.getBooleanCellValue() : null;
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
            case DATE: return "1/1/2023";
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

        public ColumnConfig(int columnIndex, String name, DataType type, boolean required, ColumnValidator validator) {
            this.columnIndex = columnIndex;
            this.name = name;
            this.type = type;
            this.required = required;
            this.validator = validator;
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
