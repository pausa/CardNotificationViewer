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
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.service.notification.StatusBarNotification;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Pausa on 18/10/2015.
 *
 * this class represents a filter to apply to notifications, in order to show only specific ones
 */
public class NotificationFilter implements Parcelable, Cloneable {
    boolean showChildren;
    boolean showSummary;
    List<String> groupFilter;
    List<String> tagFilter;
    List<String> keyFilter;
    List<String> pkgFilter;
    int minPriority;

    /**
     * constructor for the default filter. Meaning tha will hide children notifications.
     */
    public NotificationFilter (){
        showChildren = false;
        showSummary = true;
        groupFilter = null;
        tagFilter = new LinkedList<>();
        keyFilter = new LinkedList<>();
        pkgFilter = new LinkedList<>();
        minPriority = Notification.PRIORITY_MIN;
    }

    @Override
    final public Object clone() {
        NotificationFilter nf;
        try {
            nf = (NotificationFilter)super.clone();
        } catch (CloneNotSupportedException e) {
            nf = new NotificationFilter();
        }
        return nf.setGroupFilter(groupFilter)
                    .setKeyFilter(keyFilter)
                    .setShowChildren(showChildren)
                    .setShowSummary(showSummary)
                    .setTagFilter(tagFilter)
                    .setPkgFilter(pkgFilter)
                    .setMinPriority(minPriority);

    }

    protected NotificationFilter(Parcel in) {
        this();
        showChildren = (boolean)in.readValue(null);
        showSummary = (boolean)in.readValue(null);
        in.readStringList(groupFilter);
        in.readStringList(tagFilter);
        in.readStringList(keyFilter);
        in.readStringList(pkgFilter);
        minPriority=in.readInt();
    }
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeValue(showChildren);
        dest.writeValue(showSummary);
        dest.writeStringList(groupFilter);
        dest.writeStringList(tagFilter);
        dest.writeStringList(keyFilter);
        dest.writeStringList(pkgFilter);
        dest.writeInt(minPriority);
    }

    public static final Creator<NotificationFilter> CREATOR = new Creator<NotificationFilter>() {
        @Override
        public NotificationFilter createFromParcel(Parcel in) {
            return new NotificationFilter(in);
        }

        @Override
        public NotificationFilter[] newArray(int size) {
            return new NotificationFilter[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
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
    public static boolean isSummary (StatusBarNotification sbn) {
        //va fatto in base alla versione
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH && ((sbn.getNotification().flags & Notification.FLAG_GROUP_SUMMARY) == Notification.FLAG_GROUP_SUMMARY);
    }

    /**
     * tries to apply this filter to the given notification
     * @param sbn the notification to check
     * @param notificationGroups  the notification group to check groups with, needed for summaries filtering
     * @param exact defines the behaviour for string filters, if set to false it will check if the filter is a substring of the notification field
     * @return true if notification matches the filter
     */
    public boolean matchFilter (StatusBarNotification sbn, NotificationGroups notificationGroups, boolean exact){
        //Checking notificaiton priority
        if (sbn.getNotification().priority < minPriority)
            return false;

        //these mean something, only if a NotificationGroups is passed
        if (notificationGroups != null) {
            if (!showSummary && isSummary(sbn))
                return false;

            //to be a child, it has to be in a group with a summary
            if (notificationGroups.getGroupSummary(sbn.getGroupKey()) != null)
                if (!showChildren && !isSummary(sbn))
                    return false;
        }

        if (!matchStringFilter(groupFilter,sbn.getGroupKey(),exact))
            return false;

        if (!matchStringFilter(tagFilter,sbn.getTag(),exact))
            return false;

        if (!matchStringFilter(keyFilter,getNotificationKey(sbn),exact))
            return false;

        //noinspection RedundantIfStatement -- disabled warning to mantain coherence with other conditions
        if (!matchStringFilter(pkgFilter,sbn.getPackageName(),exact))
            return false;

        return true;
    }

    private boolean matchStringFilter (List<String> filterList, String s, boolean exact){
        //string filter is matched if at least one item in the list matches
        for (String filter : filterList){
            if(exact){
                if (filter.equals(s))
                    return true;
            }
            else {
                if (s != null && s.contains(filter))
                    return true;
            }
        }
        //if we arrive here, the filter matches only if no filter is present
        return filterList.isEmpty();
    }

    /**
     * applies this filter to the given notification list
     * @param notificationList the list to filter
     * @param notificationGroups the group information in order to apply group filters
     * @param exact defines the behaviour for string filters, if set to false it will check if the filter is a substring of the notification field
     * @return the filtered notification list
     */
    public List<StatusBarNotification> applyFilter (Collection<StatusBarNotification> notificationList, NotificationGroups notificationGroups, boolean exact){
        ArrayList<StatusBarNotification> filteredList = new ArrayList<>();
        for (StatusBarNotification sbn : notificationList)
            if (matchFilter(sbn, notificationGroups, exact))
                filteredList.add(sbn);
        return filteredList;
    }
    //setters
    @SuppressWarnings("unused")
    public NotificationFilter setShowChildren(boolean showChildren) {
        this.showChildren = showChildren;
        return this;
    }

    @SuppressWarnings("unused")
    public NotificationFilter setShowSummary(boolean showSummary) {
        this.showSummary = showSummary;
        return this;
    }

    @SuppressWarnings("unused")
    public NotificationFilter setGroupFilter(Collection<String> filter) {
        if (filter != null) this.groupFilter = new LinkedList<>(filter);
        return this;
    }

    @SuppressWarnings("unused")
    public NotificationFilter addGroupFilter(String filter) {
        if (filter != null) this.groupFilter.add(filter);
        return this;
    }

    @SuppressWarnings("unused")
    public NotificationFilter setTagFilter(Collection<String> filter) {
        if (filter != null) this.tagFilter = new LinkedList<>(filter);
        return this;
    }

    @SuppressWarnings("unused")
    public NotificationFilter addTagFilter(String filter) {
        if (filter != null) this.tagFilter.add(filter);
        return this;
    }

    @SuppressWarnings("unused")
    public NotificationFilter setKeyFilter(Collection<String> filter) {
        if (filter != null) this.keyFilter = new LinkedList<>(filter);
        return this;
    }

    @SuppressWarnings("unused")
    public NotificationFilter addKeyFilter(String filter) {
        if (filter != null) this.keyFilter.add(filter);
        return this;
    }

    @SuppressWarnings("unused")
    public NotificationFilter setPkgFilter(Collection<String> filter) {
        if (filter != null) this.pkgFilter = new LinkedList<>(filter);
        return this;
    }

    @SuppressWarnings("unused")
    public NotificationFilter addPkgFilter(String filter) {
        if (filter != null) this.pkgFilter.add(filter);
        return this;
    }

    @SuppressWarnings("unused")
    public NotificationFilter setMinPriority (int priority) {
        this.minPriority = priority;
        return this;
    }
}
