package model.datasupport;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class WarningInfo {
    private String studentId;
    private String studentName;
    private String subjectName;
    private int absentCount;
    private int totalSlots;
    private double absentRate;
    private String warningType;
    private Double feeMoney;

    // Constructor cho cảnh báo điểm/vắng mặt
    public WarningInfo(String studentId, String studentName, String subjectName,
                       int absentCount, int totalSlots, double absentRate,
                       Double mark, String warningType) {
        this.studentId = studentId;
        this.studentName = studentName;
        this.subjectName = subjectName;
        this.absentCount = absentCount;
        this.totalSlots = totalSlots;
        this.absentRate = absentRate;
        this.warningType = warningType;
    }

    // Constructor cho cảnh báo nợ học phí
    public WarningInfo(String studentId, String studentName, Double feeMoney, String warningType) {
        this.studentId = studentId;
        this.studentName = studentName;
        this.feeMoney = feeMoney;
        this.warningType = warningType;
    }

    public WarningInfo() {}
}
