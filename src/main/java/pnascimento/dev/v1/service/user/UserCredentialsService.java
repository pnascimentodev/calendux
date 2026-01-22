package pnascimento.dev.v1.service.user;

import org.springframework.security.crypto.password.PasswordEncoder;

public class UserCredentialsService {

    private final PasswordEncoder passwordEncoder;

    public UserCredentialsService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public String hashPassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    public boolean matches(String rawPassword, String hashedPassword) {
        return passwordEncoder.matches(rawPassword, hashedPassword);
    }
}
