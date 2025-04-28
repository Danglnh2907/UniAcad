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

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public int getAbsentCount() {
        return absentCount;
    }

    public void setAbsentCount(int absentCount) {
        this.absentCount = absentCount;
    }

    public int getTotalSlots() {
        return totalSlots;
    }

    public void setTotalSlots(int totalSlots) {
        this.totalSlots = totalSlots;
    }

    public double getAbsentRate() {
        return absentRate;
    }

    public void setAbsentRate(double absentRate) {
        this.absentRate = absentRate;
    }

    public Double getMark() {
        return mark;
    }

    public void setMark(Double mark) {
        this.mark = mark;
    }

    public String getWarningType() {
        return warningType;
    }

    public void setWarningType(String warningType) {
        this.warningType = warningType;
    }

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
