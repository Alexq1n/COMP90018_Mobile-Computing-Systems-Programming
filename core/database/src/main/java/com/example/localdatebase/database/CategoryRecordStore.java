package com.example.localdatebase.database;

import java.util.List;

public interface CategoryRecordStore {
    long addCategory(String name);
    long saveRecord(Long id, long categoryId, String title, String content);
    void deleteRecord(long id);
    List<CategoryData> getCategories();
    List<RecordData> getRecords(Long categoryId, String search);
}
