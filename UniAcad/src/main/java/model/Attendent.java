package model;

public class Attendent {
    private int status; // INT
    private String studentId; // CHAR(8), Foreign Key to Student
    private int slotNumber; // INT, Foreign Key to Slot
    private int courseId; // INT, Foreign Key to Slot

    // Constructor
    public Attendent() {
    }

    public Attendent(int status, String studentId, int slotNumber, int courseId) {
        this.status = status;
        this.studentId = studentId;
        this.slotNumber = slotNumber;
        this.courseId = courseId;
    }

    // Getters and Setters
    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public int getSlotNumber() {
        return slotNumber;
    }

    public void setSlotNumber(int slotNumber) {
        this.slotNumber = slotNumber;
    }

    public int getCourseId() {
        return courseId;
    }

    public void setCourseId(int courseId) {
        this.courseId = courseId;
    }
}