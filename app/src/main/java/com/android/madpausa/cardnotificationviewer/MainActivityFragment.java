package com.android.madpausa.cardnotificationviewer;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.wifi.WifiConfiguration;
import android.os.IBinder;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;

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

        //inizializzare il loader
        //return inflater.inflate(R.layout.fragment_main, container, false);
        return super.onCreateView(inflater,container,savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        //TODO: aggiornare lista notifiche
    }

    public void initNotificationList (List<StatusBarNotification> list){
        notificationAdapter.setList(list);
    }

    public void addNotification (StatusBarNotification sbn){
        notificationAdapter.addNotification(sbn);
    }

}
