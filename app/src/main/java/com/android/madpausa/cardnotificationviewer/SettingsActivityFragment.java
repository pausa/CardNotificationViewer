package com.android.madpausa.cardnotificationviewer;

import android.app.Fragment;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * A placeholder fragment containing a simple view.
 */
public class SettingsActivityFragment extends PreferenceFragment {
    final static String TEST_MODE = "pref_test_mode";
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preference_list);
    }


}
