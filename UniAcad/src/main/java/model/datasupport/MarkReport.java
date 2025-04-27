package model.datasupport;

public class MarkReport {
    private String studentId;
    private String studentName;
    private String subjectName;
    private double mark;
    private String status; // "Pass" or "Fail"

    public MarkReport(String studentId, String studentName, String subjectName, double mark, String status) {
        this.studentId = studentId;
        this.studentName = studentName;
        this.subjectName = subjectName;
        this.mark = mark;
        this.status = status;
    }

    // Getter & Setter
    public String getStudentId() { return studentId; }
    public String getStudentName() { return studentName; }
    public String getSubjectName() { return subjectName; }
    public double getMark() { return mark; }
    public String getStatus() { return status; }
}
