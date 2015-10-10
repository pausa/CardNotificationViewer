package com.android.madpausa.cardnotificationviewer;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

public class ConcreteNotificationListenerService extends NotificationListenerService {
    private static final String TAG = ConcreteNotificationListenerService.class.getSimpleName();
    public static final String CUSTOM_BINDING = "com.android.madpausa.cardnotificationviewer.CUSTOM_BINDING";
    public static final String NOTIFICATION_RECEIVER = "com.android.madpausa.cardnotificationviewer.NOTIFICATION_RECEIVER";
    public static final String NOTIFICATION_EXTRA = "notification";
    public static final String ADD_NOTIFICATION_ACTION = NOTIFICATION_RECEIVER + ".add_notification";
    public static final String REMOVE_NOTIFICATION_ACTION = NOTIFICATION_RECEIVER + ".remove_notification";
    private static final String SERVICE_NOTIFICATION = ConcreteNotificationListenerService.class.getSimpleName() + ".NOTIFICATION";


    private final IBinder mBinder = new LocalBinder();
    private SharedPreferences sp = null;

    LinkedHashMap<String,StatusBarNotification> notificationMap;
    LinkedHashMap<String,StatusBarNotification> archivedNotificationMap;

    Set<String> notificationsToArchive;
    


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
        archivedNotificationMap = new LinkedHashMap<String, StatusBarNotification>();
        notificationsToArchive = new HashSet<String>();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        Log.d(TAG, "arrivata notifica " + getNotificationKey(sbn));
        super.onNotificationPosted(sbn);
        if (!SERVICE_NOTIFICATION.equals(sbn.getTag()))
            handlePostedNotification(sbn);
    }

    public void handlePostedNotification (StatusBarNotification sbn) {
        //la notifica deve risalire in cima alla pila
        removeServiceNotification(sbn);
        notificationMap.put(getNotificationKey(sbn), sbn);

        //in base al parametro configurato, gestisco l'extra
        if (notificationMap.size() > Integer.parseInt(sp.getString(SettingsActivityFragment.NOTIFICATION_THRESHOLD, "-1")))
            archiveNotifications();

        //Creo l'intent per il messaggio da mandare a chi lo vuole
        Intent intent = new Intent(ADD_NOTIFICATION_ACTION);
        intent.putExtra(NOTIFICATION_EXTRA, sbn);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void archiveNotifications() {
        int threshold = Integer.parseInt(sp.getString(SettingsActivityFragment.NOTIFICATION_THRESHOLD, "-1"));
        if (threshold < 0)
            return;
        //TODO fare in modo che vengano considerate solo le notifiche clearable

        Iterator<StatusBarNotification> iterator = notificationMap.values().iterator();
        while (iterator.hasNext()){
            //ottengo la notifica più vecchia sulla mappa
            StatusBarNotification sbn = iterator.next();
            String nKey = getNotificationKey(sbn);

            //rimuovo solo se clearable
            if(sbn.isClearable()){
                Log.d(TAG, "archivio notifica: " + nKey);

                //la rimuovo dalla mappa principale e la aggiungo a quella delle archiviate
                notificationMap.remove(nKey);
                archivedNotificationMap.put(nKey, sbn);

                //aggiungo a quelle da archiviare
                notificationsToArchive.add(nKey);
                cancelNotification(sbn);

                sendServiceNotification();
            }
            //se il numero di notifiche è inferiore al threshold, ho finito
            if (notificationMap.size() <= threshold)
                break;
        }
    }

    public Map<String, StatusBarNotification> getNotificationMap(){
        LinkedHashMap<String, StatusBarNotification> map = new LinkedHashMap(archivedNotificationMap);
        map.putAll(notificationMap);
        return map;
    }

    public void clearNotificationList(){
        //rimuovo le notifiche dalla lista solo se clearable
        Collection <StatusBarNotification> nCollection = new LinkedList<StatusBarNotification> (archivedNotificationMap.values());
        nCollection.addAll(notificationMap.values());
        for (StatusBarNotification sbn : nCollection){
            if (sbn.isClearable()){
                //rimuovo la notifica anche direttamente, il sistema non manda la callback in caso di notifiche già rimosse
                removeServiceNotification(sbn);
                cancelNotification(sbn);
            }

        }

    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        Log.d(TAG, "rimossa notifica " + getNotificationKey(sbn));
        super.onNotificationRemoved(sbn);
        if(!SERVICE_NOTIFICATION.equals(sbn.getTag()))
            handleRemovedNotification(sbn);
    }

    private void handleRemovedNotification(StatusBarNotification sbn) {
        removeServiceNotification(sbn);

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
                handlePostedNotification(sbn);
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
            sp = PreferenceManager.getDefaultSharedPreferences(this);
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
    
    public void removeServiceNotification(StatusBarNotification sbn){
        String nKey = getNotificationKey(sbn);
        notificationMap.remove(nKey);

        //se la notifica non è presente nel set da archiviare, allora va rimossa anche dalle archiviate
        if (!notificationsToArchive.remove(nKey))
            archivedNotificationMap.remove(nKey);

        //rimuovo la notifica, nel caso le archiviate finiscano
        if (archivedNotificationMap.size() < 1)
            removeServiceNotification();
    }

    private void sendServiceNotification (){
        NotificationCompat.Builder nBuilder = new NotificationCompat.Builder(this);
        //TODO aggiungere il numero di notifiche disponibili
        nBuilder.setContentTitle("... more Available");
        nBuilder.setSmallIcon(R.drawable.ic_notification);
        NotificationManager nManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);

        //aggiungo l'intent per aprire l'app
        Intent resultIntent = new Intent (this, MainActivity.class);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0,PendingIntent.FLAG_UPDATE_CURRENT);
        nBuilder.setContentIntent(resultPendingIntent);

        Notification notification = nBuilder.build();
        notification.flags |= Notification.FLAG_NO_CLEAR;
        nManager.notify(SERVICE_NOTIFICATION, 0, notification);

    }

    private void removeServiceNotification(){
        Log.d(TAG, "Rimuovo notifica del service");
        NotificationManager nManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        nManager.cancel(SERVICE_NOTIFICATION,0);
    }

}
