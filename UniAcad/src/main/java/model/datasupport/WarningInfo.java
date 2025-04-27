package model.datasupport;

public class WarningInfo {
    private String studentId;
    private String studentName;
    private String subjectName;
    private int absentCount;
    private int totalSlots;
    private double absentRate;
    private Double mark;
    private String warningType;

    // Getters và Setters

    // Optional: Constructor
    public WarningInfo(String studentId, String studentName, String subjectName,
                       int absentCount, int totalSlots, double absentRate,
                       Double mark, String warningType) {
        this.studentId = studentId;
        this.studentName = studentName;
        this.subjectName = subjectName;
        this.absentCount = absentCount;
        this.totalSlots = totalSlots;
        this.absentRate = absentRate;
        this.mark = mark;
        this.warningType = warningType;
    }

    public WarningInfo() {}
}
