package com.segniertomato.filesprotection.ui;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import com.segniertomato.filesprotection.R;
import com.segniertomato.filesprotection.adapters.DeletedFilesCursorAdapter;
import com.segniertomato.filesprotection.database.FeedEntry;
import com.segniertomato.filesprotection.database.FilesProtectionContentProvider;
import com.segniertomato.filesprotection.storage.FileSystemHandler;

import java.util.ArrayList;
import java.util.List;


public class DeletedFilesViewFragment extends Fragment {

    private static final String LOG_TAG = DeletedFilesViewFragment.class.getSimpleName();

    private ProtectedFilesObserver mProtectedFileObserver;
    private ListView mListViewDeletedFiles;
    private DeletedFilesCursorAdapter mCursorAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflate, ViewGroup container, Bundle savedInstanceState) {

        View view = inflate.inflate(R.layout.listview_deleted_files, container, false);
        mListViewDeletedFiles = (ListView) view.findViewById(R.id.listview_deleted_files);

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final Context context = getContext();

        try {

            Uri uri = FilesProtectionContentProvider.URI;

            String sortOrder = FeedEntry.COLUMN_FILE_NAME + " ASC";
            final Cursor cursor = context.getContentResolver().query(uri, null, null, null, sortOrder);

            mCursorAdapter = new DeletedFilesCursorAdapter(context, cursor, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
            mListViewDeletedFiles.setAdapter(mCursorAdapter);

            Handler handler = new Handler(context.getMainLooper());
            mProtectedFileObserver = new ProtectedFilesObserver(handler);

        } catch (Exception ex) {

            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(context);
            alertBuilder.setTitle("exception");
            alertBuilder.setMessage(ex.getMessage());
            alertBuilder.create().show();

            Toast.makeText(context, "Error :" + ex.getMessage(), Toast.LENGTH_LONG).show();
        }

    }

    @Override
    public void onResume() {
        super.onResume();

        getContext().getContentResolver().registerContentObserver(FilesProtectionContentProvider.URI, false, mProtectedFileObserver);
        Cursor newCursor = mCursorAdapter.getUpdatedCursor(getContext());
        mCursorAdapter.changeCursor(newCursor);
    }

    @Override
    public void onPause() {
        super.onPause();
        getContext().getContentResolver().unregisterContentObserver(mProtectedFileObserver);
    }

    private class ProtectedFilesObserver extends ContentObserver {

        public ProtectedFilesObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            Log.d(LOG_TAG, "onChange method");

            Cursor newCursor = mCursorAdapter.getUpdatedCursor(getContext());
            mCursorAdapter.changeCursor(newCursor);
            mCursorAdapter.notifyDataSetChanged();
        }
    }
}
