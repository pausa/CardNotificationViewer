package com.android.madpausa.cardnotificationviewer;

import android.service.notification.StatusBarNotification;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import java.util.List;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends ListFragment  {
    //adapter per le notifiche
    NotificationListAdapter notificationAdapter;

    public MainActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        //caricare l'adapter
        notificationAdapter = new NotificationListAdapter(inflater.getContext());
        setListAdapter(notificationAdapter);
        super.onCreateView(inflater, container, savedInstanceState);
        //inizializzare il loader
        //return inflater.inflate(R.layout.fragment_main, container, false);
        return inflater.inflate(R.layout.fragment_main,container,false);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
    }

    public void initNotificationList (List<StatusBarNotification> list){
        notificationAdapter.setList(list);
    }

    public void addNotification (StatusBarNotification sbn){
        notificationAdapter.addNotification(sbn);
    }
    public void clearNotificationList (){
        notificationAdapter.clearList();
    }

    public void removeNotification(StatusBarNotification sbn) {
        notificationAdapter.removeNotification(sbn);
    }
}
