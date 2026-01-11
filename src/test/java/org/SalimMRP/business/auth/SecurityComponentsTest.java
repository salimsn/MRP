package org.SalimMRP.business.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SecurityComponentsTest {

    private final PasswordHasher hasher = new Sha256PasswordHasher();
    private final TokenService tokenService = new InMemoryTokenService();

    @Test
    void passwordHasherProducesVerifiableHashes() {
        String hash = hasher.hash("Tr0ub4dor&3");

        assertTrue(hasher.matches("Tr0ub4dor&3", hash));
        assertFalse(hasher.matches("wrong", hash));
    }

    @Test
    void tokenServiceIssuesUniqueAndResolvableTokens() {
        String first = tokenService.issueToken("alice");
        String second = tokenService.issueToken("alice");

        assertNotEquals(first, second);
        assertEquals("alice", tokenService.resolveUsername(first));
        tokenService.invalidate(first);
        assertFalse(tokenService.isValid(first));
    }
}
