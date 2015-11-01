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

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build;
import android.preference.PreferenceManager;
import android.service.notification.StatusBarNotification;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RemoteViews;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by ANTPETRE on 02/10/2015.
 *
 * this implements the adapter to show the notification. The Aim is to make it as generic as possible and to provide android-like notification views.
 */
public class NotificationListAdapter  extends RecyclerView.Adapter<CardElementHolder> {
    @SuppressWarnings("unused")
    private static final String TAG = NotificationListAdapter.class.getSimpleName();
    private static final String DARK_BACKGROUND_PREF = "DARK_BACKGROUND_PREF";
    private static final String LIGHT_BACKGROUND_PREF = "LIGHT_BACKGROUND_PREF";

    List <StatusBarNotification> nList;
    Set<String> nSet;
    ConcreteNotificationListenerService nService;

    //the notification filter for this adapter
    NotificationFilter nFilter;
    NotificationFilter forceDarkBackground;
    NotificationFilter forceLightBackground;

    Context context;
    public NotificationListAdapter(Context c) {
        super();
        context = c;
        nList = new ArrayList<>();
        nSet = new HashSet<>();
        nFilter = new NotificationFilter();
        forceDarkBackground = loadNotificationFilter(DARK_BACKGROUND_PREF);
        forceLightBackground = loadNotificationFilter(LIGHT_BACKGROUND_PREF);
    }

    public void setNotificationService(ConcreteNotificationListenerService nService) {
        this.nService = nService;
        changeDataSet();
    }

    @SuppressWarnings("unused")
    public void setNotificationFilter (NotificationFilter filter){
        nFilter = filter;
    }

    /**
     * adds the notification to the list, handles updates too
     * @param sbn the notification to be added
     */
    public void addNotification (StatusBarNotification sbn){
        //adding the notification to the top of the list
        nList.add(0,sbn);
        //if it is added to the set, notify the adapter
        if (nSet.add(NotificationFilter.getNotificationKey(sbn))){
            notifyItemInserted(0);
        }
        else { //if not, should find where the old one was, remove it and update the adapter
            int pos = findNotificationPosition(sbn);
            nList.remove(pos);

            //Since it has been added to top, if old position was different, notify the move before the change
            if ((pos - 1) != 0){
                notifyItemMoved(pos-1, 0);
            }
            notifyItemChanged(0);
        }

    }

    /**
     * notifies the update action, no change of position assumed
     * @param sbn the notification to update
     */
    public void updateNotification (StatusBarNotification sbn){
        int pos = findNotificationPosition(sbn);
        if (pos >= 0){
            notifyItemChanged(pos);
        }
    }

    /**
     * removes notification from the list
     * @param sbn the notificatio nto be removed
     */
    public void removeNotification(StatusBarNotification sbn){
        //removing from the set, if not present, nothing to do
        if(nSet.remove(NotificationFilter.getNotificationKey(sbn))){
            int pos = findNotificationPosition(sbn);
            nList.remove(pos);
            notifyItemRemoved(pos);
        }
    }

    @Override
    public CardElementHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.notification_list_element, parent, false);
        return new CardElementHolder(view, parent,nService);
    }

    @Override
    public void onBindViewHolder(CardElementHolder holder, int position) {

        StatusBarNotification sbn = nList.get(position);
        RemoteViews nRemote = sbn.getNotification().bigContentView;
        if (nRemote == null)
            nRemote = sbn.getNotification().contentView;

        //removing loeaded views
        holder.getCardView().removeAllViews();

        //setting notification to holder
        holder.setSbn(sbn);
        //adding new view
        View notificationView = nRemote.apply(context, holder.parent);

        holder.setNotificationView(notificationView);

        //setting dark background, when needed
        if (isDarkBackground(holder))
            holder.getCardView().setBackgroundColor(getColor(R.color.cardview_dark_background));
        else holder.getCardView().setBackgroundColor(getColor(R.color.cardview_light_background));

    }

    @Override
    public int getItemCount() {
        return nList.size();
    }

    /**
     * this will update the whole list, meant for reload it, should not be used on punctual updates
     */
    public void changeDataSet() {
        //updating list first, like a stack
        //TODO handle groups using dedicated view

        nList = new ArrayList<>();
        nSet = new HashSet<>();

        if (nService != null){
            Collection<StatusBarNotification> notifications = nService.getNotificationMap().values();
            NotificationGroups nGroup = nService.getNotificationGroups();
            for (StatusBarNotification sbn : notifications){
                //if matches filter, adding it to the list and to the set
                if (nFilter.matchFilter(sbn,nGroup,true)) {
                    nList.add(0, sbn);
                    nSet.add(NotificationFilter.getNotificationKey(sbn));
                }
            }
        }
        notifyDataSetChanged();

    }

    private boolean isDarkBackground(CardElementHolder holder){
        StatusBarNotification sbn = holder.getSbn();
        //eventual overridings should go in either Dark or Light Filter
        if (forceDarkBackground.matchFilter(sbn,null,true)) return true;
        if (forceLightBackground.matchFilter(sbn,null,true)) return false;

        //base color, to force a light background (in line with default notification theme)
        int textColor = Color.BLACK;

        //getting notification view resources
        Resources nViewResources = holder.getNotificationView().getResources();

        if (nViewResources != null){
            //trying to understand background color from title text
            int titleViewId = nViewResources.getIdentifier("android:id/title", null, null);
            if (titleViewId != 0){
                TextView titleView = (TextView)holder.getNotificationView().findViewById(titleViewId);
                if (titleView != null)
                    textColor=titleView.getTextColors().getDefaultColor();
            }
        }

        //if text is dark, use light background and vice versa
        return !isColorDark(textColor);
    }

    /**
     * thanks stack overflow! checks if a color is dark
     * @param color the actual color
     * @return true if dark
     */
    private boolean isColorDark(int color){
        double darkness = 1-(0.299*Color.red(color) + 0.587*Color.green(color) + 0.114* Color.blue(color))/255;
        return darkness >= 0.5;
    }


    private int getColor (int id){
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            //noinspection deprecation
            return context.getResources().getColor(id);
        else
            return context.getResources().getColor(id,null);
    }

    /**
     * finds the last notification in the adapter list that matches input
     * @param sbn the notification to find
     * @return the position, or -1 if not found
     */
    private int findNotificationPosition (StatusBarNotification sbn){
        String nKey = (NotificationFilter.getNotificationKey(sbn));
        int listSize = nList.size();

        for (int pos = listSize - 1; pos >= 0; pos--){
            if (nKey.equals(NotificationFilter.getNotificationKey(nList.get(pos))))
                return pos;
        }

        return -1;
    }

    private NotificationFilter loadNotificationFilter (String filterID){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        Set<String> set = sp.getStringSet(filterID, new HashSet<String>());
        return new NotificationFilter().setPkgFilter(set).addPkgFilter("");
    }

    public void addToDarkBackground (StatusBarNotification sbn){
        forceDarkBackground.addPkgFilter(sbn.getPackageName());
        updateNotification(sbn);
        persistFilterAdd(DARK_BACKGROUND_PREF,sbn.getPackageName());
    }
    public void addToLightBackground (StatusBarNotification sbn){
        forceLightBackground.addPkgFilter(sbn.getPackageName());
        updateNotification(sbn);
        persistFilterAdd(LIGHT_BACKGROUND_PREF,sbn.getPackageName());
    }

    private void persistFilterAdd(String filterID, String packageName) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        Set<String> set = sp.getStringSet(filterID, null);
        if (set == null){
            set = new HashSet<>();
        }
        set.add(packageName);

        SharedPreferences.Editor editor = sp.edit();
        editor.putStringSet(filterID, set);

        editor.apply();
    }

    public void restoreDefaultBackground(StatusBarNotification sbn){
        String filterID = null;
        NotificationFilter filter = null;
        if (forceDarkBackground.matchFilter(sbn,null,true)){
            filterID = DARK_BACKGROUND_PREF;
            filter = forceDarkBackground;
        }
        else if (forceLightBackground.matchFilter(sbn,null,true)){
            filterID = LIGHT_BACKGROUND_PREF;
            filter = forceLightBackground;
        }
        if(filterID!=null){
            filter.removePkgFilter(sbn.getPackageName());
            updateNotification(sbn);
            persistFilterRemove(filterID, sbn.getPackageName());
        }
    }

    private void persistFilterRemove(String filterID, String packageName) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        Set<String> set = sp.getStringSet(filterID, null);
        if (set == null){
            return;
        }
        set.remove(packageName);

        SharedPreferences.Editor editor = sp.edit();

        if (set.isEmpty())
            editor.remove(filterID);
        else
            editor.putStringSet(filterID, set);

        editor.apply();
    }

}
