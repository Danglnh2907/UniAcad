package model;

public class Curriculum {
    private String curriculumId; // VARCHAR
    private String curriculumName; // VARCHAR
    private String majorId; // CHAR(2), Foreign Key to Major

    // Constructor
    public Curriculum() {
    }

    public Curriculum(String curriculumId, String curriculumName, String majorId) {
        this.curriculumId = curriculumId;
        this.curriculumName = curriculumName;
        this.majorId = majorId;
    }

    // Getters and Setters
    public String getCurriculumId() {
        return curriculumId;
    }

    public void setCurriculumId(String curriculumId) {
        this.curriculumId = curriculumId;
    }

    public String getCurriculumName() {
        return curriculumName;
    }

    public void setCurriculumName(String curriculumName) {
        this.curriculumName = curriculumName;
    }

    public String getMajorId() {
        return majorId;
    }

    public void setMajorId(String majorId) {
        this.majorId = majorId;
    }
}