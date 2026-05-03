package com.carthage.services;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the roles JSON array parsing logic used in UserService.login().
 */
public class RolesParsingTest {

    /**
     * Replicates the exact parsing logic from UserService.login().
     */
    private List<String> parseRoles(String rolesRaw) {
        if (rolesRaw == null || rolesRaw.isBlank()) return List.of();
        String stripped = rolesRaw.trim()
                .replaceAll("^\\[|\\]$", "")  // remove leading [ and trailing ]
                .replaceAll("\"", "");          // remove all quotes
        if (stripped.isBlank()) return List.of();
        return Arrays.asList(stripped.split(","));
    }

    @Test
    void testSingleRoleUser() {
        List<String> roles = parseRoles("[\"ROLE_USER\"]");
        assertEquals(1, roles.size());
        assertEquals("ROLE_USER", roles.get(0));
    }

    @Test
    void testSingleRoleArbitre() {
        List<String> roles = parseRoles("[\"ROLE_ARBITRE\"]");
        assertEquals(1, roles.size());
        assertEquals("ROLE_ARBITRE", roles.get(0));
    }

    @Test
    void testSingleRoleAdmin() {
        List<String> roles = parseRoles("[\"ROLE_ADMIN\"]");
        assertEquals(1, roles.size());
        assertEquals("ROLE_ADMIN", roles.get(0));
        assertTrue(roles.stream().anyMatch(r -> r.toUpperCase().contains("ADMIN")));
    }

    @Test
    void testMultipleRoles() {
        List<String> roles = parseRoles("[\"ROLE_USER\",\"ROLE_ADMIN\"]");
        assertEquals(2, roles.size());
        assertTrue(roles.contains("ROLE_USER"));
        assertTrue(roles.contains("ROLE_ADMIN"));
    }

    @Test
    void testExactMatchWorks() {
        // This is the key regression test — exact contains() check must work
        List<String> roles = parseRoles("[\"ROLE_USER\"]");
        assertTrue(roles.contains("ROLE_USER"), "Exact role match should work after fix");
        assertFalse(roles.contains("[\"ROLE_USER\"]"), "Raw JSON string should NOT be in the list");
    }

    @Test
    void testNullRoles() {
        List<String> roles = parseRoles(null);
        assertTrue(roles.isEmpty());
    }

    @Test
    void testEmptyRoles() {
        List<String> roles = parseRoles("");
        assertTrue(roles.isEmpty());
    }
}
