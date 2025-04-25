package model;

public class Teacher {
    private String teacherId; // VARCHAR
    private String teacherEmail; // VARCHAR
    private String firstName; // VARCHAR
    private String midleName; // VARCHAR
    private String lastName; // VARCHAR
    private String teacherPhone; // VARCHAR
    private String departmentId; // CHAR(2), Foreign Key to Department

    // Constructor
    public Teacher() {
    }

    public Teacher(String teacherId, String teacherEmail, String firstName, String midleName, String lastName, String teacherPhone, String departmentId) {
        this.teacherId = teacherId;
        this.teacherEmail = teacherEmail;
        this.firstName = firstName;
        this.midleName = midleName;
        this.lastName = lastName;
        this.teacherPhone = teacherPhone;
        this.departmentId = departmentId;
    }

    // Getters and Setters
    public String getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(String teacherId) {
        this.teacherId = teacherId;
    }

    public String getTeacherEmail() {
        return teacherEmail;
    }

    public void setTeacherEmail(String teacherEmail) {
        this.teacherEmail = teacherEmail;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getMidleName() {
        return midleName;
    }

    public void setMidleName(String midleName) {
        this.midleName = midleName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getTeacherPhone() {
        return teacherPhone;
    }

    public void setTeacherPhone(String teacherPhone) {
        this.teacherPhone = teacherPhone;
    }

    public String getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(String departmentId) {
        this.departmentId = departmentId;
    }
}