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

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import static java.lang.System.currentTimeMillis;

public class MainActivity extends AppCompatActivity implements NotificationDialogFragment.NotificationDialogListener {
    private static final String TAG = MainActivity.class.getSimpleName();

    ConcreteNotificationListenerService nService;
    NotificationReceiver nReceiver;
    MainActivityFragment nFragment;
    boolean nBound = false;

    public void startNotificationServiceActivity(View view) {
        Intent intent = new Intent ("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
        startActivity(intent);
    }

    public void removeNotification(StatusBarNotification sbn){
        nFragment.removeNotification(sbn);
    }
    public void addNotification(StatusBarNotification sbn){
        nFragment.addNotification(sbn);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);

        if (sp.getBoolean(SettingsActivityFragment.TEST_MODE,false))
            setContentView(R.layout.activity_main_test);
        else setContentView(R.layout.activity_main);

        //binding the service
        Intent intent = new Intent (this, ConcreteNotificationListenerService.class);
        intent.setAction(ConcreteNotificationListenerService.CUSTOM_BINDING);
        bindService(intent, nConnection, Context.BIND_AUTO_CREATE);
        nReceiver = new NotificationReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ConcreteNotificationListenerService.ADD_NOTIFICATION_ACTION);
        filter.addAction(ConcreteNotificationListenerService.REMOVE_NOTIFICATION_ACTION);
        filter.addAction(NotificationDialogFragment.NOTIFICATION_DIALOG_INTENT);
        LocalBroadcastManager.getInstance(this).registerReceiver(nReceiver, filter);
        nFragment = (MainActivityFragment)getSupportFragmentManager().findFragmentById(R.id.fragment);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(nConnection);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(nReceiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent (this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    public void sendTestNotification (View view){
        NotificationCompat.Builder nBuilder = new NotificationCompat.Builder(this);
        nBuilder.setContentTitle("Test Notification!");
        nBuilder.setContentText("This is just a test");
        nBuilder.setSmallIcon(R.drawable.ic_notification);

        Notification notification = nBuilder.build();


        NotificationManager nManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        Long millis = currentTimeMillis();
        nManager.notify(millis.intValue(), notification);
    }

    private ServiceConnection nConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service){
            ConcreteNotificationListenerService.LocalBinder binder = (ConcreteNotificationListenerService.LocalBinder) service;
            nService = binder.getService();
            nBound = true;
            nFragment.initNotificationList(nService);
        }
        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            nService = null;
            nBound = false;
        }
    };

    public void clearAllNotifications(View view) {
        if (nBound)
            nService.clearNotificationList();
    }

    @Override
    public void setColorMode(StatusBarNotification sbn, int mode) {
        switch (mode){
            case NotificationDialogFragment.FORCE_DARK:
                nFragment.notificationAdapter.addToDarkBackground(sbn);
                break;
            case NotificationDialogFragment.FORCE_LIGHT:
                nFragment.notificationAdapter.addToLightBackground(sbn);
                break;
            default:
                nFragment.notificationAdapter.restoreDefaultBackground(sbn);
        }
    }

    @Override
    public void setArchiveMode(StatusBarNotification sbn, int mode) {
        switch (mode){
            case NotificationDialogFragment.FORCE_ARCHIVE:
                nService.addToAlwaysArchived(sbn);
                break;
            default:
                nService.removeFromAlwaysArchived(sbn);
                break;
        }

    }

    private class NotificationReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Parcelable pExtra;
            pExtra = intent.getParcelableExtra(ConcreteNotificationListenerService.NOTIFICATION_EXTRA);
            if (pExtra != null && pExtra instanceof StatusBarNotification){
                //checking action
                switch (intent.getAction()) {
                    case NotificationDialogFragment.NOTIFICATION_DIALOG_INTENT:
                        openNotificationDialog ((StatusBarNotification) pExtra);
                        break;
                    case ConcreteNotificationListenerService.REMOVE_NOTIFICATION_ACTION:
                        removeNotification((StatusBarNotification) pExtra);
                        break;
                    default:
                        addNotification((StatusBarNotification) pExtra);
                        break;
                }
            }
            else {
                Log.d (TAG, "received invalid message");
            }
        }
    }

    private void openNotificationDialog(StatusBarNotification sbn) {
        Log.d(TAG, "opening dialog!");
        Bundle bun = new Bundle();
        bun.putParcelable(NotificationDialogFragment.NOTIFICATION_BUNDLE, sbn);

        int colorMode = NotificationDialogFragment.DEFAULT_COLOR;
        if (nFragment.notificationAdapter.forceDarkBackground.matchFilter(sbn,null,true)){
            colorMode = NotificationDialogFragment.FORCE_DARK;
        }
        else if (nFragment.notificationAdapter.forceLightBackground.matchFilter(sbn,null,true)){
            colorMode = NotificationDialogFragment.FORCE_LIGHT;
        }
        bun.putInt(NotificationDialogFragment.COLOR_BUNDLE,colorMode);

        int archiveMode = NotificationDialogFragment.DEFAULT_ARCHIVE;
        if (nService.isAlwaysArchived(sbn))
            archiveMode = NotificationDialogFragment.FORCE_ARCHIVE;
        bun.putInt(NotificationDialogFragment.ARCHIVE_BUNDLE,archiveMode);

        NotificationDialogFragment dialog = new NotificationDialogFragment();
        dialog.setArguments(bun);
        dialog.show(getFragmentManager(),NotificationDialogFragment.class.getSimpleName());
    }

}
