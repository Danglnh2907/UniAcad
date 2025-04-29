package util.service.security;

import java.time.LocalDate;

public class VerifyChecking {

    /**
     * Kiểm tra email có hợp lệ hay không
     */
    public boolean verifyEmail(String email) {
        if (email == null) return false;
        String regex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
        return email.matches(regex);
    }

    /**
     * Kiểm tra số điện thoại có hợp lệ (10-15 số, cho phép +)
     */
    public boolean verifyPhoneNumber(String phoneNumber) {
        if (phoneNumber == null) return false;
        String regex = "^\\+?[0-9]{10,15}$";
        return phoneNumber.matches(regex);
    }

    /**
     * Kiểm tra họ tên hợp lệ (Bắt đầu bằng chữ in hoa)
     */
    public boolean verifyFullName(String fullName) {
        if (fullName == null) return false;
        String regex = "^[A-Z][a-zA-Z\\s]+$";
        return fullName.matches(regex);
    }

    /**
     * Kiểm tra ngày sinh phải bé hơn ngày hôm nay
     */
    public boolean verifyDateOfBirth(LocalDate dob) {
        if (dob == null) return false;
        return dob.isBefore(LocalDate.now());
    }

    /**
     * Kiểm tra mã sinh viên StudentID đúng 8 ký tự
     */
    public boolean verifyStudentID(String studentId) {
        if (studentId == null) return false;
        return studentId.matches("^\\w{8}$");
    }

    /**
     * Kiểm tra SSN không null, độ dài 20 ký tự trở xuống
     */
    public boolean verifySSN(String ssn) {
        if (ssn == null) return false;
        return ssn.length() <= 20;
    }

    /**
     * Kiểm tra CurriculumID không null, không rỗng
     */
    public boolean verifyCurriculumID(String curriculumId) {
        return curriculumId != null && !curriculumId.trim().isEmpty();
    }
}
