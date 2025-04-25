package model;

public class Group {
    private String studentId; // CHAR(8), Foreign Key to Student
    private String classId; // CHAR(6), Foreign Key to Class

    // Constructor
    public Group() {
    }

    public Group(String studentId, String classId) {
        this.studentId = studentId;
        this.classId = classId;
    }

    // Getters and Setters
    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getClassId() {
        return classId;
    }

    public void setClassId(String classId) {
        this.classId = classId;
    }
}