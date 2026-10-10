package com.example.localdatebase.database;

public final class RecordData {
    public final long id;
    public final long categoryId;
    public final String categoryName;
    public final String title;
    public final String content;
    public final long createdAt;
    public final long updatedAt;

    public RecordData(long id, long categoryId, String categoryName, String title, String content,
                      long createdAt, long updatedAt) {
        this.id = id;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.title = title;
        this.content = content;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
