package util.service.excel;

import model.database.Curriculum;
import model.database.Student;
import util.service.file.FileService;
import util.service.excel.ExcelService.ColumnConfig;
import util.service.excel.ExcelService.DataType;
import util.service.excel.ExcelService.ExcelProcessingResult;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class StudentExcelService {
    private final ExcelService excelService;

    public StudentExcelService(FileService fileService) {
        this.excelService = new ExcelService(fileService);
    }

    /**
     * Sinh file Excel template để nhập Student
     */
    public void generateTemplateAddStudent(String fileName) throws IOException {
        List<ColumnConfig> configs = Arrays.asList(
                new ColumnConfig(0, "StudentID", DataType.STRING, true, null),
                new ColumnConfig(1, "StudentName", DataType.STRING, true, null),
                new ColumnConfig(2, "StudentSSN", DataType.STRING, true, null),
                new ColumnConfig(3, "StudentEmail", DataType.EMAIL, true, null),
                new ColumnConfig(4, "StudentPhone", DataType.STRING, true, null),
                new ColumnConfig(5, "CurriculumID", DataType.STRING, true, null),
                new ColumnConfig(6, "StudentGender", DataType.BOOLEAN, true, null),
                new ColumnConfig(7, "Address", DataType.STRING, false, null),
                new ColumnConfig(8, "StudentDoB", DataType.DATE, true, null)
        );
        excelService.generateExcelTemplate(fileName, configs);
    }

    /**
     * Đọc file Excel nhập dữ liệu Student
     */
    public List<Student> readStudentsFromExcel(String fileName) throws IOException {
        ExcelProcessingResult result = excelService.processExcelFile(
                fileName,
                null,   // tự infer column
                2,      // start từ dòng 2 (bỏ dòng header 1)
                null,   // sheet đầu tiên
                1       // dòng header 1
        );

        return result.data.stream().map(row -> {
            Student student = new Student();
            student.setStudentID((String) row.get("StudentID"));
            student.setStudentName((String) row.get("StudentName"));
            student.setStudentSSN((String) row.get("StudentSSN"));
            student.setStudentEmail((String) row.get("StudentEmail"));
            student.setStudentPhone((String) row.get("StudentPhone"));

            // 🛠 Gán Curriculum Entity:
            Curriculum curriculum = new Curriculum();
            curriculum.setCurriculumID((String) row.get("CurriculumID"));
            student.setCurriculumID(curriculum);

            student.setStudentGender(Boolean.TRUE.equals(row.get("StudentGender")));
            student.setAddress((String) row.get("Address"));

            if (row.get("StudentDoB") instanceof Date) {
                Date dob = (Date) row.get("StudentDoB");
                student.setStudentDoB(dob.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate());
            }

            // 🛠 StudentStatus mặc định là 0 (Enrolled)
            student.setStudentStatus(0);

            return student;
        }).collect(Collectors.toList());
    }

    public static void main(String[] args) {
        // Test the StudentExcelService
        FileService fileService = new FileService("");
        StudentExcelService studentExcelService = new StudentExcelService(fileService);

        try {
            // Generate template
            String templateFileName = "StudentTemplate - Copy.xlsx";
//            studentExcelService.generateTemplateAddStudent(templateFileName);
//            System.out.println("Template generated: " + templateFileName);

            // Read students from Excel
            List<Student> students = studentExcelService.readStudentsFromExcel(templateFileName);
            System.out.println("Read students: " + students.size());
            for (Student student : students) {
                System.out.println(student.getStudentID());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
