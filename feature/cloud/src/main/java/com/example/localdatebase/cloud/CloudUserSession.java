package com.example.localdatebase.cloud;

/** Public account identity exposed to the app. Passwords are never part of this model. */
public final class CloudUserSession {
    public final String uid;
    public final String email;

    public CloudUserSession(String uid, String email) {
        this.uid = uid;
        this.email = email == null ? "" : email;
    }
}
