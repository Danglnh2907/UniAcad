package util.service.excel;

import util.service.file.FileService;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Demo for using ExcelService: generate template, read Excel file, validate data.
 */
public class Main {
    public static void main(String[] args) {
        try {
            FileService fileService = new FileService("");
            ExcelService excelService = new ExcelService(fileService);

            // Define column configurations
            List<ExcelService.ColumnConfig> columnConfigs = Arrays.asList(
                    new ExcelService.ColumnConfig(0, "Name", ExcelService.DataType.STRING, true,
                            value -> {
                                if (value == null || ((String) value).trim().isEmpty()) {
                                    throw new ExcelService.ValidationException("Name cannot be empty.");
                                }
                            }),
                    new ExcelService.ColumnConfig(1, "Email", ExcelService.DataType.EMAIL, false, null),
                    new ExcelService.ColumnConfig(2, "BirthDate", ExcelService.DataType.DATE, false, null),
                    new ExcelService.ColumnConfig(3, "Photo", ExcelService.DataType.IMAGE, false, null)
            );

            // Step 1: Generate Excel Template
//            String templateFileName = "student_template.xlsx";
//            excelService.generateExcelTemplate(templateFileName, columnConfigs);
//            System.out.println("✅ Template generated: " + templateFileName);

            // Step 2: Read and validate data from an Excel file
            String excelFileName = "data.xlsx"; // Ensure this file exists
            File excelFile = new File(fileService.getFilePath(excelFileName, "xlsx"));
            if (!excelFile.exists()) {
                System.out.println("⚠️ Excel file not found: " + excelFileName);
                System.out.println("Please create and fill the file based on the generated template.");
                return;
            }

            ExcelService.ExcelProcessingResult result = excelService.processExcelFile(
                    excelFileName, columnConfigs, 2, null, 1 // Start from row 2, header at row 1
            );

            // Step 3: Print Data
            System.out.println("\n📄 Extracted Data:");
            if (result.data.isEmpty()) {
                System.out.println("No data extracted.");
            } else {
                for (Map<String, Object> row : result.data) {
                    System.out.println(row);
                }
            }

            // Step 4: Print Errors
            if (!result.errors.isEmpty()) {
                System.out.println("\n⚠️ Validation Errors:");
                for (ExcelService.ProcessingError error : result.errors) {
                    System.out.printf("- Row %d, Column %d: %s%n", error.rowIndex, error.columnIndex, error.message);
                }
            } else {
                System.out.println("\n✅ No validation errors.");
            }

        } catch (FileNotFoundException e) {
            System.err.println("🔥 File not found: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("🔥 I/O error: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println("🔥 Invalid input: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("🔥 Unexpected error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}