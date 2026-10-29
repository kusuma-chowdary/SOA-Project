package com.archivalia.auth;

import com.archivalia.common.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthControllerTest {
    private final UserAccountRepository users = mock(UserAccountRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final JwtService jwtService = new JwtService(
            "a-long-test-secret-that-is-only-used-for-unit-tests", 3600);
    private final AuthController controller = new AuthController(users, passwordEncoder, jwtService);

    @Test
    void registrationReturnsSessionImmediately() {
        when(users.existsByEmailIgnoreCase("reader@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("password-hash");
        when(users.save(any(UserAccount.class))).thenAnswer(invocation -> {
            UserAccount user = invocation.getArgument(0);
            ReflectionTestUtils.setField(user, "id", "user-1");
            return user;
        });

        AuthController.AuthResponse response = controller.register(
                new AuthController.RegisterRequest("Reader", "reader@example.com", "password123"));

        assertSession(response);
    }

    @Test
    void loginReturnsSessionImmediatelyAfterPasswordValidation() {
        UserAccount user = new UserAccount("Reader", "reader@example.com", "password-hash", "USER");
        ReflectionTestUtils.setField(user, "id", "user-1");
        when(users.findByEmailIgnoreCase("reader@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "password-hash")).thenReturn(true);

        AuthController.AuthResponse response = controller.login(
                new AuthController.LoginRequest("reader@example.com", "password123"));

        assertSession(response);
    }

    @Test
    void accountUpdateChangesEmailAndPasswordAndReturnsRefreshedSession() {
        UserAccount user = new UserAccount("Reader", "reader@example.com", "password-hash", "USER");
        ReflectionTestUtils.setField(user, "id", "user-1");
        when(users.findById("user-1")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("current-password", "password-hash")).thenReturn(true);
        when(users.existsByEmailIgnoreCaseAndIdNot("new@example.com", "user-1")).thenReturn(false);
        when(passwordEncoder.encode("new-password")).thenReturn("new-password-hash");

        AuthController.AuthResponse response = controller.updateAccount("user-1",
                new AuthController.UpdateCredentialsRequest("current-password", "new@example.com", "new-password"));

        assertEquals("new@example.com", user.getEmail());
        assertEquals("new-password-hash", user.getPasswordHash());
        assertEquals("new@example.com", response.email());
        assertEquals("user-1", jwtService.parse(response.accessToken()).getSubject());
        assertEquals("new@example.com", jwtService.parse(response.accessToken()).get("email", String.class));
        verify(users).save(user);
    }

    @Test
    void accountUpdateRejectsAnIncorrectCurrentPassword() {
        UserAccount user = new UserAccount("Reader", "reader@example.com", "password-hash", "USER");
        ReflectionTestUtils.setField(user, "id", "user-1");
        when(users.findById("user-1")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("incorrect-password", "password-hash")).thenReturn(false);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> controller.updateAccount("user-1",
                        new AuthController.UpdateCredentialsRequest("incorrect-password", "new@example.com", "")));

        assertEquals(401, exception.getStatusCode().value());
    }

    @Test
    void accountUpdateKeepsPasswordWhenNewPasswordIsBlank() {
        UserAccount user = new UserAccount("Reader", "reader@example.com", "password-hash", "USER");
        ReflectionTestUtils.setField(user, "id", "user-1");
        when(users.findById("user-1")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("current-password", "password-hash")).thenReturn(true);
        when(users.existsByEmailIgnoreCaseAndIdNot("new@example.com", "user-1")).thenReturn(false);

        controller.updateAccount("user-1",
                new AuthController.UpdateCredentialsRequest("current-password", "new@example.com", ""));

        assertEquals("new@example.com", user.getEmail());
        assertEquals("password-hash", user.getPasswordHash());
    }

    private void assertSession(AuthController.AuthResponse response) {
        assertNotNull(response.accessToken());
        assertEquals("user-1", response.userId());
        assertEquals("Reader", response.name());
        assertEquals("reader@example.com", response.email());
        assertEquals("USER", response.role());
        assertEquals(3600, response.expiresInSeconds());
        assertEquals("user-1", jwtService.parse(response.accessToken()).getSubject());
    }
}
