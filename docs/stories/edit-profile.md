# Story: Edit User Profile

**ID:** USER-PROFILE-EDIT-001
**Module:** User
**Status:** Implemented (pending manual smoke test against live DB)
**Created:** 2026-04-24
**Owner:** TBD

---

## 1. User Story

**As a** logged-in user (Joueur or Arbitre)
**I want to** edit my profile information from the Profil page
**So that** I can keep my account details accurate, change my display nickname, and rotate my password without contacting an administrator.

---

## 2. Context

The Profil view (`profil-view.fxml` / `ProfilController.java`) already shows read-only data sourced from the current `User` in `SessionContext`. A **"✏ Modifier le Profil"** button exists and is wired to `onEditProfil()`, but the handler is currently a no-op (`{}`). The `User` entity exposes editable fields (`username`, `nickname`, `email`, `password`) and `UserService` already has BCrypt-aware login/register logic, but **no update method**. Passwords are stored as bcrypt hashes (Symfony `$2y$` prefix is normalized to `$2a$`).

---

## 3. Acceptance Criteria

### AC1 — Open the editor
- Clicking **"✏ Modifier le Profil"** on `profil-view.fxml` opens an "Edit Profile" modal dialog (FXML-driven, same theme as existing dialogs e.g. `tournoi-create-dialog.fxml`).
- The dialog is pre-filled with the current user's `username`, `nickname`, and `email`.
- Email is editable.
- Cancel/close discards changes without writing to the database.

### AC2 — Field-level validation (client-side, before submit)
- `username`: required, non-blank, length 3–32, trimmed.
- `nickname`: optional; if provided, length ≤ 32, trimmed.
- `email`: required, must match the existing regex `^[\w.+-]+@[\w-]+\.[\w.]+$`.
- Password change is **optional**:
  - If any of `currentPassword`, `newPassword`, `confirmNewPassword` is filled, **all three must be filled**.
  - `currentPassword` must match the user's stored hash (verified via BCrypt with the same `$2y$ → $2a$` normalization used in `login()`).
  - `newPassword` must be ≥ 6 characters (matches `register()` rule).
  - `newPassword` must equal `confirmNewPassword`.
  - `newPassword` must differ from `currentPassword`.
- All validation errors are surfaced inline in the dialog with French messages consistent with `UserService.AuthException` style.

### AC3 — Server-side uniqueness
- If the email is changed, it must not already belong to another user (`SELECT COUNT(*) FROM user WHERE email = ? AND id != ?`).
- On collision, show: *"Cet email est déjà utilisé."*

### AC4 — Persistence
- A new method `UserService.updateProfile(UUID userId, String username, String nickname, String email, String currentPassword, String newPassword)` performs the update in a single transaction:
  - `UPDATE user SET username=?, nickname=?, email=? [, password=?] WHERE id = UNHEX(REPLACE(?, '-', ''))`
  - When a new password is set, hash it with `BCrypt.hashpw(newPassword, BCrypt.gensalt(12))` (same cost factor as `register()`).
- On success, the updated `User` is returned and `SessionContext.getInstance().setCurrentUser(updatedUser)` is called so the rest of the UI sees the new values.

### AC5 — UI refresh
- After a successful save, the dialog closes and `ProfilController.initialize()` (or an equivalent `refresh()` method) re-renders `usernameLabel`, `avatarInitial`, `emailLabel`, etc., without requiring the user to navigate away and back.
- A success toast/alert is shown: *"Profil mis à jour avec succès."*

### AC6 — Error handling
- All failures throw `UserService.AuthException` with a user-readable French message and are caught by the controller, which displays them inline (no stack traces in the UI).
- DB connection failures show: *"Erreur de connexion à la base de données."*

### AC7 — Security
- Roles, balance, status, `is_verified`, `created_at`, and `id` are **never** writable from this dialog.
- The `currentPassword` field is only validated server-side via BCrypt — never echoed, logged, or sent in clear text beyond the in-process call.

---

## 4. Technical Notes / Implementation Plan

### Files to create
- `src/main/resources/com/carthage/view/user/profil-edit-dialog.fxml` — the modal layout (TextFields for username, nickname, email; PasswordFields for current, new, confirm; Save / Cancel buttons; an error Label).
- `src/main/java/com/carthage/controllers/user/ProfilEditController.java` — handles bind/validate/submit; injects the current `User`; calls `UserService.updateProfile(...)`.

### Files to modify
- `src/main/java/com/carthage/services/UserService.java` — add `updateProfile(...)` method (mirror style of `register()`); reuse `hexToUUID` and BCrypt helpers; reuse `AuthException`.
- `src/main/java/com/carthage/controllers/user/ProfilController.java`:
  - Implement `onEditProfil()`: load `profil-edit-dialog.fxml`, pass current `User`, show as modal `Stage.initModality(Modality.APPLICATION_MODAL)`.
  - Extract the field-population block from `initialize()` into a private `renderUser(User user)` method so it can be re-called after save.
- (Optional) `src/main/resources/com/carthage/css/main.css` — add `.dialog-error` style if not already present.

### Out of scope
- Avatar upload / image picker.
- Discord ID, license, or role changes.
- Two-factor or email re-verification on email change (note as a future story).
- Editing `bio` (the FXML has a `bioLabel` but `User` has no bio field today).

---

## 5. Test Plan

### Manual smoke
1. Log in as a Joueur; click **Modifier le Profil**; change username only; save → header re-renders with new username and initial.
2. Change email to one already taken by another account → inline error *"Cet email est déjà utilisé."*; no DB write.
3. Fill only `newPassword` without `currentPassword` → inline error blocks submit.
4. Provide wrong `currentPassword` → inline error *"Mot de passe actuel incorrect."*
5. Provide `newPassword` ≠ `confirmNewPassword` → inline error.
6. Successful password change → log out, log back in with the new password.

### Automated (JUnit, mirrors `PasswordResetServiceTest` style)
- `UserServiceUpdateProfileTest`:
  - rejects blank username;
  - rejects invalid email format;
  - rejects duplicate email belonging to another user;
  - rejects wrong current password;
  - rejects mismatched new/confirm;
  - persists username/email change without touching `password` when password fields are empty;
  - persists bcrypt-hashed password when all three password fields are valid.

---

## 6. Definition of Done

- [ ] All acceptance criteria pass manual smoke testing.
- [ ] `UserServiceUpdateProfileTest` passes via `mvn test`.
- [ ] No new compiler warnings in modified files.
- [ ] `SessionContext` is updated after a successful save (verified by re-opening the Profil view without re-login).
- [ ] No plaintext passwords are logged or written to the DB.
- [ ] Code reviewed and merged.
