package com.segniertomato.filesprotection.util;


import java.io.File;

public final class Constants {

    public static final String BUNDLE_INIT_PARAM_ROOT_PATH_ARRAY_LIST = "BUNDLE_INIT_STRING_ARRAY_LIST";
    public static final String BUNDLE_INIT_PARAM_BOOLEAN_IS_RECURSIVE_ARRAY = "BUNDLE_INIT_BOOLEAN_ARRAY";

    public static final String INTENT_PARAM_COPY_SOURCE_PATH = "INTENT_COPY_SOURCE_PATH";
    public static final String INTENT_PARAM_COPY_DESTIN_PATH = "INTENT_COPY_DESTIN_PATH";

    public static final String TRASH_FOLDER_NAME = ".trash";
    public static final String EXTERNAL_DIR_PREFIX = "/Android/data";
    public static final String INNER_DIR_PREFIX = "/data/data";
    public static final String PACKAGE_PREFIX = "/com.segniertomato.filesprotection";
    public static final String DEFAULT_TRASH_FOLDER_PATH = INNER_DIR_PREFIX +  PACKAGE_PREFIX + File.separator + TRASH_FOLDER_NAME;

    public static final String INTENT_FILTER_CHANGE_PREF= "com.segniertomato.filesprotection.PREFERENCES_CHANGED";

    private Constants() {

    }

    public static final class MessageError {

        public static final String MESSAGE_ERROR_RUN_SETTINGS_FRAGMENT = "Can't run settings fragment. Please try again.";
        public static final String MESSAGE_ERROR_RUN_DELETED_FILES_FRAGMENT = "Can't run deleted files view fragment. Please try again.";
        public static final String MESSAGE_ERROR_PRESS_BACK_BUTTON = "Can't return to a previous fragment. Please try again.";

        private MessageError() {
        }
    }

    public static final class ServiceMessage {

        public static final int MSG_GET_FILES_PATH = 1;
        public static final int MSG_UPDATE_FILE_OBSERVER = 2;

        private ServiceMessage() {
        }
    }
}
