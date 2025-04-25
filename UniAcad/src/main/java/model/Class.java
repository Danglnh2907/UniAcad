package model;

public class Class {
    private String classId; // CHAR(6)

    // Constructor
    public Class() {
    }

    public Class(String classId) {
        this.classId = classId;
    }

    // Getters and Setters
    public String getClassId() {
        return classId;
    }

    public void setClassId(String classId) {
        this.classId = classId;
    }
}