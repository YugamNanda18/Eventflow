package com.eventflow.auth;

import com.eventflow.tenant.Organization;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class JwtProviderTest {

    private JwtProvider jwtProvider;
    private User testUser;

    @BeforeEach
    void setUp() {
        String secret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
        jwtProvider = new JwtProvider(secret, 3600000, 86400000);

        Organization org = new Organization("org_test", "TestOrg", "testorg");
        testUser = new User("usr_123", org, "test@eventflow.com", "hashed_pwd", "Test", "User");
        testUser.setRoles(Set.of(new Role("ROLE_ADMIN", "ADMIN", "Admin Role")));
    }

    @Test
    void testGenerateAndValidateAccessToken() {
        String token = jwtProvider.generateAccessToken(testUser);
        assertNotNull(token);
        assertTrue(jwtProvider.validateToken(token));

        Claims claims = jwtProvider.getClaims(token);
        assertEquals("usr_123", claims.getSubject());
        assertEquals("test@eventflow.com", claims.get("email"));
        assertEquals("org_test", claims.get("tenantId"));
    }
}
