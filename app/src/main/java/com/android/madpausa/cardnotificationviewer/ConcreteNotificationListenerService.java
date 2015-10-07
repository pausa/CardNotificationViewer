package com.android.madpausa.cardnotificationviewer;

import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ConcreteNotificationListenerService extends NotificationListenerService {
    private static final String TAG = ConcreteNotificationListenerService.class.getSimpleName();
    public static final String CUSTOM_BINDING = "com.android.madpausa.cardnotificationviewer.CUSTOM_BINDING";
    public static final String NOTIFICATION_RECEIVER = "com.android.madpausa.cardnotificationviewer.NOTIFICATION_RECEIVER";
    public static final String NOTIFICATION_EXTRA = "notification";
    public static final String ADD_NOTIFICATION_ACTION = NOTIFICATION_RECEIVER + ".add_notification";
    public static final String REMOVE_NOTIFICATION_ACTION = NOTIFICATION_RECEIVER + ".remove_notification";


    private final IBinder mBinder = new LocalBinder();
    Map<String,StatusBarNotification> notificationMap;


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
        notificationMap = new LinkedHashMap<String,StatusBarNotification>();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        Log.d(TAG, "arrivata notifica " + getNotificationKey(sbn));
        super.onNotificationPosted(sbn);
        handlePostedNotification(sbn);
    }

    public void handlePostedNotification (StatusBarNotification sbn) {
        //la notifica deve risalire in cima alla pila
        notificationMap.remove(getNotificationKey(sbn));
        notificationMap.put(getNotificationKey(sbn), sbn);

        //Creo l'intent per il messaggio da mandare a chi lo vuole
        Intent intent = new Intent(ADD_NOTIFICATION_ACTION);
        intent.putExtra(NOTIFICATION_EXTRA, sbn);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    public List<StatusBarNotification> getActiveNotificationsList(){
        return new ArrayList<StatusBarNotification>(notificationMap.values());
    }

    public Map<String, StatusBarNotification> getNotificationMap(){
        return notificationMap;
    }

    public void clearNotificationList(){
        //rimuovo le notifiche dalla lista solo se clearable
        for (StatusBarNotification sbn : notificationMap.values()){
            if (sbn.isClearable())
                cancelNotification(sbn);
        }

    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        Log.d(TAG, "rimossa notifica " + getNotificationKey(sbn));
        super.onNotificationRemoved(sbn);
        handleRemovedNotification(sbn);
    }

    private void handleRemovedNotification(StatusBarNotification sbn) {
        notificationMap.remove(getNotificationKey(sbn));

        //Creo l'intent per il messaggio da mandare a chi lo vuole
        Intent intent = new Intent(REMOVE_NOTIFICATION_ACTION);
        intent.putExtra(NOTIFICATION_EXTRA, sbn);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        Log.d(TAG, "listener connesso!");
        StatusBarNotification[] nArray = this.getActiveNotifications();
        if (nArray != null){
            for (StatusBarNotification sbn : nArray)
                notificationMap.put(getNotificationKey(sbn), sbn);
        }

    }

    /**
     * Gets a key to use in notification map. Made to be compatible with older android versions
     * @param sbn a notification tu use in order to generate the key
     * @return a notification key
     */
    public static String getNotificationKey(StatusBarNotification sbn) {
        //va fatto in base alla versione
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH)
            return sbn.getKey();
        else
            return sbn.getPackageName() + sbn.getTag() + sbn.getId();
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (intent.getAction().equals(CUSTOM_BINDING)){
            Log.d(TAG, "custom binding");
            return mBinder;
        }
        else{
            Log.d(TAG,"system binding");
            return super.onBind(intent);
        }

    }

    public void cancelNotification (StatusBarNotification sbn){
        //per mantenere la retrocompatibilità
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH)
            super.cancelNotification(sbn.getKey());
        else
            super.cancelNotification(sbn.getPackageName(),sbn.getTag(),sbn.getId());
    }

}
