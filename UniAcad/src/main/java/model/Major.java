package model;

public class Major {
    private String majorId; // CHAR(2)
    private String majorName; // VARCHAR

    // Constructor
    public Major() {
    }

    public Major(String majorId, String majorName) {
        this.majorId = majorId;
        this.majorName = majorName;
    }

    // Getters and Setters
    public String getMajorId() {
        return majorId;
    }

    public void setMajorId(String majorId) {
        this.majorId = majorId;
    }

    public String getMajorName() {
        return majorName;
    }

    public void setMajorName(String majorName) {
        this.majorName = majorName;
    }
}