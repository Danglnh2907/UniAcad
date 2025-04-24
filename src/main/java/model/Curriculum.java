package model;

public class Curriculum {
    private String curriculumId;
    private String curriculumName;
    private String majorID;
    private Major major;

    public Curriculum(String curriculumId, String curriculumName, String majorID) {
        this.curriculumId = curriculumId;
        this.curriculumName = curriculumName;
        this.majorID = majorID;
    }

    public String getCurriculumId() {
        return curriculumId;
    }

    public void setCurriculumId(String curriculumId) {
        this.curriculumId = curriculumId;
    }

    public Major getMajor() {
        return major;
    }

    public void setMajor(Major major) {
        this.major = major;
    }

    public String getMajorID() {
        return majorID;
    }

    public void setMajorID(String majorID) {
        this.majorID = majorID;
    }

    public String getCurriculumName() {
        return curriculumName;
    }

    public void setCurriculumName(String curriculumName) {
        this.curriculumName = curriculumName;
    }
}
