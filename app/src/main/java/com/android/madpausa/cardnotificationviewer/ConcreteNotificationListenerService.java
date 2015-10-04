package com.android.madpausa.cardnotificationviewer;

import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ConcreteNotificationListenerService extends NotificationListenerService {
    private static final String TAG = ConcreteNotificationListenerService.class.getSimpleName();
    private final IBinder mBinder = new LocalBinder();
    List<StatusBarNotification> notificationList;


    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {

        ConcreteNotificationListenerService getService() {
            // Return this instance of LocalService so clients can call public methods
            return ConcreteNotificationListenerService.this;
        }
    }

    public ConcreteNotificationListenerService() {
        super();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Creo il servizio");
        notificationList = new ArrayList<StatusBarNotification>();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        Log.d(TAG, "arrivata notifica " + sbn.toString());
        super.onNotificationPosted(sbn);
        handlePostedNotification(sbn);
    }
    public void handlePostedNotification (StatusBarNotification sbn) {
        notificationList.add(sbn);
        //Creo l'intent per il messaggio da mandare a chi lo vuole

        Intent intent = new Intent(getString(R.string.notification_receiver));
        intent.putExtra(getString(R.string.notification_extra),sbn);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    public List<StatusBarNotification> getActiveNotificationsList(){
        return notificationList;
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        super.onNotificationRemoved(sbn);
    }

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        Log.d(TAG, "listener connesso!");
        StatusBarNotification[] nArray = this.getActiveNotifications();
        if (nArray != null){
            notificationList = new ArrayList<StatusBarNotification>(Arrays.asList(nArray));
        }

    }


    @Override
    public IBinder onBind(Intent intent) {
        if (intent.getAction().equals(getString(R.string.custom_binding))){
            Log.d(TAG, "custom binding");
            return mBinder;
        }
        else{
            Log.d(TAG,"system binding");
            return super.onBind(intent);
        }
    }
}
