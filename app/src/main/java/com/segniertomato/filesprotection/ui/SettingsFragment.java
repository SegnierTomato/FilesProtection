package com.segniertomato.filesprotection.ui;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

//import android.support.v7.preference.EditTextPreferenceFix;
import com.segniertomato.filesprotection.util.Constants;
import com.takisoft.fix.support.v7.preference.EditTextPreference;

import android.support.v7.preference.Preference;
import android.util.Log;
import android.widget.Toast;

import com.segniertomato.filesprotection.R;
import com.segniertomato.filesprotection.preferences.SettingsKey;
import com.segniertomato.filesprotection.storage.StorageHelper;
import com.segniertomato.filesprotection.util.FileHelper;
import com.takisoft.fix.support.v7.preference.PreferenceFragmentCompatDividers;

import java.io.File;
import java.util.List;


public class SettingsFragment extends PreferenceFragmentCompatDividers implements SharedPreferences.OnSharedPreferenceChangeListener, Preference.OnPreferenceChangeListener {

    private static final String LOG_TAG = SettingsFragment.class.getSimpleName();

    private EditTextPreference inputFeeld1;
    private EditTextPreference inputFeeld2;

    private Context mContext;

    private SharedPreferences mSharedPref;

    @Override
    public void onCreatePreferencesFix(Bundle bundle, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
    }

    @Override
    public void onStart() {
        super.onStart();

        Activity activity = getActivity();
        mContext = activity;

        setDividerPreferences(DIVIDER_CATEGORY_BETWEEN);

        mSharedPref = mContext.getSharedPreferences(SettingsKey.APP_SHARED_PREF, Context.MODE_PRIVATE);
        mSharedPref.registerOnSharedPreferenceChangeListener(this);

        inputFeeld1 = (EditTextPreference) findPreference(SettingsKey.KEY_INPUT_PATH_1);
        inputFeeld2 = (EditTextPreference) findPreference(SettingsKey.KEY_INPUT_PATH_2);

        String firstPath = inputFeeld1.getText();
        String secondPath = inputFeeld2.getText();

        boolean isFirstPath = FileHelper.isPathExist(firstPath);
        boolean isSecondPath = FileHelper.isPathExist(secondPath);

        if (isFirstPath || isSecondPath) {

            if (isFirstPath && !isSecondPath) {
                secondPath = "";
            } else if (!isFirstPath && isSecondPath) {
                firstPath = "";
            }

        } else {

            String[] defaultPaths = defineDefaultValue();

            firstPath = defaultPaths[0];
            secondPath = defaultPaths[1];
        }

        SharedPreferences.Editor editor = mSharedPref.edit();

        if (FileHelper.isPathExist(firstPath)) {
            editor.putString(SettingsKey.KEY_INPUT_PATH_1, firstPath);
        }

        if (FileHelper.isPathExist(secondPath)) {
            editor.putString(SettingsKey.KEY_INPUT_PATH_2, secondPath);
        }

        editor.apply();

        inputFeeld1.setText(firstPath);
        inputFeeld2.setText(secondPath);

        inputFeeld1.setSummary(firstPath);
        inputFeeld2.setSummary(secondPath);


        inputFeeld1.setOnPreferenceChangeListener(this);
        inputFeeld2.setOnPreferenceChangeListener(this);
    }

    private String[] defineDefaultValue() {

        String[] paths = {"", ""};

        StorageHelper helper = StorageHelper.getInstance();

        List<StorageHelper.MountDevice> externalDevices = helper.getExternalMountedDevices();
        List<StorageHelper.MountDevice> removableDevices = helper.getRemovableMountedDevices();

        for (StorageHelper.MountDevice externalDevice : externalDevices) {

            paths[0] = externalDevice.getPath();

            boolean isNotSamePath = false;
            for (StorageHelper.MountDevice removableDevice : removableDevices) {

                if (!externalDevice.getPath().equals(removableDevice.getPath())) {
                    paths[1] = removableDevice.getPath();
                    isNotSamePath = true;
                    break;
                }
            }

            if (isNotSamePath) {
                break;
            }
        }

        if (!paths[0].isEmpty()) {
            paths[0] += File.separator;
        }

        if (!paths[1].isEmpty()) {
            paths[1] += File.separator;
        }

        return paths;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }


    @Override
    public void onPause() {
        mSharedPref.unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        Log.d(LOG_TAG, "onSharedPreferenceChanged");

        Toast.makeText(mContext, "Preferences changed", Toast.LENGTH_SHORT).show();

        Context context = getActivity().getApplicationContext();
        context.sendBroadcast(new Intent(Constants.INTENT_FILTER_CHANGE_PREF));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {

        Log.d(LOG_TAG, "onPreferenceChange");

        EditTextPreference editPreference = (EditTextPreference) preference;
        String key = editPreference.getKey();
        String newPath = (String) newValue;

        boolean isValid = isValidateEditInputValue(key, newPath);

        if (isValid) {
            preference.setSummary(newPath);
            SharedPreferences.Editor editor = mSharedPref.edit();
            editor.putString(key, newPath);
            editor.apply();

        } else {
            Toast.makeText(mContext, "Path is invalid. Please, try again.", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private boolean isValidateEditInputValue(String key, String path) {

        if (key != null && path != null) {

            if (FileHelper.isPathExist(path)) {

                if (key.equals(inputFeeld1.getKey()) && path.equals(inputFeeld2.getText())) {
                    return false;

                } else if (key.equals(inputFeeld2.getKey()) && path.equals(inputFeeld1.getText())) {
                    return false;
                }
                return true;
            }
        }
        return false;
    }
}
