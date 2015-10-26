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

public class MainActivity extends AppCompatActivity {
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
        LocalBroadcastManager.getInstance(this).registerReceiver(nReceiver,filter);
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
        nBuilder.setPriority(Notification.PRIORITY_MIN);

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

    private class NotificationReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Parcelable pExtra;
            pExtra = intent.getParcelableExtra(ConcreteNotificationListenerService.NOTIFICATION_EXTRA);
            if (pExtra != null && pExtra instanceof StatusBarNotification){
                //checking action
                switch (intent.getAction()) {
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

}
