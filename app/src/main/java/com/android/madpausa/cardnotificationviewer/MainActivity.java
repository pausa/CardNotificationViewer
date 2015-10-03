package com.android.madpausa.cardnotificationviewer;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.wifi.WifiConfiguration;
import android.os.Binder;
import android.os.IBinder;
import android.service.notification.StatusBarNotification;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    ConcreteNotificationListenerService nService;
    boolean nBound;
    ActivityBinder aBinder = new ActivityBinder();

    public class ActivityBinder extends Binder {
        public void addNotification(StatusBarNotification sbn){
            MainActivityFragment nFragment = (MainActivityFragment)getSupportFragmentManager().findFragmentById(R.id.fragment);
            nFragment.addNotification(sbn);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //binding del service
        Intent intent = new Intent (this, ConcreteNotificationListenerService.class);

        Log.d(TAG, "Avvio il binding");
        bindService(intent, nConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(nConnection);
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

    private ServiceConnection nConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service){
            ConcreteNotificationListenerService.LocalBinder binder = (ConcreteNotificationListenerService.LocalBinder) service;
            Log.d(TAG,"connesso a servizio");
            nService = binder.getService();
            nBound = true;
            Log.d(TAG,"aggiornamento su fragment");
            MainActivityFragment nFragment = (MainActivityFragment)getSupportFragmentManager().findFragmentById(R.id.fragment);
            nFragment.initNotificationList(nService.getActiveNotificationsList());
            Log.d(TAG,"Associo il binder del client");
            nService.setClientBinder(aBinder);

        }
        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            nService = null;
            nBound = false;
        }

    };

}
