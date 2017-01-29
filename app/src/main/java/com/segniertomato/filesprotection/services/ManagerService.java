package com.segniertomato.filesprotection.services;

import android.app.ActivityManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.RemoteException;
import android.util.Log;

import com.segniertomato.filesprotection.preferences.SettingsKey;
import com.segniertomato.filesprotection.storage.StorageHelper;
import com.segniertomato.filesprotection.util.Constants;
import com.segniertomato.filesprotection.util.FileHelper;

import java.util.ArrayList;
import java.util.List;


public class ManagerService extends Service {

    private static final String LOG_TAG = ManagerService.class.getSimpleName();

    final Messenger mMessenger = new Messenger(new IncomingHandler());

    private BroadcastReceiver mRebootBrReceiver = null;
    private BroadcastReceiver mMediaStateBrReceiver = null;
    private BroadcastReceiver mChangePrefBrReceiver = null;

    private Messenger mServiceMessenger = null;

    private ServiceConnection mRemoteConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

            Log.d(LOG_TAG, "onServiceConnected method");
            mServiceMessenger = new Messenger(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mServiceMessenger = null;
        }

    };

    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }

    @Override
    public void onCreate() {

        Log.d(LOG_TAG, "onCreate method");

        mChangePrefBrReceiver = new PreferenceChangeBroadcastReceiver(this);
        mRebootBrReceiver = new RebootBroadcastReceiver();

        registerReceiver(mChangePrefBrReceiver, new IntentFilter(Constants.INTENT_FILTER_CHANGE_PREF));

        mMediaStateBrReceiver = new MediaStateBroadcastReceiver(this);

        IntentFilter mediaIntentFilter = new IntentFilter();
        mediaIntentFilter.addAction("android.intent.action.MEDIA_MOUNTED");
        mediaIntentFilter.addAction("android.intent.action.MEDIA_UNMOUNTED");
        mediaIntentFilter.addAction("android.intent.action.MEDIA_EJECT");
        mediaIntentFilter.addAction("android.intent.action.MEDIA_REMOVED");

        registerReceiver(mMediaStateBrReceiver, mediaIntentFilter);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.d(LOG_TAG, "onStartCommand method");

        Thread initThread = new Thread(new Runnable() {

            @Override
            public void run() {

                Context context = getApplicationContext();

                Intent startObserverService = new Intent(context, ObserverService.class);
                ManagerService.this.startService(startObserverService);

                boolean isBound = getObserverService();
                Log.d(LOG_TAG, "Bound with Observer Service is: " + isBound);
            }
        });
        initThread.start();

//        return START_REDELIVER_INTENT;
        return START_STICKY;
    }

    private boolean getObserverService() {

        Intent intent = new Intent(this, ObserverService.class);
        return bindService(intent, mRemoteConnection, Context.BIND_ABOVE_CLIENT);
    }

    private String[] readPreferences() {

        String[] paths = {"", ""};

        try {

            SharedPreferences sharedPref = this.getSharedPreferences(SettingsKey.APP_SHARED_PREF, Context.MODE_PRIVATE);

            paths = new String[2];

            paths[0] = sharedPref.getString(SettingsKey.KEY_INPUT_PATH_1, null);
            paths[1] = sharedPref.getString(SettingsKey.KEY_INPUT_PATH_2, null);

        } catch (Exception ex) {
            Log.e(LOG_TAG, ex.getMessage(), ex.getCause());
        }

        return paths;
    }

    private Bundle packStringPaths2Bundle(String[] paths) {

        Bundle bundle = new Bundle();

        if (paths == null) {
            return bundle;
        }

        ArrayList<String> listPaths = new ArrayList<>(2);

        for (String path : paths) {

            boolean isPath = FileHelper.isPathExist(path);
            if (isPath) {
                listPaths.add(path);
            }
        }

        if (listPaths.isEmpty()) {

            StorageHelper helper = StorageHelper.getInstance();

            List<StorageHelper.MountDevice> externalDevices = helper.getExternalMountedDevices();
            List<StorageHelper.MountDevice> removableDevices = helper.getRemovableMountedDevices();

            for (StorageHelper.MountDevice device : externalDevices) {
                listPaths.add(device.getPath());
                break;
            }

            for (StorageHelper.MountDevice device : removableDevices) {
                listPaths.add(device.getPath());
                break;
            }
        }

        boolean[] arrayIsRecursive = new boolean[listPaths.size()];

        for (int i = 0; i < listPaths.size(); i++) {
            arrayIsRecursive[i] = true;
        }

        bundle.putStringArrayList(Constants.BUNDLE_INIT_PARAM_ROOT_PATH_ARRAY_LIST, listPaths);
        bundle.putBooleanArray(Constants.BUNDLE_INIT_PARAM_BOOLEAN_IS_RECURSIVE_ARRAY, arrayIsRecursive);

        return bundle;
    }

    @Override
    public void onDestroy() {

        Log.d(LOG_TAG, "onDestroy method");

        if (mRemoteConnection != null) {
            unbindService(mRemoteConnection);
        }

        stopService(new Intent(this, ObserverService.class));

        unregisterReceiver(this.mChangePrefBrReceiver);
        unregisterReceiver(this.mMediaStateBrReceiver);

        super.onDestroy();
    }

    public static class RebootBroadcastReceiver extends BroadcastReceiver {

        private static final String LOG_TAG = RebootBroadcastReceiver.class.getSimpleName();
        private static final String WAKE_LOCK_TAG = RebootBroadcastReceiver.class.getName();

        @Override
        public void onReceive(Context context, Intent intent) {
            new RestartThread(context).start();
        }

        class RestartThread extends Thread {

            private Context mContext;

            public RestartThread(Context context) {
                super();
                this.mContext = context;

            }

            public void run() {

                PowerManager.WakeLock wLock = null;

                try {

                    PowerManager powerManager = (PowerManager) this.mContext.getSystemService(Context.POWER_SERVICE);
                    wLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK |
                            PowerManager.ACQUIRE_CAUSES_WAKEUP, WAKE_LOCK_TAG);
                    wLock.acquire();

                    Log.d(LOG_TAG, "Restart broadcast receiver is triggered");

                    ActivityManager manager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
                    for (ActivityManager.RunningServiceInfo serviceInfo : manager.getRunningServices(Integer.MAX_VALUE)) {

                        if (ManagerService.class.getName().equals(serviceInfo.service.getClassName())) {
                            Log.d(LOG_TAG, "Manger Service is running.");
                        }

                        if (ObserverService.class.getName().equals(serviceInfo.service.getClassName())) {
                            Log.d(LOG_TAG, "Observer service is running.");
                        }
                    }

                    Intent startManagerService = new Intent(mContext, ManagerService.class);
                    mContext.startService(startManagerService);


                } catch (Exception ex) {

                } finally {
                    if (wLock != null) {
                        wLock.release();
                    }
                }
            }
        }
    }

    public static class MediaStateBroadcastReceiver extends BroadcastReceiver {

        private static final String LOG_TAG = MediaStateBroadcastReceiver.class.getSimpleName();
        private ManagerService managerService;

        public MediaStateBroadcastReceiver() {
            super();
            Log.d(LOG_TAG, "Constructor is empty");
        }

        public MediaStateBroadcastReceiver(ManagerService service) {
            super();

            Log.d(LOG_TAG, "Constructor with parameter");
            managerService = service;
        }

        @Override
        public void onReceive(Context context, Intent intent) {

            Log.d(LOG_TAG, "Media State Broadcast Receiver");
            Log.d(LOG_TAG, "Intent action: " + intent.getAction());

            try {

                if (managerService == null) {
                    Log.d(LOG_TAG, "Reference to Manager Service is null.");
                    return;
                }

                if (managerService.mServiceMessenger == null) {
                    Log.d(LOG_TAG, "ServiceMessenger is null");
                    managerService.getObserverService();
                }

                if (managerService.mServiceMessenger != null) {
                    String[] protectedPath = managerService.readPreferences();
                    Bundle bundle = managerService.packStringPaths2Bundle(protectedPath);

                    Message msg = Message.obtain(null, Constants.ServiceMessage.MSG_UPDATE_FILE_OBSERVER, 0, 0);
                    msg.setData(bundle);

                    managerService.mServiceMessenger.send(msg);
                }

            } catch (RemoteException ex) {
                Log.e(LOG_TAG, ex.getMessage(), ex.getCause());
            }
        }
    }

    public static class PreferenceChangeBroadcastReceiver extends BroadcastReceiver {

        private static final String LOG_TAG = PreferenceChangeBroadcastReceiver.class.getSimpleName();
        private ManagerService managerService;


        public PreferenceChangeBroadcastReceiver() {
            Log.d(LOG_TAG, "Empty constructor");
        }

        public PreferenceChangeBroadcastReceiver(ManagerService service) {
            Log.d(LOG_TAG, "Constructor with parameter");
            managerService = service;
        }

        @Override
        public void onReceive(Context context, Intent intent) {

            Log.d(LOG_TAG, "Preference Change Broadcast Receiver");
            Log.d(LOG_TAG, "Intent action: " + intent.getAction());

            try {

                if (managerService == null) {
                    Log.d(LOG_TAG, "Reference to Manager Service is null.");
                    return;
                }

                if (managerService.mServiceMessenger == null) {
                    Log.d(LOG_TAG, "ServiceMessenger is null");
                    managerService.getObserverService();
                }
                if (managerService.mServiceMessenger != null) {

                    String[] protectedPaths = managerService.readPreferences();
                    Bundle bundle = managerService.packStringPaths2Bundle(protectedPaths);

                    Message msg = Message.obtain(null, Constants.ServiceMessage.MSG_UPDATE_FILE_OBSERVER, 0, 0);
                    msg.setData(bundle);

                    managerService.mServiceMessenger.send(msg);
                }

            } catch (RemoteException ex) {
                Log.e(LOG_TAG, ex.getMessage(), ex.getCause());
            }
        }
    }

    class IncomingHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {

                case Constants.ServiceMessage.MSG_GET_FILES_PATH:

                    Log.d(LOG_TAG, "ManagerService Handler: MSG_GET_FILES_PATH");

                    try {

                        if (mServiceMessenger != null) {

                            String[] protectedPaths = readPreferences();

                            Log.d(LOG_TAG, "Protected paths: " + protectedPaths[0] + "\n" + protectedPaths[1]);
                            Bundle bundle = packStringPaths2Bundle(protectedPaths);

                            Message responseMessage = Message.obtain(null, Constants.ServiceMessage.MSG_UPDATE_FILE_OBSERVER, 0, 0);
                            responseMessage.setData(bundle);

                            mServiceMessenger.send(responseMessage);
                            Log.d(LOG_TAG, "Data was sending");

                        } else {
                            Log.d(LOG_TAG, "Service Messenger is null.");
                        }

                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }

                    break;

            }
            super.handleMessage(msg);
        }

    }


}
