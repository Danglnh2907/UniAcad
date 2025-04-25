package model;

public class Grade {
    private Integer fe; // INT, nullable
    private Integer onGoing; // INT, nullable
    private int courseId; // INT, Foreign Key to Course
    private String studentId; // CHAR(8), Foreign Key to Student

    // Constructor
    public Grade() {
    }

    public Grade(Integer fe, Integer onGoing, int courseId, String studentId) {
        this.fe = fe;
        this.onGoing = onGoing;
        this.courseId = courseId;
        this.studentId = studentId;
    }

    // Getters and Setters
    public Integer getFe() {
        return fe;
    }

    public void setFe(Integer fe) {
        this.fe = fe;
    }

    public Integer getOnGoing() {
        return onGoing;
    }

    public void setOnGoing(Integer onGoing) {
        this.onGoing = onGoing;
    }

    public int getCourseId() {
        return courseId;
    }

    public void setCourseId(int courseId) {
        this.courseId = courseId;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }
}