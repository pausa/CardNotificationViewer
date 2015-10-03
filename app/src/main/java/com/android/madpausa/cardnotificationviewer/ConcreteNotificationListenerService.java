package com.android.madpausa.cardnotificationviewer;

import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ConcreteNotificationListenerService extends NotificationListenerService {
    private static final String TAG = ConcreteNotificationListenerService.class.getSimpleName();
    private final IBinder mBinder = null;
    MainActivity.ActivityBinder clientBinder = null;
    List<StatusBarNotification> notificationList;
    MainActivity boundActivity;
    IBinder originalBinder;


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
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG,"Avvio il servizio! " + super.getClass().toString());
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        Log.d(TAG, "arrivata notifica " + sbn.toString());
        super.onNotificationPosted(sbn);
        handlePostedNotification(sbn);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn, RankingMap rankingMap) {
        super.onNotificationPosted(sbn, rankingMap);
        handlePostedNotification(sbn);
    }

    public void handlePostedNotification (StatusBarNotification sbn) {
        notificationList.add(sbn);
        if (clientBinder != null){
            clientBinder.addNotification(sbn);
        }
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
        originalBinder = super.onBind(intent);
        Log.d(TAG, "restituisco il binder");
        return mBinder;
    }

    public void setClientBinder (MainActivity.ActivityBinder cBinder){
        clientBinder = cBinder;
    }
}
