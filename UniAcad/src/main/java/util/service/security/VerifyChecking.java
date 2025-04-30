package util.service.security;

import java.time.LocalDate;

public class VerifyChecking {

    public boolean verifyEmail(String email) {
        if (email == null) return false;
        String regex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
        return email.matches(regex);
    }

    public boolean verifyPhoneNumber(String phoneNumber) {
        if (phoneNumber == null) return false;
        String regex = "^\\+?[0-9]{10,15}$";
        return phoneNumber.matches(regex);
    }

    public boolean verifyFullName(String fullName) {
        if (fullName == null) return false;
        String regex = "^[A-Z][a-zA-Z\\s]+$";
        return fullName.matches(regex);
    }

    public boolean verifyDateOfBirth(LocalDate dob) {
        if (dob == null) return false;
        return dob.isBefore(LocalDate.now());
    }

    public boolean verifyStudentID(String studentId) {
        if (studentId == null) return false;
        return studentId.matches("^\\w{8}$");
    }

    public boolean verifySSN(String ssn) {
        if (ssn == null) return false;
        return ssn.length() <= 20;
    }

    public boolean verifyCurriculumID(String curriculumId) {
        return curriculumId != null && !curriculumId.trim().isEmpty();
    }
}
