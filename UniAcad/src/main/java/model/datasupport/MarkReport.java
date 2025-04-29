package model.datasupport;

import lombok.Getter;

import java.util.Map;
@Getter
public class MarkReport {
    private String studentId;
    private String subjectId;
    private String subjectName;
    private Map<String, Double> marks;
    private double averageMark;

    public MarkReport(String studentId, String subjectId, String subjectName, Map<String, Double> marks, double averageMark) {
        this.studentId = studentId;
        this.subjectId = subjectId;
        this.subjectName = subjectName;
        this.marks = marks;
        this.averageMark = averageMark;
    }
}
