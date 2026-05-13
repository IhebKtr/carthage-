package com.carthage.utils;

import com.carthage.entity.User;

/**
 * Singleton that holds the currently authenticated user for the lifetime
 * of the JavaFX session.
 */
public class SessionContext {

    private static SessionContext instance;
    private User currentUser;

    private SessionContext() {}

    public static SessionContext getInstance() {
        if (instance == null) {
            instance = new SessionContext();
        }
        return instance;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    /** Call on logout to clear the session. */
    public void cleanSession() {
        currentUser = null;
    }
}
