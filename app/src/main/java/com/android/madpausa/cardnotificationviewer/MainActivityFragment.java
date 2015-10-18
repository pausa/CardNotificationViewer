package com.android.madpausa.cardnotificationviewer;

import android.service.notification.StatusBarNotification;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment {
    //notification list adapter
    NotificationListAdapter notificationAdapter;

    RecyclerView nRecyclerView;
    RecyclerView.LayoutManager nLayoutManager;

    public MainActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //TODO implement extra to filter this view
        //creating the adapter
        notificationAdapter = new NotificationListAdapter(inflater.getContext());

        nRecyclerView = (RecyclerView) inflater.inflate (R.layout.fragment_main,container,false);
        nRecyclerView.setHasFixedSize(false);

        nLayoutManager = new LinearLayoutManager(inflater.getContext());
        nRecyclerView.setLayoutManager(nLayoutManager);

        nRecyclerView.setAdapter(notificationAdapter);

        super.onCreateView(inflater, container, savedInstanceState);

        //loading the loader
        return nRecyclerView;
    }

    @Override
    public void onResume() {
        super.onResume();
        notificationAdapter.changeDataSet();
    }

    public void initNotificationList (ConcreteNotificationListenerService nService){
        notificationAdapter.setNotificationService(nService);
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
