package com.segniertomato.filesprotection.adapters;


import android.animation.ObjectAnimator;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.aakira.expandablelayout.ExpandableLayoutListenerAdapter;
import com.github.aakira.expandablelayout.ExpandableLinearLayout;
import com.github.aakira.expandablelayout.Utils;
import com.segniertomato.filesprotection.R;
import com.segniertomato.filesprotection.database.FeedEntry;
import com.segniertomato.filesprotection.database.FilesProtectionContentProvider;
import com.segniertomato.filesprotection.storage.FileSystemHandler;
import com.segniertomato.filesprotection.util.DateConverter;
import com.segniertomato.filesprotection.util.FileHelper;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLDataException;

public class DeletedFilesCursorAdapter extends CursorAdapter {

    private static final String LOG_TAG = DeletedFilesCursorAdapter.class.getSimpleName();

    private SparseBooleanArray mExpandState = new SparseBooleanArray();


    public DeletedFilesCursorAdapter(Context context, Cursor cursor, int flags) {
        super(context, cursor, flags);

        for (int i = 0; i < cursor.getCount(); i++) {
            mExpandState.append(i, false);
        }
    }

    public DeletedFilesCursorAdapter(Context context, Cursor cursor, boolean autoRequery) {
        super(context, cursor, autoRequery);

        for (int i = 0; i < cursor.getCount(); i++) {
            mExpandState.append(i, false);
        }
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {

        Log.d(LOG_TAG, " newView method");

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.listview_item_deleted_files, parent, false);

        ExpandableLinearLayout expandLinLayout = (ExpandableLinearLayout) view.findViewById(R.id.expandablelinearlayout);
        RelativeLayout expandButtonLayout = (RelativeLayout) view.findViewById(R.id.button_expand_layout_info);

        TextView fileName = (TextView) view.findViewById(R.id.textview_file_name);
        TextView deletedDate = (TextView) view.findViewById(R.id.textview_deleted_date);
        TextView originPath = (TextView) view.findViewById(R.id.textview_original_path);
        Button actionRestore = (Button) view.findViewById(R.id.button_action_restore);
        Button actionDelete = (Button) view.findViewById(R.id.button_action_delete);

        fileName.setSelected(true);
        originPath.setSelected(true);

        ViewHolder holder = new ViewHolder();
        holder.textViewFileName = fileName;
        holder.textViewOriginPath = originPath;
        holder.textViewDeletedDate = deletedDate;

        holder.buttonActionRestore = actionRestore;
        holder.buttonActionDelete = actionDelete;

        holder.expandLinLayout = expandLinLayout;
        holder.expandButtonLayout = expandButtonLayout;

        view.setTag(R.id.VIEW_HOLDER_TAG_KEY, holder);

        return view;
    }

    @Override
    public void bindView(final View view, final Context context, final Cursor cursor) {

        Log.d(LOG_TAG, "bindView method");

        final int position = cursor.getPosition();

        final ViewHolder holder = (ViewHolder) view.getTag(R.id.VIEW_HOLDER_TAG_KEY);

        holder.textViewFileName.setText(cursor.getString(1));
        holder.textViewOriginPath.setText(cursor.getString(2));

        String date = DateConverter.formatString2StringConsideringTimeZone(cursor.getString(4));
        holder.textViewDeletedDate.setText(date);

        holder.expandLinLayout.setInRecyclerView(false);
        holder.expandLinLayout.setInterpolator(new FastOutSlowInInterpolator());
        holder.expandLinLayout.setExpanded(mExpandState.get(position));
        holder.expandLinLayout.setListener(new ExpandableLayoutListenerAdapter() {

            @Override
            public void onPreOpen() {
                createRotateAnimator(holder.expandButtonLayout, 0f, 180f).start();
                mExpandState.put(position, true);
            }

            @Override
            public void onPreClose() {
                createRotateAnimator(holder.expandButtonLayout, 180f, 0f).start();
                mExpandState.put(position, false);
            }
        });

        holder.expandButtonLayout.setRotation(mExpandState.get(position) ? 180f : 0f);
        holder.expandButtonLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                holder.expandLinLayout.toggle();
            }
        });

        Integer columnID = Integer.valueOf(cursor.getInt(0));
        holder.buttonActionRestore.setTag(R.id.ROW_ID, columnID);
        holder.buttonActionDelete.setTag(R.id.ROW_ID, columnID);

        holder.buttonActionRestore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                try {
                    Integer rowID = (Integer) v.getTag(R.id.ROW_ID);

                    String fileName = holder.textViewFileName.getText().toString();
                    String date = holder.textViewDeletedDate.getText().toString();

                    String restoredFileName = FileHelper.getFileNameWithDeletedDate(fileName, date);
                    String destinationPath = holder.textViewOriginPath.getText().toString() + restoredFileName;

                    synchronized (this) {
                        String sourceFilePath = getTrashPathFromDB(context, rowID);

                        FileHelper.copyingFile(sourceFilePath, destinationPath);

                        if (!FileHelper.removeFile(sourceFilePath)) {
                            Toast.makeText(context, "File was restoring, but data about restored file can't be removed from a database and device storage.", Toast.LENGTH_LONG).show();
                            throw new IOException("Restored file can't be deleted from a database protected files and device storage.");
                        }

                        int countDeletedRows = deleteRowFromDB(context, rowID);

                        if (countDeletedRows == 0) {
                            Toast.makeText(context, "File was restoring, but data about restored file can't be removed from a database.", Toast.LENGTH_LONG).show();
                            throw new SQLiteException("Data about restored file can't be removed from a database.");
                        }
                    }
                    Toast.makeText(context, "Data were restored", Toast.LENGTH_SHORT).show();

                    Cursor newCursor = getUpdatedCursor(context);

                    holder.expandLinLayout.toggle();
                    mExpandState.put(cursor.getPosition(), false);

                    DeletedFilesCursorAdapter.this.changeCursor(newCursor);
//                    DeletedFilesCursorAdapter.this.notifyDataSetChanged();

                } catch (SQLException | SQLDataException | FileNotFoundException | IllegalArgumentException ex) {
                    Log.e(LOG_TAG, ex.getMessage(), ex.getCause());
                    Toast.makeText(context, ex.getMessage(), Toast.LENGTH_LONG).show();

                } catch (IOException ex) {
                    Log.e(LOG_TAG, ex.getMessage(), ex.getCause());
                    Toast.makeText(context, ex.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });

        holder.buttonActionDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                try {
                    Integer rowID = (Integer) v.getTag(R.id.ROW_ID);
                    String sourceFilePath = getTrashPathFromDB(context, rowID);

                    if (!FileHelper.removeFile(sourceFilePath)) {
                        throw new IOException("Can't delete file from devices storage.");
                    }

                    int countDeletedRows = deleteRowFromDB(context, rowID);

                    if (countDeletedRows == 0) {
                        Toast.makeText(context, "Data didn't delete", Toast.LENGTH_SHORT).show();
                        throw new SQLiteException("File was deleting, but data about restored file can't be removed from a database.");
                    }

                    Toast.makeText(context, "Data were deleted", Toast.LENGTH_SHORT).show();

                    Cursor newCursor = getUpdatedCursor(context);

                    holder.expandLinLayout.toggle();
                    mExpandState.put(cursor.getPosition(), false);

                    DeletedFilesCursorAdapter.this.changeCursor(newCursor);
//                    DeletedFilesCursorAdapter.this.notifyDataSetChanged();

                } catch (SQLiteException | SQLDataException | IOException ex) {
                    Log.e(LOG_TAG, ex.getMessage(), ex.getCause());
                    Toast.makeText(context, "Can't delete protected file.", Toast.LENGTH_LONG).show();
                }
            }
        });

    }

    private int deleteRowFromDB(Context context, int rowID) {

        Uri uri = FilesProtectionContentProvider.URI;

        String where = FeedEntry.COLUMN_ID + " = ? ";
        String[] selectionArgs = {String.valueOf(rowID)};

        return context.getContentResolver().delete(uri, where, selectionArgs);
    }

    public Cursor getUpdatedCursor(Context context) throws IllegalArgumentException {

        Uri uri = FilesProtectionContentProvider.URI;
        String sortOrder = FeedEntry.COLUMN_FILE_NAME + " ASC";
        final Cursor cursor = context.getContentResolver().query(uri, null, null, null, sortOrder);
        return cursor;
    }

    public String getTrashPathFromDB(Context context, int rowID) throws SQLDataException {

        Uri.Builder uriBuilder = new Uri.Builder();

        uriBuilder.scheme(FilesProtectionContentProvider.URI.getScheme());
        uriBuilder.authority(FilesProtectionContentProvider.URI.getAuthority());

        for (String pathSegment : FilesProtectionContentProvider.URI.getPathSegments()) {
            uriBuilder.appendPath(pathSegment);
        }
        uriBuilder.appendEncodedPath(String.valueOf(rowID));

        String[] projection = {FeedEntry.COLUMN_TRASH_PATH};
        Cursor itemRowCursor = context.getContentResolver().query(uriBuilder.build(), projection, null, null, null);

        if (itemRowCursor != null && itemRowCursor.moveToFirst()) {
            String trashFilePath = itemRowCursor.getString(0);

            if (trashFilePath != null) {
                return trashFilePath;

            } else {
                throw new SQLDataException("Can't get data from database.");
            }
        } else {
            throw new SQLDataException("Can't get data from database.");
        }

    }

    private ObjectAnimator createRotateAnimator(final View target, final float from, final float to) {
        ObjectAnimator animator = ObjectAnimator.ofFloat(target, "rotation", from, to);
        animator.setDuration(300);
        animator.setInterpolator(Utils.createInterpolator(Utils.LINEAR_INTERPOLATOR));
        return animator;
    }

    private class ViewHolder {
        private TextView textViewFileName;
        private TextView textViewDeletedDate;
        private TextView textViewOriginPath;

        private Button buttonActionRestore;
        private Button buttonActionDelete;

        private ExpandableLinearLayout expandLinLayout;
        private RelativeLayout expandButtonLayout;
    }
}
