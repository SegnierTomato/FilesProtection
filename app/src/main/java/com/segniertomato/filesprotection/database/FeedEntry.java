package com.segniertomato.filesprotection.database;


public final class FeedEntry {

    public static final String TABLE_NAME = "protected_files";

    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_FILE_NAME = "file_name";
    public static final String COLUMN_ORIGINAL_PATH = "original_path";
    public static final String COLUMN_TRASH_PATH = "trash_path";
    public static final String COLUMN_DELETED_DATE = "deleted_at";

    private FeedEntry() {

    }

    public static class SQLBuilder {

        public static final String CREATE_PROTECTED_FILES_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                COLUMN_FILE_NAME + " TEXT NOT NULL COLLATE NOCASE," +
                COLUMN_ORIGINAL_PATH + " TEXT NOT NULL COLLATE NOCASE, " +
                COLUMN_TRASH_PATH + " TEXT NOT NULL COLLATE NOCASE, " +
                COLUMN_DELETED_DATE + " DATETIME DEFAULT CURRENT_TIMESTAMP " + ");";

        public static final String DROP_PROTECTED_FILES_TABLE = "DROP TABLE IF EXISTS " + TABLE_NAME;
    }
}
