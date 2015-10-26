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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

public class ConcreteNotificationListenerService extends NotificationListenerService {
    private static final String TAG = ConcreteNotificationListenerService.class.getSimpleName();
    public static final String CUSTOM_BINDING = "com.android.madpausa.cardnotificationviewer.CUSTOM_BINDING";
    public static final String NOTIFICATION_RECEIVER = "com.android.madpausa.cardnotificationviewer.NOTIFICATION_RECEIVER";
    public static final String NOTIFICATION_EXTRA = "notification";
    public static final String ADD_NOTIFICATION_ACTION = NOTIFICATION_RECEIVER + ".add_notification";
    public static final String REMOVE_NOTIFICATION_ACTION = NOTIFICATION_RECEIVER + ".remove_notification";
    public static final String SERVICE_NOTIFICATION = ConcreteNotificationListenerService.class.getSimpleName() + ".NOTIFICATION";
    public static final String ARCHIVED_NOTIFICATIONS_EXTRA = "ARCHIVED_NOTIFICATIONS_EXTRA";


    private final IBinder mBinder = new LocalBinder();
    private SharedPreferences sp = null;

    LinkedHashMap<String,StatusBarNotification> notificationMap;
    LinkedHashMap<String,StatusBarNotification> archivedNotificationMap;

    public NotificationGroups getNotificationGroups() {
        return notificationGroups;
    }

    NotificationGroups notificationGroups;
    NotificationFilter baseNotificationFilter;


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
        notificationMap = new LinkedHashMap<>();
        archivedNotificationMap = new LinkedHashMap<>();
        notificationGroups = new NotificationGroups();
        baseNotificationFilter = new NotificationFilter();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        super.onNotificationPosted(sbn);
        if (!SERVICE_NOTIFICATION.equals(sbn.getTag()))
            handlePostedNotification(sbn);
    }

    public void handlePostedNotification (StatusBarNotification sbn) {
        //should be removed if already existing, in order to put it back in top position
        removeServiceNotification(sbn);
        NotificationFilter notificationFilter = (NotificationFilter)baseNotificationFilter.clone();
        //min priority notifications should be archived too
        notificationFilter.setMinPriority(Notification.PRIORITY_DEFAULT);

        //if the notification has to be shown, is putted in the primary map
        if (notificationFilter.matchFilter(sbn,notificationGroups,true))
            notificationMap.put(NotificationFilter.getNotificationKey(sbn), sbn);
        else {
            //otherwise, it goes directly in the archived ones
            archivedNotificationMap.put(NotificationFilter.getNotificationKey(sbn), sbn);
            sendServiceNotification();
        }

        //adding it to the group structure
        notificationGroups.addGroupMember(sbn);

        //if the notification are more than the threshold, archive older ones
        if (notificationMap.size() > Integer.parseInt(sp.getString(SettingsActivityFragment.NOTIFICATION_THRESHOLD, "-1")))
            archiveNotifications();

        //sending notification to binded clients
        Intent intent = new Intent(ADD_NOTIFICATION_ACTION);
        intent.putExtra(NOTIFICATION_EXTRA, sbn);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void archiveNotifications() {
        int threshold = Integer.parseInt(sp.getString(SettingsActivityFragment.NOTIFICATION_THRESHOLD, "-1"));
        if (threshold < 0)
            return;
        //TODO as there might be times in which this logic won't work properly, I should firstly archive non summary notifications in the main map.
        for (StatusBarNotification sbn : notificationMap.values()) {
            //getting the older notification
            String nKey = NotificationFilter.getNotificationKey(sbn);

            //moving the notification in the archived ones
            notificationMap.remove(nKey);
            archivedNotificationMap.put(nKey, sbn);

            //hiding the notification
            hideNotification(sbn);

            //if I went back to threshold number, then I can stop
            if (notificationMap.size() <= threshold)
                break;
        }

        //displaying the notification about remining ones
        sendServiceNotification();
    }

    //returning a map containing all the notifications, because it's linked, the order is preserved
    public LinkedHashMap<String, StatusBarNotification> getNotificationMap(){
        LinkedHashMap<String, StatusBarNotification> map = new LinkedHashMap<>(archivedNotificationMap);
        map.putAll(notificationMap);
        return map;
    }

    /**
     * removes all clearable notifications from the service
     */
    public void clearNotificationList(){

        LinkedList<StatusBarNotification> nCollection = new LinkedList<> (archivedNotificationMap.values());
        nCollection.addAll(notificationMap.values());

        for (StatusBarNotification sbn : nCollection){
            if (sbn.isClearable()){
                //remove notificaion from this service
                removeServiceNotification(sbn);

                //tells the system to cancel the notification
                cancelNotification(sbn);
            }

        }

    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        super.onNotificationRemoved(sbn);
        if(!SERVICE_NOTIFICATION.equals(sbn.getTag()))
            handleRemovedNotification(sbn);
    }

    private void handleRemovedNotification(StatusBarNotification sbn) {
        //Remove notification from service
        removeServiceNotification(sbn);

        //sending information about the removed notification
        Intent intent = new Intent(REMOVE_NOTIFICATION_ACTION);
        intent.putExtra(NOTIFICATION_EXTRA, sbn);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        Log.d(TAG, "listener connected");

        //bootstrapping the notification map
        StatusBarNotification[] nArray = this.getActiveNotifications();
        if (nArray != null){
            for (StatusBarNotification sbn : nArray)
                if(!SERVICE_NOTIFICATION.equals(sbn.getTag()))
                    handlePostedNotification(sbn);
        }

    }



    @Override
    public IBinder onBind(Intent intent) {
        sp = PreferenceManager.getDefaultSharedPreferences(this);

        //bind for non system clients, needed to prevent odd behaviour from android notification service
        if (intent.getAction().equals(CUSTOM_BINDING)){
            return mBinder;
        }
        else{
            return super.onBind(intent);
        }

    }

    /**
     * helper class, cancels given notification from system. Should be compatible with all android versions
     * @param sbn the notification to cancel
     */
    @SuppressWarnings("deprecation")
    public void cancelNotification (StatusBarNotification sbn){

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH)
            super.cancelNotification(sbn.getKey());
        else
            super.cancelNotification(sbn.getPackageName(),sbn.getTag(),sbn.getId());
    }

    /**
     * removes notification from this service only
     * @param sbn the notification to be removed from service
     */
    public void removeServiceNotification(StatusBarNotification sbn){
        String nKey = NotificationFilter.getNotificationKey(sbn);

        //removes from main notifications
        notificationMap.remove(nKey);

        //removes from archived notifications
        archivedNotificationMap.remove(nKey);

        //if archived notifications are over, I should remove service notification, otherwise update it
        if (archivedNotificationMap.size() < 1)
            removeServiceNotification();
        else sendServiceNotification();

        //it should be removed from group structure too
        notificationGroups.removeGroupMember(sbn);
    }

    /**
     * sends the service notification
     */
    private void sendServiceNotification (){

        //filtering archived notification list
        List<StatusBarNotification> filteredArchivedNotificationList = baseNotificationFilter.applyFilter(archivedNotificationMap.values(), notificationGroups, true);
        int filteredArchiveddSize = filteredArchivedNotificationList.size();

        //should show notification only if there are notifications to be shown
        if (filteredArchiveddSize > 0) {
            NotificationCompat.Builder nBuilder = new NotificationCompat.Builder(this);

            nBuilder.setContentTitle(String.format(getString(R.string.service_notification_text), filteredArchiveddSize));

            nBuilder.setSmallIcon(R.drawable.ic_notification);
            //gets the correct color resource, based on android version
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                nBuilder.setColor(getResources().getColor(R.color.app_background, null));
            else //noinspection deprecation
                nBuilder.setColor(getResources().getColor(R.color.app_background));

            NotificationManager nManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            //setting the intent
            Intent resultIntent = new Intent(this, MainActivity.class);

            //setting the extra containing the archived notifications
            resultIntent.putExtra(ARCHIVED_NOTIFICATIONS_EXTRA, new HashSet<>(archivedNotificationMap.keySet()));

            TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
            stackBuilder.addParentStack(MainActivity.class);
            stackBuilder.addNextIntent(resultIntent);

            PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
            nBuilder.setContentIntent(resultPendingIntent);

            //low priority, not min, as it has to show in the lockscreen
            nBuilder.setPriority(Notification.PRIORITY_LOW);

            Notification notification = nBuilder.build();

            //this notification should be sticky
            notification.flags |= Notification.FLAG_NO_CLEAR;
            nManager.notify(SERVICE_NOTIFICATION, 0, notification);
        }
        //else I should remove the notification
        else
            removeServiceNotification();
    }

    private void removeServiceNotification(){
        NotificationManager nManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        nManager.cancel(SERVICE_NOTIFICATION,0);
    }

    /**
     * Should hide the given notification from the statusBar. Unimplemented for now. Xposed is handling it at the moment
     * @param sbn the notification to lower
     */
    @SuppressWarnings("UnusedParameters")
    public void hideNotification (StatusBarNotification sbn){
        //TODO find a legit way, even using root
    }


}
