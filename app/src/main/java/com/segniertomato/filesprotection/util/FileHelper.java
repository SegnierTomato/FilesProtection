package com.segniertomato.filesprotection.util;

import android.util.Log;

//import com.google.common.io.Closer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FileHelper {

    private static final String LOG_TAG = FileHelper.class.getSimpleName();

    public static boolean isPathExist(final String candidatePath) {

        if (candidatePath != null) {
            File file = new File(candidatePath);

            if (file != null && file.exists()) {
                return true;
            }
        }
        return false;
    }

    public static boolean isFile(final String pathFile) {

        if (pathFile != null) {
            File file = new File(pathFile);

            if (file != null && file.exists() && file.isFile()) {
                return true;
            }
        }
        return false;
    }

    public static String getFileNameWithDeletedDate(final String fileName, final String deletedDate) throws IllegalArgumentException {

        if (fileName == null || deletedDate == null) {
            String msg = fileName == null ? "First parameter is invalid. FileName can't be null." : "Second parameter is invalid. Deleted date can't be null.";
            throw new IllegalArgumentException(msg);
        }

        String[] fileNameFragments = fileName.split("\\.");
        StringBuilder recoveryFileNameBuilder = new StringBuilder();

        int i = 0;
        for (; i < fileNameFragments.length - 2; i++) {
            recoveryFileNameBuilder.append(fileNameFragments[i]);
            recoveryFileNameBuilder.append(".");
        }

        recoveryFileNameBuilder.append(fileNameFragments[i]);

        recoveryFileNameBuilder.append("_");
        recoveryFileNameBuilder.append(deletedDate);

        if (fileNameFragments.length != 1) {
            recoveryFileNameBuilder.append(".");
            recoveryFileNameBuilder.append(fileNameFragments[fileNameFragments.length - 1]);
        }

        return recoveryFileNameBuilder.toString();
    }

    public static void copyingFile(final String sourcePath, final String destinationPath) throws IllegalArgumentException, FileNotFoundException, IOException {

        if (sourcePath == null || destinationPath == null || sourcePath.isEmpty() || destinationPath.isEmpty()) {

            StringBuilder msg = new StringBuilder("Incoming parameter is invalid. ");
            if (sourcePath == null || destinationPath == null) {
                msg.append(sourcePath == null ? "Source path can't be null." : "Destination path can't be null.");
            } else {
                msg.append(sourcePath.isEmpty() ? "Source path can't be empty." : "Destination path can't be empty.");
            }
            throw new IllegalArgumentException(msg.toString());
        }

        File sourceFile = new File(sourcePath);
        if (sourceFile.exists() && sourceFile.isFile()) {

//            Closer closer = Closer.create();

            File recoveryFile = new File(destinationPath);
            recoveryFile.getParentFile().mkdirs();
            recoveryFile.createNewFile();

            InputStream fileInStream = null;
            OutputStream fileOutputStream = null;

            try {
//                InputStream fileInStream = closer.register(new FileInputStream(sourceFile));
//                OutputStream fileOutputStream = closer.register(new FileOutputStream(destinationPath));

                fileInStream = new FileInputStream(sourceFile);
                fileOutputStream = new FileOutputStream(recoveryFile);

                int offset = 0;
                int readByte = 0;
                byte[] readBuffer = new byte[4096];

                while ((readByte = fileInStream.read(readBuffer)) != -1) {
                    fileOutputStream.write(readBuffer, 0, readByte);
                }

            } catch (Throwable e) {
                Log.e(LOG_TAG, e.getMessage(), e.getCause());
//                throw closer.rethrow(e);

            } finally {
//                closer.close();
                if(fileInStream!=null){
                    fileInStream.close();
                }

                if(fileOutputStream!=null){
                    fileOutputStream.close();
                }
            }

        } else {
            throw new FileNotFoundException("Can't find a restored file on a trash folder.");
        }
    }

    public static boolean removeFile(final String pathFile) {

        if (pathFile != null) {

            File deletingFile = new File(pathFile);
            if (deletingFile.exists()) {
                return deletingFile.delete();
            }
        }

        return false;
    }
}
