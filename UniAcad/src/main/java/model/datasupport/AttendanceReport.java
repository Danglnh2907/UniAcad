package model.datasupport;

public class AttendanceReport {
    private String studentId;
    private String studentName;
    private String subjectName;
    private int totalSlots;
    private int absentSlots;
    private double absentRate;

    public AttendanceReport(String studentId, String studentName, String subjectName,
                            int totalSlots, int absentSlots, double absentRate) {
        this.studentId = studentId;
        this.studentName = studentName;
        this.subjectName = subjectName;
        this.totalSlots = totalSlots;
        this.absentSlots = absentSlots;
        this.absentRate = absentRate;
    }

    // Getters
    public String getStudentId() { return studentId; }
    public String getStudentName() { return studentName; }
    public String getSubjectName() { return subjectName; }
    public int getTotalSlots() { return totalSlots; }
    public int getAbsentSlots() { return absentSlots; }
    public double getAbsentRate() { return absentRate; }
}
