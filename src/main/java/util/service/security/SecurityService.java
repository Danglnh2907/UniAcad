package util.service.security;

import org.mindrot.jbcrypt.BCrypt;

public class SecurityService {
    public String encrypt(String str) {
        return BCrypt.hashpw(str, BCrypt.gensalt());
    }

    public String tokenize(String str) {
        return BCrypt.hashpw(str, BCrypt.gensalt());
    }
}
