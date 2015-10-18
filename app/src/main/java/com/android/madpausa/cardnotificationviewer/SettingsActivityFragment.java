package com.android.madpausa.cardnotificationviewer;

import android.os.Bundle;
import android.preference.PreferenceFragment;

/**
 * A placeholder fragment containing a simple view.
 */
public class SettingsActivityFragment extends PreferenceFragment {
    final static String TEST_MODE = "pref_test_mode";
    final static String NOTIFICATION_THRESHOLD = "pref_notification_threshold";
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preference_list);
    }


}
