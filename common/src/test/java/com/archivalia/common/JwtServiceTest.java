package com.archivalia.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {
    private final JwtService jwtService = new JwtService(
            "a-long-test-secret-that-is-only-used-for-unit-tests", 3600);

    @Test
    void createsAndParsesSignedTokens() {
        String token = jwtService.createToken("user-1", "reader@example.com", "USER");
        var claims = jwtService.parse(token);

        assertEquals("user-1", claims.getSubject());
        assertEquals("reader@example.com", claims.get("email", String.class));
        assertEquals("USER", claims.get("role", String.class));
        assertTrue(claims.getExpiration().after(new java.util.Date()));
        assertEquals(3600, jwtService.getExpirationSeconds());
    }
}
