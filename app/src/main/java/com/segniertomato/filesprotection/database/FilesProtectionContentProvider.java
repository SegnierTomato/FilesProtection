package com.segniertomato.filesprotection.database;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.Nullable;


public class FilesProtectionContentProvider extends ContentProvider {

    public static final Uri URI;
    private static final UriMatcher mUriMatcher;

    private static final String AUTHORITY = "com.segniertomato.filesprotection.contentprovider";
    private static final int CODE_TYPE = 1;
    private static final int CODE_ITEM_TYPE = 2;

    static {

        URI = Uri.parse("content://" + AUTHORITY + "/files");

        mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        mUriMatcher.addURI(AUTHORITY, "files", CODE_TYPE);
        mUriMatcher.addURI(AUTHORITY, "files/#", CODE_ITEM_TYPE);
    }

    private SQLiteDatabase mSQLiteDB;
    private SQLiteHelper mSqLiteHelper;

    @Override
    public boolean onCreate() {

        mSqLiteHelper = new SQLiteHelper(getContext());
        mSQLiteDB = mSqLiteHelper.getWritableDatabase();

        return true;
    }

    @Nullable
    @Override
    public String getType(Uri uri) {

        switch (mUriMatcher.match(uri)) {
            case CODE_TYPE:
                return "vnd.android.cursor.dir/vnd.com.segniertomato.filesprotection.contentprovider.files";

            case CODE_ITEM_TYPE:
                return "vnd.android.cursor.item/vnd.com.segniertomato.filesprotection.contentprovider.files";

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) throws IllegalArgumentException{

        SQLiteQueryBuilder sqLQueryBuilder = new SQLiteQueryBuilder();
        sqLQueryBuilder.setTables(FeedEntry.TABLE_NAME);

        switch (mUriMatcher.match(uri)) {
            case CODE_TYPE:
                break;

            case CODE_ITEM_TYPE:

                sqLQueryBuilder.appendWhere(FeedEntry.COLUMN_ID + " = " + uri.getLastPathSegment());
                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        if (sortOrder == null || sortOrder.isEmpty()) {
            sortOrder = "deleted_at DESC";
        }

        Cursor cursor = sqLQueryBuilder.query(mSQLiteDB, projection, selection, selectionArgs, null, null, sortOrder);
        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        return cursor;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) throws IllegalArgumentException {

        int deletedRow;

        switch (mUriMatcher.match(uri)) {
            case CODE_TYPE:

                deletedRow = mSQLiteDB.delete(FeedEntry.TABLE_NAME, selection, selectionArgs);
                break;

            case CODE_ITEM_TYPE:

                String rowId = uri.getLastPathSegment();

                StringBuilder whereClause = new StringBuilder(FeedEntry.COLUMN_ID + "=" + rowId);
                if (selection != null || !selection.isEmpty()) {
                    whereClause.append("AND (" + selection + ")");
                }
                deletedRow = mSQLiteDB.delete(FeedEntry.TABLE_NAME, whereClause.toString(), selectionArgs);

                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return deletedRow;
    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues contentValues)throws SQLException, SQLiteException, IllegalArgumentException {

        Uri updatedUri = null;

        switch (mUriMatcher.match(uri)) {
            case CODE_TYPE:

                long idNewRow = mSQLiteDB.insertOrThrow(FeedEntry.TABLE_NAME, null, contentValues);

                if (idNewRow != -1) {
                    updatedUri = ContentUris.withAppendedId(uri, idNewRow);

                } else {
                    throw new SQLiteException("Failed to insert row into " + uri);
                }
                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(updatedUri, null);
        return updatedUri;
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String selection, String[] selectionArgs) throws IllegalArgumentException{

        int rowUpdated = -1;
        switch (mUriMatcher.match(uri)) {

            case CODE_TYPE:
                rowUpdated = mSQLiteDB.update(FeedEntry.TABLE_NAME, contentValues, selection, selectionArgs);
                break;

            case CODE_ITEM_TYPE:

                String rowId = uri.getLastPathSegment();
                StringBuilder whereClause = new StringBuilder(FeedEntry.COLUMN_ID + "=" + rowId);

                if (selection != null || selection.isEmpty()) {
                    whereClause.append("AND (" + selection + ")");
                }
                rowUpdated = mSQLiteDB.update(FeedEntry.TABLE_NAME, contentValues, whereClause.toString(), selectionArgs);
                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return rowUpdated;
    }
}
