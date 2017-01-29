package com.segniertomato.filesprotection.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


public class SQLiteHelper extends SQLiteOpenHelper {

    private final static String DATABASE_NAME = "protected_files.db";
    private final static int DATABASE_VERSION = 1;

    public SQLiteHelper(Context context){
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public SQLiteHelper(Context context, final String DB_NAME, final int DB_VERSION){
        super(context, DB_NAME, null, DB_VERSION);
    }

    public SQLiteHelper(Context context, final String DB_NAME, final int DB_VERSION, SQLiteDatabase.CursorFactory factory){
        super(context, DB_NAME, factory, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(FeedEntry.SQLBuilder.CREATE_PROTECTED_FILES_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

        sqLiteDatabase.execSQL(FeedEntry.SQLBuilder.DROP_PROTECTED_FILES_TABLE);
        onCreate(sqLiteDatabase);
    }
}
