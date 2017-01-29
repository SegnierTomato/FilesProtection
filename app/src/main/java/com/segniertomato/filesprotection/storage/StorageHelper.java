package com.segniertomato.filesprotection.storage;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.StatFs;
import android.provider.SyncStateContract;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.segniertomato.filesprotection.util.Constants;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class StorageHelper {

    private final static String LOG_TAG = StorageHelper.class.getSimpleName();

    private static StorageHelper sStorage;
    private MountDevicesScanner mMountScanner;

    private StorageHelper() {

        mMountScanner = this.new MountDevicesScanner();
        mMountScanner.scanMemory();
    }

    public static StorageHelper getInstance() {
        if (sStorage == null) {
            sStorage = new StorageHelper();
        }
        return sStorage;
    }

    public void refreshMountedDevices() {
        mMountScanner.scanMemory();
    }

    public ArrayList<MountDevice> getAllMountedDevices() {

        ArrayList<MountDevice> mountedDevice = new ArrayList<StorageHelper.MountDevice>();

        mountedDevice.addAll(mMountScanner.getMountedExternalDevices());
        mountedDevice.addAll(mMountScanner.getMountedRemovableDevices());

        return mountedDevice;
    }

    public ArrayList<MountDevice> getExternalMountedDevices() {
        return mMountScanner.getMountedExternalDevices();
    }

    public ArrayList<MountDevice> getRemovableMountedDevices() {
        return mMountScanner.getMountedRemovableDevices();
    }

    public String getApplicationDataDirectory(Context context, final String filePath) throws NullPointerException, IllegalArgumentException {

        if (context == null || filePath == null) {

            String msg = context == null ? "First argument is invalid. Context can't be null." : "Second argument is invalid. File path can't be null.";
            throw new NullPointerException(msg);

        } else if (filePath.isEmpty()) {
            throw new IllegalArgumentException("Second argument is invalid. File path can't be empty.");
        }

        String mountPath = getMountDeviceForPath(filePath);

        String packageName = null;
        try {
            packageName = getPackageName(context);

        } catch (NullPointerException ex) {
            packageName = Constants.PACKAGE_PREFIX;
        }

        StringBuilder appDataDir = new StringBuilder();

        if (mountPath == null) {

            if (filePath.indexOf(Constants.INNER_DIR_PREFIX) == 0) {
                appDataDir.append(Constants.INNER_DIR_PREFIX);

            } else {
                String[] fileDirsName = filePath.split(File.separator);
                appDataDir.append(File.separator + fileDirsName[1]);
                appDataDir.append(Constants.EXTERNAL_DIR_PREFIX);
            }
        }else{
            appDataDir.append(mountPath);
            appDataDir.append(Constants.EXTERNAL_DIR_PREFIX);
        }

        appDataDir.append(File.separator);
        appDataDir.append(packageName);

        return appDataDir.toString();
    }

    private String getMountDeviceForPath(final String path) {

        String mountPath = null;

        if (path != null) {

            List<MountDevice> mountDeviceList = getAllMountedDevices();
            for (MountDevice mountDevice : mountDeviceList) {

                int index = path.indexOf(mountDevice.getPath());
                if (index != -1) {
                    mountPath = mountDevice.getPath();
                    break;
                }
            }
        }
        return mountPath;
    }

    private String getPackageName(Context context) throws NullPointerException {

        if (context == null) {
            throw new NullPointerException("Parameter is invalid. Context can't be null.");
        }
        return context.getPackageName();
    }

    public enum MountDeviceType {
        EXTERNAL_SD_CARD, REMOVABLE_SD_CARD
    }

    public class MountDevice {
        private MountDeviceType mType;
        private String mPath;
        private Integer mHash;

        public MountDevice(MountDeviceType type, String path, Integer hash) {
            super();
            mType = type;
            mPath = path;
            mHash = hash;
        }

        /**
         * @return the type
         */
        public final MountDeviceType getType() {
            return mType;
        }

        /**
         * @return the path
         */
        public final String getPath() {
            return mPath;
        }

        /**
         * @return the hash
         */
        public final Integer getHash() {
            return mHash;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals(Object obj) {

            MountDevice mountObj = (MountDevice) obj;

            if (!mPath.equals(mountObj.getPath())) {
                return mHash.equals(mountObj.getHash());
            }

            return true;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
            return mHash;
        }

    }

    private class MountDevicesScanner {
        private ArrayList<MountDevice> mMountedExternalDevices = null;
        private ArrayList<MountDevice> mMountedRemovableDevices = null;

        private int calcHash(File dir) {
            StringBuilder tmpHash = new StringBuilder();

            tmpHash.append(dir.getTotalSpace());
            tmpHash.append(dir.getUsableSpace());

            File[] list = dir.listFiles();
            for (File file : list) {
                tmpHash.append(file.getName());
                if (file.isFile()) {
                    tmpHash.append(file.length());
                }
            }

            return tmpHash.toString().hashCode();

        }

        /**
         * @return the mountedExternalDevices
         */
        public ArrayList<MountDevice> getMountedExternalDevices() {
            return mMountedExternalDevices;
        }

        /**
         * @return the mountedDevices
         */
        public ArrayList<MountDevice> getMountedRemovableDevices() {
            return mMountedRemovableDevices;
        }

        public void scanMemory() {

            List<MountDevice> mountDevicesEnvList = getDevicesWithEnvironment();
            List<MountDevice> mountDevicesProcList = getDevicesWithProcess();

            for (MountDevice mountDeviceEnv : mountDevicesEnvList) {

                String mountDevEnvPath = mountDeviceEnv.getPath();
                Iterator<MountDevice> iterator = mountDevicesProcList.iterator();

                while (iterator.hasNext()) {
                    MountDevice mountDeviceProc = iterator.next();

                    String mountDevProcPath = mountDeviceProc.getPath();
                    if (mountDevEnvPath.equals(mountDevProcPath)) {
                        iterator.remove();
                        break;
                    }
                }
            }

            mountDevicesEnvList.addAll(mountDevicesProcList);

            mMountedExternalDevices = new ArrayList<>(mountDevicesEnvList.size());
            mMountedRemovableDevices = new ArrayList<>(mountDevicesEnvList.size());

            for (MountDevice mountDevice : mountDevicesEnvList) {

                MountDeviceType typeDevice = mountDevice.getType();
                if (typeDevice == MountDeviceType.EXTERNAL_SD_CARD) {
                    mMountedExternalDevices.add(mountDevice);
                } else {
                    mMountedRemovableDevices.add(mountDevice);
                }
            }
        }

        private List<MountDevice> getDevicesWithEnvironment() {

            List<MountDevice> mountDevices = new ArrayList<>(3);

            // get external
            String path = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();

            if (!path.trim().isEmpty() && android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) {

                MountDevice device = checkAndGetMountDevice(path, MountDeviceType.EXTERNAL_SD_CARD);

                if (device != null) {
                    mountDevices.add(device);
                }

            }

            // get removable
            String rawSecondaryStoragesStr = System.getenv("SECONDARY_STORAGE");
            if (rawSecondaryStoragesStr != null && !rawSecondaryStoragesStr.isEmpty()) {
                // All Secondary SD-CARDs splited into array
                final String[] rawSecondaryStorages = rawSecondaryStoragesStr.split(File.pathSeparator);

                for (String rawSecondaryStorage : rawSecondaryStorages) {
                    MountDevice device = checkAndGetMountDevice(path, MountDeviceType.REMOVABLE_SD_CARD);

                    if (device != null) {
                        mountDevices.add(device);
                    }
                }
            }

            return mountDevices;
        }

        private List<MountDevice> getDevicesWithProcess() {

            List<MountDevice> mountDevices = new ArrayList<>(3);

            InputStream inputStream = null;
            InputStreamReader inputStreamReader = null;
            BufferedReader bufferedReader = null;

            Process proc = null;
            String line;

            try {

                Runtime runtime = Runtime.getRuntime();
                proc = runtime.exec("mount");

                try {

                    inputStream = proc.getInputStream();
                    inputStreamReader = new InputStreamReader(inputStream);
                    bufferedReader = new BufferedReader(inputStreamReader);

                    while ((line = bufferedReader.readLine()) != null) {

                        if (line.contains("secure") || line.contains("asec")) {
                            continue;
                        }

                        if (line.contains("fat") || line.contains("ntfs")) {// TF card

                            String columns[] = line.split(" ");

                            if (columns != null && columns.length > 1) {

                                MountDevice device = checkAndGetMountDevice(columns[1], MountDeviceType.REMOVABLE_SD_CARD);

                                if (device != null) {
                                    mountDevices.add(device);
                                }
                            }

                        } else if (line.contains("fuse")) {// internal(External)
                            // storage
                            String columns[] = line.split(" ");

                            if (columns != null && columns.length > 1) {
                                // mount = mount.concat(columns[1] + "\n");
                                MountDevice device = checkAndGetMountDevice(columns[1], MountDeviceType.EXTERNAL_SD_CARD);

                                if (device != null) {
                                    mountDevices.add(device);
                                }
                            }
                        }
                    }
                } finally {
                    if (bufferedReader != null) {
                        bufferedReader.close();
                    }
                    if (inputStreamReader != null) {
                        inputStreamReader.close();
                    }
                    if (proc != null) {
                        proc.destroy();
                    }
                    if (inputStream != null) {
                        inputStream.close();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            return mountDevices;
        }

        @Nullable
        private MountDevice checkAndGetMountDevice(final String path, MountDeviceType type) {

            MountDevice device = null;

            File root = new File(path);
            if (root.exists() && root.isDirectory() && root.canWrite()) {
                device = new MountDevice(type, path, calcHash(root));
            }

            return device;
        }
    }
}
