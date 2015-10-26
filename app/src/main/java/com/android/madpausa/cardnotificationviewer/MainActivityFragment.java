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
        //going back to top, after inserting
        nRecyclerView.scrollToPosition(0);
    }
    public void removeNotification(StatusBarNotification sbn) {
        notificationAdapter.removeNotification(sbn);
    }


}
