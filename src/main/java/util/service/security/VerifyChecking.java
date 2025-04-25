package util.service.security;

public class VerifyChecking {
    public boolean verifyEmail(String email) {
        String regex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
        return email.matches(regex);
    }

    public boolean verifyPhoneNumber(String phoneNumber) {
        String regex = "^\\+?[0-9]{10,15}$";
        return phoneNumber.matches(regex);
    }

    public boolean verifyFullName(String fullName) {
        String regex = "^[A-Z][a-zA-Z\\s]+$";
        return fullName.matches(regex);
    }

    public boolean verifyPassword(String password) {
        String regex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[A-Za-z\\d@$!%*?&]{8,}$";
        return password.matches(regex);
    }
}
