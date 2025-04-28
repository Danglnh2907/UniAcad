package model.datasupport;

import lombok.Getter;

@Getter
public class MarkReport {
    // Getter & Setter
    private final String studentId;
    private final String studentName;
    private final String subjectName;
    private final double mark;
    private final String status; // "Pass" or "Fail"

    public MarkReport(String studentId, String studentName, String subjectName, double mark, String status) {
        this.studentId = studentId;
        this.studentName = studentName;
        this.subjectName = subjectName;
        this.mark = mark;
        this.status = status;
    }

}
