package com.archivalia.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminAccountInitializer implements ApplicationRunner {
    private final UserAccountRepository users;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;

    public AdminAccountInitializer(UserAccountRepository users, PasswordEncoder passwordEncoder,
                                   @Value("${app.jwt.admin-email}") String email,
                                   @Value("${app.jwt.admin-password:}") String password) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (password == null || password.isBlank()) {
            throw new IllegalStateException("Set ADMIN_PASSWORD in the local environment to initialize the administrator account.");
        }
        if (password.length() < 12) {
            throw new IllegalStateException("ADMIN_PASSWORD must contain at least 12 characters.");
        }
        users.findByEmailIgnoreCase(email).ifPresentOrElse(
                existing -> {
                    if (!"ADMIN".equals(existing.getRole())) {
                        throw new IllegalStateException(
                                "ADMIN_EMAIL belongs to a non-administrator account. Choose a different ADMIN_EMAIL.");
                    }
                },
                () -> users.save(new UserAccount("Library Administrator", email, passwordEncoder.encode(password), "ADMIN")));
    }
}
