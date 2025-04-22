package util.security;

import org.mindrot.jbcrypt.BCrypt;

public class SecurityService {
    public String encrypt(String str) {
        return BCrypt.hashpw(str, BCrypt.gensalt());
    }
}
