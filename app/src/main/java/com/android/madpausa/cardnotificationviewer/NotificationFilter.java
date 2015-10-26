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
import java.util.List;

/**
 * Created by Pausa on 18/10/2015.
 *
 * this class represents a filter to apply to notifications, in order to show only specific ones
 */
public class NotificationFilter implements Parcelable, Cloneable {
    boolean showChildern;
    boolean showSummary;
    String groupFilter;
    String tagFilter;
    String keyFilter;
    String pkgFilter;
    int minPriority;

    /**
     * constructor for the default filter. Meaning tha will hide children notifications.
     */
    public NotificationFilter (){
        showChildern = false;
        showSummary = true;
        groupFilter = null;
        tagFilter = null;
        keyFilter = null;
        pkgFilter = null;
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
                    .setShowChildern(showChildern)
                    .setShowSummary(showSummary)
                    .setTagFilter(tagFilter)
                    .setPkgFilter(pkgFilter)
                    .setMinPriority(minPriority);

    }

    protected NotificationFilter(Parcel in) {
        this();
        boolean[] booleanFilters = in.createBooleanArray();
        String[] stringFilters = in.createStringArray();
        int[] intFilters = in.createIntArray();
        showChildern = booleanFilters[0];
        showSummary = booleanFilters[1];
        groupFilter = stringFilters[0];
        tagFilter = stringFilters[1];
        keyFilter = stringFilters[2];
        pkgFilter = stringFilters[3];
        minPriority=intFilters[0];
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

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        boolean[] booleanFilters = new boolean[] {showChildern, showSummary};
        String[] stringFilters = new String[] {groupFilter, tagFilter, keyFilter, pkgFilter};
        int[] intFilters = new int[] {minPriority};
        dest.writeBooleanArray(booleanFilters);
        dest.writeStringArray(stringFilters);
        dest.writeIntArray(intFilters);
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
                if (!showChildern && !isSummary(sbn))
                    return false;
        }
        if (exact)
            return matchExactFilter(sbn);
        else
            return matchFilter(sbn);
    }

    private boolean matchExactFilter(StatusBarNotification sbn) {
        if (groupFilter != null)
            if(!groupFilter.equals(sbn.getGroupKey()))
                return false;

        if (tagFilter != null)
            if(!tagFilter.equals(sbn.getTag()))
                return false;

        if (keyFilter != null)
            if(!keyFilter.equals(getNotificationKey(sbn)))
                return false;

        if (pkgFilter != null)
            if(!pkgFilter.equals(sbn.getPackageName()))
                return false;

        return true;
    }

    private boolean matchFilter(StatusBarNotification sbn) {
        if (groupFilter != null)
            if(sbn.getGroupKey() == null || !sbn.getGroupKey().contains(groupFilter))
                return false;

        if (tagFilter != null)
            if(sbn.getTag() == null || !sbn.getTag().contains(tagFilter))
                return false;

        if (keyFilter != null)
            if(getNotificationKey(sbn) == null || !getNotificationKey(sbn).contains(keyFilter))
                return false;

        if (pkgFilter != null)
            if(sbn.getPackageName() == null || !sbn.getPackageName().contains(pkgFilter))
                return false;

        return true;
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
    public NotificationFilter setShowChildern(boolean showChildern) {
        this.showChildern = showChildern;
        return this;
    }

    @SuppressWarnings("unused")
    public NotificationFilter setShowSummary(boolean showSummary) {
        this.showSummary = showSummary;
        return this;
    }

    @SuppressWarnings("unused")
    public NotificationFilter setGroupFilter(String groupFilter) {
        this.groupFilter = groupFilter;
        return this;
    }

    @SuppressWarnings("unused")
    public NotificationFilter setTagFilter(String tagFilter) {
        this.tagFilter = tagFilter;
        return this;
    }

    @SuppressWarnings("unused")
    public NotificationFilter setKeyFilter(String keyFilter) {
        this.keyFilter = keyFilter;
        return this;
    }

    @SuppressWarnings("unused")
    public NotificationFilter setPkgFilter(String pkgFilter) {
        this.pkgFilter = pkgFilter;
        return this;
    }

    @SuppressWarnings("unused")
    public NotificationFilter setMinPriority (int priority) {
        this.minPriority = priority;
        return this;
    }
}
