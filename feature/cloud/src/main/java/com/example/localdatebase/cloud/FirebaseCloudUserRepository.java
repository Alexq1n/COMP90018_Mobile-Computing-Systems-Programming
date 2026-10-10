package com.example.localdatebase.cloud;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Firebase implementation. Raw passwords are sent only to Firebase Authentication. */
public final class FirebaseCloudUserRepository implements CloudUserRepository {
    private final FirebaseAuth auth;
    private final FirebaseFirestore firestore;

    public FirebaseCloudUserRepository() {
        this(FirebaseAuth.getInstance(), FirebaseFirestore.getInstance());
    }

    FirebaseCloudUserRepository(FirebaseAuth auth, FirebaseFirestore firestore) {
        this.auth = auth;
        this.firestore = firestore;
    }

    @Override public CloudUserSession currentUser() {
        FirebaseUser user = auth.getCurrentUser();
        return user == null ? null : session(user);
    }

    @Override public void register(
            String email, String password, CloudResultCallback<CloudUserSession> callback) {
        String cleanEmail = requireEmail(email);
        requirePassword(password);
        auth.createUserWithEmailAndPassword(cleanEmail, password)
                .addOnSuccessListener(result -> {
                    FirebaseUser user = result.getUser();
                    if (user == null) {
                        callback.onError(new IllegalStateException("Firebase 未返回用户信息"));
                        return;
                    }
                    Map<String, Object> profile = new HashMap<>();
                    profile.put("email", cleanEmail);
                    profile.put("createdAt", FieldValue.serverTimestamp());
                    profile.put("updatedAt", FieldValue.serverTimestamp());
                    userDocument(user).set(profile, SetOptions.merge())
                            .addOnSuccessListener(unused -> callback.onSuccess(session(user)))
                            .addOnFailureListener(callback::onError);
                })
                .addOnFailureListener(callback::onError);
    }

    @Override public void signIn(
            String email, String password, CloudResultCallback<CloudUserSession> callback) {
        String cleanEmail = requireEmail(email);
        requirePassword(password);
        auth.signInWithEmailAndPassword(cleanEmail, password)
                .addOnSuccessListener(result -> {
                    FirebaseUser user = result.getUser();
                    if (user == null) callback.onError(new IllegalStateException("登录结果中没有用户"));
                    else callback.onSuccess(session(user));
                })
                .addOnFailureListener(callback::onError);
    }

    @Override public void signOut() {
        auth.signOut();
    }

    @Override public void savePreferences(
            UserPreferences preferences, CloudResultCallback<Void> callback) {
        FirebaseUser user = requireUser();
        if (preferences == null) throw new IllegalArgumentException("用户偏好不能为空");

        Map<String, Object> profile = new HashMap<>();
        profile.put("email", user.getEmail() == null ? "" : user.getEmail());
        profile.put("updatedAt", FieldValue.serverTimestamp());

        Map<String, Object> data = new HashMap<>();
        data.put("theme", preferences.theme);
        data.put("language", preferences.language);
        data.put("preferredCategories", preferences.preferredCategories);
        data.put("notificationsEnabled", preferences.notificationsEnabled);
        data.put("updatedAt", FieldValue.serverTimestamp());

        DocumentReference profileRef = userDocument(user);
        profileRef.set(profile, SetOptions.merge())
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    return profileRef.collection("preferences").document("default")
                            .set(data, SetOptions.merge());
                })
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }

    @Override public void loadPreferences(CloudResultCallback<UserPreferences> callback) {
        FirebaseUser user = requireUser();
        userDocument(user).collection("preferences").document("default").get()
                .addOnSuccessListener(document -> callback.onSuccess(readPreferences(document)))
                .addOnFailureListener(callback::onError);
    }

    private DocumentReference userDocument(FirebaseUser user) {
        return firestore.collection("users").document(user.getUid());
    }

    private FirebaseUser requireUser() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) throw new IllegalStateException("请先登录云端账号");
        return user;
    }

    private static CloudUserSession session(FirebaseUser user) {
        return new CloudUserSession(user.getUid(), user.getEmail());
    }

    private static UserPreferences readPreferences(DocumentSnapshot document) {
        if (!document.exists()) return new UserPreferences("system", "zh-CN", null, true);
        List<String> categories = new ArrayList<>();
        Object rawCategories = document.get("preferredCategories");
        if (rawCategories instanceof List<?>) {
            for (Object value : (List<?>) rawCategories)
                if (value instanceof String) categories.add((String) value);
        }
        Boolean notifications = document.getBoolean("notificationsEnabled");
        return new UserPreferences(document.getString("theme"), document.getString("language"),
                categories, notifications == null || notifications);
    }

    private static String requireEmail(String email) {
        String clean = email == null ? "" : email.trim();
        if (clean.isEmpty() || !clean.contains("@"))
            throw new IllegalArgumentException("请输入有效邮箱地址");
        return clean;
    }

    private static void requirePassword(String password) {
        if (password == null || password.length() < 6)
            throw new IllegalArgumentException("密码至少需要 6 个字符");
    }
}
