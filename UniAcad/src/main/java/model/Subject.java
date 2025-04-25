package model;

public class Subject {
    private String subjectId; // CHAR(7)
    private String subjectName; // VARCHAR
    private int credits; // INT
    private String departmentId; // CHAR(2), Foreign Key to Department

    // Constructor
    public Subject() {
    }

    public Subject(String subjectId, String subjectName, int credits, String departmentId) {
        this.subjectId = subjectId;
        this.subjectName = subjectName;
        this.credits = credits;
        this.departmentId = departmentId;
    }

    // Getters and Setters
    public String getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(String subjectId) {
        this.subjectId = subjectId;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public int getCredits() {
        return credits;
    }

    public void setCredits(int credits) {
        this.credits = credits;
    }

    public String getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(String departmentId) {
        this.departmentId = departmentId;
    }
}