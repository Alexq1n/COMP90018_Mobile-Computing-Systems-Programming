package com.example.localdatebase.cloud;

/** Stable API for account authentication and per-user Firestore data. */
public interface CloudUserRepository {
    CloudUserSession currentUser();
    void register(String email, String password, CloudResultCallback<CloudUserSession> callback);
    void signIn(String email, String password, CloudResultCallback<CloudUserSession> callback);
    void signOut();
    void savePreferences(UserPreferences preferences, CloudResultCallback<Void> callback);
    void loadPreferences(CloudResultCallback<UserPreferences> callback);
}
