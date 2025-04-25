package model;

public class Study {
    private String classId; // CHAR(6), Foreign Key to Class
    private int courseId; // INT, Foreign Key to Course

    // Constructor
    public Study() {
    }

    public Study(String classId, int courseId) {
        this.classId = classId;
        this.courseId = courseId;
    }

    // Getters and Setters
    public String getClassId() {
        return classId;
    }

    public void setClassId(String classId) {
        this.classId = classId;
    }

    public int getCourseId() {
        return courseId;
    }

    public void setCourseId(int courseId) {
        this.courseId = courseId;
    }
}