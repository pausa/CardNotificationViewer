package com.android.madpausa.cardnotificationviewer;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.wifi.WifiConfiguration;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcelable;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

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
        setContentView(R.layout.activity_main);

        //binding del service
        Intent intent = new Intent (this, ConcreteNotificationListenerService.class);
        intent.setAction(ConcreteNotificationListenerService.CUSTOM_BINDING);
        Log.d(TAG, "Avvio il binding");
        bindService(intent, nConnection, Context.BIND_AUTO_CREATE);

        Log.d(TAG, "registro il receiver");
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
    protected void onResume() {
        super.onResume();
        if(nBound)
            nFragment.initNotificationList(nService.getActiveNotificationsList());
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
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    public void sendTestNotification (View view){
        NotificationCompat.Builder nBuilder = new NotificationCompat.Builder(this);
        nBuilder.setContentTitle("Test Notification!");
        nBuilder.setContentText("This is just a test");
        nBuilder.setSmallIcon(R.drawable.ic_notification);

        NotificationManager nManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        nManager.notify(0, nBuilder.build());
    }

    private ServiceConnection nConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service){
            ConcreteNotificationListenerService.LocalBinder binder = (ConcreteNotificationListenerService.LocalBinder) service;
            Log.d(TAG,"connesso a servizio");
            nService = binder.getService();
            nBound = true;
            Log.d(TAG, "aggiornamento su fragment");
            nFragment.initNotificationList(nService.getActiveNotificationsList());
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
        nFragment.clearNotificationList();
    }

    private class NotificationReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d (TAG, "Ricevuto messaggio!");
            Parcelable pExtra;
            pExtra = intent.getParcelableExtra(ConcreteNotificationListenerService.NOTIFICATION_EXTRA);
            if (pExtra != null && pExtra instanceof StatusBarNotification){
                Log.d (TAG, "Ricevuta notifica!");
                //controllo l'azione
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
                Log.d (TAG, "messaggio non valido");
            }
        }
    }

}
