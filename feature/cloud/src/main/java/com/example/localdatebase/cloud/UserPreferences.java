package com.example.localdatebase.cloud;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Preferences synchronized at users/{uid}/preferences/default. */
public final class UserPreferences {
    public final String theme;
    public final String language;
    public final List<String> preferredCategories;
    public final boolean notificationsEnabled;

    public UserPreferences(String theme, String language, List<String> preferredCategories,
                           boolean notificationsEnabled) {
        this.theme = clean(theme, "system");
        this.language = clean(language, "zh-CN");
        this.preferredCategories = Collections.unmodifiableList(new ArrayList<>(
                preferredCategories == null ? Collections.emptyList() : preferredCategories));
        this.notificationsEnabled = notificationsEnabled;
    }

    private static String clean(String value, String fallback) {
        String cleaned = value == null ? "" : value.trim();
        return cleaned.isEmpty() ? fallback : cleaned;
    }
}
