package com.segniertomato.filesprotection.ui;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;

//import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.PreferenceManager;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Toast;


import com.segniertomato.filesprotection.R;
import com.segniertomato.filesprotection.services.ManagerService;
import com.segniertomato.filesprotection.services.ObserverService;
import com.segniertomato.filesprotection.util.Constants;


public class MainActivity extends AppCompatActivity {


    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    private Menu mMenu;

    private AlertDialog permissionDialogInfo = null;

    private ServiceState mState = ServiceState.STOP;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        if (savedInstanceState == null) {

            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

            DeletedFilesViewFragment delFilesFragment = new DeletedFilesViewFragment();
            final String tag = DeletedFilesViewFragment.class.getName();

            fragmentTransaction.add(R.id.fragment_parent_layout, delFilesFragment, tag);
            fragmentTransaction.addToBackStack(tag);
            int result = fragmentTransaction.commit();

            if (result < 0) {
                Toast.makeText(this, Constants.MessageError.MESSAGE_ERROR_RUN_DELETED_FILES_FRAGMENT, Toast.LENGTH_SHORT).show();
                finish();
            }
        }

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            permissionDialogInfo = createPermissionDialog();
        }

    }

    private AlertDialog createPermissionDialog() {

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setPositiveButton(R.string.permission_dialog_try_again_action, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int id) {
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, R.id.REQUEST_WRITE_EXTERNAL_STORAGE);
                }
            }
        });

        dialogBuilder.setNegativeButton(R.string.permission_dialog_exit_action, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
                    finish();
                }
            }
        });

        dialogBuilder.setTitle(R.string.permission_dialog_title);
        dialogBuilder.setMessage(R.string.permission_dialog_message);

        return dialogBuilder.create();
    }

    private void updateServicesStatusIcon(boolean isRun) {

        MenuItem item = null;

        if (mMenu != null) {
            item = mMenu.findItem(R.id.service_state);

        } else {
            Log.d(LOG_TAG, "Menu object is null.");
            return;
        }

        String stateTitle = null;
        int iconResource = -1;

        if (isRun) {
            stateTitle = getResources().getString(R.string.service_state_stop_action);
            iconResource = R.drawable.ic_stop_black_48px;

        } else {
            stateTitle = getResources().getString(R.string.service_state_start_action);
            iconResource = R.drawable.ic_start_black_48px;
        }

        Drawable iconServiceStatus = null;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            iconServiceStatus = getResources().getDrawable(iconResource);
        } else {
            iconServiceStatus = getResources().getDrawable(iconResource, this.getTheme());
        }

        item.setTitle(stateTitle);
        item.setIcon(iconServiceStatus);
    }


    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);

        for (ActivityManager.RunningServiceInfo serviceInfo : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(serviceInfo.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.activity_main, menu);
        this.mMenu = menu;

        boolean isManagerServiceRunning = isServiceRunning(ManagerService.class);
        boolean isObserverServiceRunning = isServiceRunning(ObserverService.class);

        String statusService = null;

        statusService = isManagerServiceRunning ? "started" : "stopped";
        Toast.makeText(this, "Manager service " + statusService, Toast.LENGTH_SHORT).show();

        statusService = isObserverServiceRunning ? "started" : "stopped";
        Toast.makeText(this, "Observer service " + statusService, Toast.LENGTH_SHORT).show();

        if (isObserverServiceRunning) {
            mState = ServiceState.START;
            updateServicesStatusIcon(true);

        } else {
            mState = ServiceState.STOP;
            updateServicesStatusIcon(false);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
        switch (id) {

            case R.id.settings:

                FragmentManager fragmentManager = getSupportFragmentManager();

                String higherStackTag = fragmentManager.getBackStackEntryAt(fragmentManager.getBackStackEntryCount() - 1).getName();

                if (higherStackTag.equals(SettingsFragment.class.getName())) {
                    return true;
                }

                SettingsFragment settFragment = new SettingsFragment();
                final String currOperationTag = settFragment.getClass().getName();

                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

                fragmentTransaction.replace(R.id.fragment_parent_layout, settFragment, currOperationTag);
                fragmentTransaction.addToBackStack(currOperationTag);
                int result = fragmentTransaction.commit();

                if (result < 0) {
                    Toast.makeText(this, Constants.MessageError.MESSAGE_ERROR_RUN_SETTINGS_FRAGMENT, Toast.LENGTH_SHORT).show();
                }

                return true;

            case R.id.service_state:

                if (mState == ServiceState.START) {
                    if (changeServiceStatus(ServiceState.STOP)) {
                        updateServicesStatusIcon(false);
                    }

                } else {
                    if (changeServiceStatus(ServiceState.START)) {
                        updateServicesStatusIcon(true);
                    }
                }

                return true;

            default:
                return super.onOptionsItemSelected(item);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case R.id.REQUEST_WRITE_EXTERNAL_STORAGE:

                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (changeServiceStatus(ServiceState.START)) {
                        updateServicesStatusIcon(true);
                    }

                } else {
                    permissionDialogInfo.show();

                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
                        Button exitButton = permissionDialogInfo.getButton(DialogInterface.BUTTON_POSITIVE);
                        boolean enableValue = !shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE) ? false : true;
                        exitButton.setEnabled(enableValue);
                    }

                }
                break;

            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private boolean changeServiceStatus(ServiceState state) {

        switch (state) {

            case START:

                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {

                    if (this.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        this.requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, R.id.REQUEST_WRITE_EXTERNAL_STORAGE);
                        return false;
                    }
                }

                this.startService(new Intent(this, ManagerService.class));
                mState = ServiceState.START;
                Toast.makeText(this, "Services started", Toast.LENGTH_SHORT).show();
                return true;

            case STOP:

                this.stopService(new Intent(this, ManagerService.class));
                mState = ServiceState.STOP;
                Toast.makeText(this, "Services stopped", Toast.LENGTH_SHORT).show();
                return true;

            default:
                Log.d(LOG_TAG, "Un");
                return false;
        }
    }


    @Override
    public void onBackPressed() {

        FragmentManager fragmentManager = getSupportFragmentManager();

        Fragment fragment = fragmentManager.findFragmentByTag(SettingsFragment.class.getName());

        if (fragment != null) {

            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            boolean isPopBack = fragmentManager.popBackStackImmediate();

            if (isPopBack) {
                fragmentTransaction.commit();
            } else {
                Log.e(LOG_TAG, Constants.MessageError.MESSAGE_ERROR_PRESS_BACK_BUTTON);
            }

        } else {
            super.onBackPressed();
            finish();
        }
    }

    private enum ServiceState {
        START,
        STOP
    }
}
