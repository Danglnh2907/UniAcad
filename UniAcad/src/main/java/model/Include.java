package model;

public class Include {
    private int semester; // INT
    private String curriculumId; // VARCHAR, Foreign Key to Curriculum
    private String subjectId; // CHAR(7), Foreign Key to Subject

    // Constructor
    public Include() {
    }

    public Include(int semester, String curriculumId, String subjectId) {
        this.semester = semester;
        this.curriculumId = curriculumId;
        this.subjectId = subjectId;
    }

    // Getters and Setters
    public int getSemester() {
        return semester;
    }

    public void setSemester(int semester) {
        this.semester = semester;
    }

    public String getCurriculumId() {
        return curriculumId;
    }

    public void setCurriculumId(String curriculumId) {
        this.curriculumId = curriculumId;
    }

    public String getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(String subjectId) {
        this.subjectId = subjectId;
    }
}