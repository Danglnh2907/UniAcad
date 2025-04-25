package model;

import java.util.Date;

public class Student {
    private String studentId; // CHAR(8)
    private String studentEmail; // VARCHAR
    private String lastName; // VARCHAR
    private String midleName; // VARCHAR
    private String firstName; // VARCHAR
    private Date studentDoB; // DATE
    private int studentGender; // INT
    private String studentSSN; // VARCHAR
    private String district; // VARCHAR
    private String province; // VARCHAR
    private String detail; // VARCHAR
    private String town; // VARCHAR
    private String studentPhone; // VARCHAR
    private String curriculumId; // VARCHAR, Foreign Key to Curriculum

    // Constructor
    public Student() {
    }

    public Student(String studentId, String studentEmail, String lastName, String midleName, String firstName,
                   Date studentDoB, int studentGender, String studentSSN, String district, String province,
                   String detail, String town, String studentPhone, String curriculumId) {
        this.studentId = studentId;
        this.studentEmail = studentEmail;
        this.lastName = lastName;
        this.midleName = midleName;
        this.firstName = firstName;
        this.studentDoB = studentDoB;
        this.studentGender = studentGender;
        this.studentSSN = studentSSN;
        this.district = district;
        this.province = province;
        this.detail = detail;
        this.town = town;
        this.studentPhone = studentPhone;
        this.curriculumId = curriculumId;
    }

    // Getters and Setters
    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getStudentEmail() {
        return studentEmail;
    }

    public void setStudentEmail(String studentEmail) {
        this.studentEmail = studentEmail;
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

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public Date getStudentDoB() {
        return studentDoB;
    }

    public void setStudentDoB(Date studentDoB) {
        this.studentDoB = studentDoB;
    }

    public int getStudentGender() {
        return studentGender;
    }

    public void setStudentGender(int studentGender) {
        this.studentGender = studentGender;
    }

    public String getStudentSSN() {
        return studentSSN;
    }

    public void setStudentSSN(String studentSSN) {
        this.studentSSN = studentSSN;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public String getTown() {
        return town;
    }

    public void setTown(String town) {
        this.town = town;
    }

    public String getStudentPhone() {
        return studentPhone;
    }

    public void setStudentPhone(String studentPhone) {
        this.studentPhone = studentPhone;
    }

    public String getCurriculumId() {
        return curriculumId;
    }

    public void setCurriculumId(String curriculumId) {
        this.curriculumId = curriculumId;
    }
}