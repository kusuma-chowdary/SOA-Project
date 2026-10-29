package com.archivalia.auth;

import com.archivalia.common.JwtService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserAccountRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(UserAccountRepository users, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    @Transactional
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists.");
        }
        UserAccount user = users.save(new UserAccount(
                request.name().trim(), email, passwordEncoder.encode(request.password()), "USER"));
        return responseFor(user);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        UserAccount user = users.findByEmailIgnoreCase(request.email().trim())
                .filter(account -> passwordEncoder.matches(request.password(), account.getPasswordHash()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email or password is incorrect."));
        return responseFor(user);
    }

    @GetMapping("/me")
    public UserProfile currentUser(@RequestHeader("X-Authenticated-User") String userId) {
        UserAccount user = users.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User account was not found."));
        return new UserProfile(user.getId(), user.getName(), user.getEmail(), user.getRole());
    }

    @PutMapping("/me")
    @Transactional
    public AuthResponse updateAccount(@RequestHeader("X-Authenticated-User") String userId,
                                      @Valid @RequestBody UpdateCredentialsRequest request) {
        UserAccount user = users.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User account was not found."));
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Current password is incorrect.");
        }

        String email = request.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCaseAndIdNot(email, userId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists.");
        }
        String newPassword = request.newPassword();
        if (newPassword != null && !newPassword.isBlank() && newPassword.length() < 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New password must contain at least 8 characters.");
        }

        String passwordHash = newPassword == null || newPassword.isBlank()
                ? user.getPasswordHash()
                : passwordEncoder.encode(newPassword);
        user.updateCredentials(email, passwordHash);
        users.save(user);
        return responseFor(user);
    }

    private AuthResponse responseFor(UserAccount user) {
        return new AuthResponse(jwtService.createToken(user.getId(), user.getEmail(), user.getRole()),
                user.getId(), user.getName(), user.getEmail(), user.getRole(), jwtService.getExpirationSeconds());
    }

    public record RegisterRequest(
            @NotBlank @Size(min = 2, max = 80) String name,
            @NotBlank @Email @Size(max = 160) String email,
            @NotBlank @Size(min = 8, max = 100) String password) { }
    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) { }
    public record UpdateCredentialsRequest(
            @NotBlank String currentPassword,
            @NotBlank @Email @Size(max = 160) String email,
            @Size(max = 100) String newPassword) { }
    public record AuthResponse(String accessToken, String userId, String name, String email, String role,
                               long expiresInSeconds) { }
    public record UserProfile(String userId, String name, String email, String role) { }
}
