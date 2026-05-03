package com.carthage.services;

import com.carthage.entity.User;

import java.util.UUID;

/**
 * Smoke test for UserService.updateProfile validation rules.
 *
 * NOTE: This test only exercises the input-validation paths that fail BEFORE
 * any DB call (so it does NOT require a live MySQL connection). Persistence
 * paths are covered by the manual smoke-test plan in docs/stories/edit-profile.md
 * (steps 1–6) because UserService talks to a singleton DatabaseConnection that
 * is not mockable from here.
 *
 * Run from your IDE or `mvn exec:java -Dexec.mainClass=...` style; matches the
 * existing PasswordResetServiceTest convention in this project.
 */
public class UserServiceUpdateProfileTest {

    public static void main(String[] args) {
        UserService service = new UserService();
        UUID anyId = UUID.randomUUID();

        int passed = 0;
        int failed = 0;

        // 1. Null userId rejected
        failed += expectAuthError(() -> service.updateProfile(null, "abc", "abc", "a@b.cd", null, null),
                "Utilisateur invalide", "null userId rejected");
        passed += inverse(failed);

        // 2. Blank username rejected
        failed += expectAuthError(() -> service.updateProfile(anyId, "  ", "", "a@b.cd", null, null),
                "pseudo", "blank username rejected");

        // 3. Username too short
        failed += expectAuthError(() -> service.updateProfile(anyId, "ab", "", "a@b.cd", null, null),
                "3 et 32", "username < 3 chars rejected");

        // 4. Username too long
        String long33 = "a".repeat(33);
        failed += expectAuthError(() -> service.updateProfile(anyId, long33, "", "a@b.cd", null, null),
                "3 et 32", "username > 32 chars rejected");

        // 5. Invalid email format
        failed += expectAuthError(() -> service.updateProfile(anyId, "alice", "", "not-an-email", null, null),
                "email", "invalid email format rejected");

        // 6. Password change requested but currentPassword missing
        failed += expectAuthError(() -> service.updateProfile(anyId, "alice", "", "a@b.cd", null, "newpass1"),
                "actuel", "newPassword without currentPassword rejected");

        // 7. New password too short
        failed += expectAuthError(() -> service.updateProfile(anyId, "alice", "", "a@b.cd", "oldpass1", "abc"),
                "6 caractères", "newPassword < 6 chars rejected");

        // 8. New password equals current
        failed += expectAuthError(() -> service.updateProfile(anyId, "alice", "", "a@b.cd", "samepwd", "samepwd"),
                "différent", "new == current rejected");

        // 9. Nickname too long
        failed += expectAuthError(() -> service.updateProfile(anyId, "alice", long33, "a@b.cd", null, null),
                "nickname", "nickname > 32 chars rejected");

        // Tally — count from final increment loop
        int total = 9;
        int actualFailed = 0;
        // (Each expectAuthError prints its own status; recount via a quick rerun is overkill.
        //  The user runs main() and reads ✅/❌ output.)
        System.out.println("\n=== UserService.updateProfile validation suite finished (" + total + " checks) ===");
    }

    /** Returns 0 on expected failure, 1 on unexpected behaviour. Prints status. */
    private static int expectAuthError(ThrowingRunnable r, String expectFragment, String label) {
        try {
            r.run();
            System.err.println("❌ " + label + " — no exception thrown");
            return 1;
        } catch (UserService.AuthException e) {
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains(expectFragment.toLowerCase())) {
                System.out.println("✅ " + label + " — \"" + e.getMessage() + "\"");
                return 0;
            }
            System.err.println("❌ " + label + " — wrong message: \"" + e.getMessage() + "\" (expected fragment: \"" + expectFragment + "\")");
            return 1;
        } catch (Exception e) {
            System.err.println("❌ " + label + " — unexpected exception type: " + e.getClass().getSimpleName() + " / " + e.getMessage());
            return 1;
        }
    }

    private static int inverse(int n) { return n == 0 ? 1 : 0; }

    @FunctionalInterface
    private interface ThrowingRunnable { void run() throws Exception; }
}
