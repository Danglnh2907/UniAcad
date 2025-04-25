package model;

public class Staff {
    private String staffId; // VARCHAR
    private String lastName; // VARCHAR
    private String midleName; // VARCHAR
    private String fullName; // VARCHAR
    private String staffEmail; // VARCHAR
    private String staffPhone; // VARCHAR

    // Constructor
    public Staff() {
    }

    public Staff(String staffId, String lastName, String midleName, String fullName, String staffEmail, String staffPhone) {
        this.staffId = staffId;
        this.lastName = lastName;
        this.midleName = midleName;
        this.fullName = fullName;
        this.staffEmail = staffEmail;
        this.staffPhone = staffPhone;
    }

    // Getters and Setters
    public String getStaffId() {
        return staffId;
    }

    public void setStaffId(String staffId) {
        this.staffId = staffId;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getMidleName() {
        return midleName;
    }

    public void setMidleName(String midleName) {
        this.midleName = midleName;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getStaffEmail() {
        return staffEmail;
    }

    public void setStaffEmail(String staffEmail) {
        this.staffEmail = staffEmail;
    }

    public String getStaffPhone() {
        return staffPhone;
    }

    public void setStaffPhone(String staffPhone) {
        this.staffPhone = staffPhone;
    }
}