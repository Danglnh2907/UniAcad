package model;

public class Course {
    private int courseId; // INT
    private String subjectId; // CHAR(7), Foreign Key to Subject
    private String termId; // CHAR(4), Foreign Key to Term

    // Constructor
    public Course() {
    }

    public Course(int courseId, String subjectId, String termId) {
        this.courseId = courseId;
        this.subjectId = subjectId;
        this.termId = termId;
    }

    // Getters and Setters
    public int getCourseId() {
        return courseId;
    }

    public void setCourseId(int courseId) {
        this.courseId = courseId;
    }

    public String getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(String subjectId) {
        this.subjectId = subjectId;
    }

    public String getTermId() {
        return termId;
    }

    public void setTermId(String termId) {
        this.termId = termId;
    }
}
