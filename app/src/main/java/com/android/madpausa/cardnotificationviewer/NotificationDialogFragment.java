/**
 *  Copyright 2015 Antonio Petrella
 *
 *  This file is part of Card Notification Viewer
 *
 *   Card Notification Viewer is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Card Notification Viewer is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Card Notification Viewer.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.android.madpausa.cardnotificationviewer;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;

import java.util.ArrayList;

/**
 * Created by Pausa on 01/11/2015.
 */
public class NotificationDialogFragment extends DialogFragment {

    public static final int FORCE_LIGHT = -1;
    public static final int FORCE_DARK = -2;
    public static final int DEFAULT_COLOR = -3;
    public static final int FORCE_ARCHIVE = -4;
    public static final int DEFAULT_ARCHIVE = -5;

    public static final String COLOR_BUNDLE = "COLOR_MODE";
    public static final String ARCHIVE_BUNDLE = "ARCHIVE_MODE";
    public static final String NOTIFICATION_BUNDLE = "NOTIFICATION";
    public static final String NOTIFICATION_DIALOG_INTENT = "com.madpausa.cardnotificationviewer.OPEN_NOTIFICATION_DIALOG";


    ArrayList<CharSequence> itemTitles;
    ArrayList<Integer> itemIds;

    StatusBarNotification sbn;
    String title;

    NotificationDialogListener listener;

    public NotificationDialogFragment() {
        super ();
        itemTitles = new ArrayList<>();
        itemIds = new ArrayList<>();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        listener = (NotificationDialogListener) activity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int colorMode = getArguments().getInt(COLOR_BUNDLE,DEFAULT_COLOR);
        int archiveMode = getArguments().getInt(ARCHIVE_BUNDLE,DEFAULT_ARCHIVE);
        sbn = getArguments().getParcelable(NOTIFICATION_BUNDLE);
        String pkgName = sbn.getPackageName();

        if (pkgName == null){
            title = "";
        }
        else{
            PackageManager pm = getActivity().getApplicationContext().getPackageManager();
            try {
                ApplicationInfo ai = pm.getApplicationInfo(pkgName, 0);
                title = (String)pm.getApplicationLabel(ai);
            } catch (PackageManager.NameNotFoundException e) {
                title = "";
            }
        }

        switch (colorMode){
            case FORCE_LIGHT:
            case FORCE_DARK:
                itemTitles.add(getString(R.string.default_background_dialog));
                itemIds.add(DEFAULT_COLOR);
                break;
            default:
                itemTitles.add(getString(R.string.force_dark_background_dialog));
                itemTitles.add(getString(R.string.force_light_background_dialog));
                itemIds.add(FORCE_DARK);
                itemIds.add(FORCE_LIGHT);
                break;
        }
        switch (archiveMode){
            case FORCE_ARCHIVE:
                itemTitles.add(getString(R.string.default_archive_dialog));
                itemIds.add(DEFAULT_ARCHIVE);
                break;
            default:
                itemTitles.add(getString(R.string.force_archive_dialog));
                itemIds.add(FORCE_ARCHIVE);
                break;
        }
    }

    public interface NotificationDialogListener{
        void setColorMode(StatusBarNotification sbn, int mode);
        void setArchiveMode(StatusBarNotification sbn, int mode);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(title);
        builder.setItems(itemTitles.toArray(new CharSequence[itemTitles.size()]), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (itemIds.get(which)){
                    case FORCE_LIGHT:
                        setLightBackground();
                        break;
                    case FORCE_DARK:
                        setDarkBackground();
                        break;
                    case DEFAULT_COLOR:
                        setDefaultBackground();
                        break;
                    case FORCE_ARCHIVE:
                        setAlwaysArchive();
                        break;
                    case DEFAULT_ARCHIVE:
                        setDefaultArchive();
                        break;
                }
            }
        });
        return builder.create();
    }

    private void setDefaultArchive() {
        listener.setArchiveMode(sbn, DEFAULT_ARCHIVE);
    }

    private void setAlwaysArchive() {
        listener.setArchiveMode(sbn, FORCE_ARCHIVE);
    }

    private void setDefaultBackground() {
        listener.setColorMode(sbn, DEFAULT_COLOR);
    }

    private void setDarkBackground() {
        listener.setColorMode(sbn, FORCE_DARK);
    }

    private void setLightBackground() {
        listener.setColorMode(sbn, FORCE_LIGHT);
    }
}
