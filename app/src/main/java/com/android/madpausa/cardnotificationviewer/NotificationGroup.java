package com.android.madpausa.cardnotificationviewer;

import android.app.Notification;
import android.os.Build;
import android.service.notification.StatusBarNotification;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by antpetre on 16/10/2015.
 */
public class NotificationGroup {
    Map<String,Set<String>> groups;
    Map<String,String> summaries;

    public NotificationGroup(){
        groups = new HashMap<>();
        summaries = new HashMap<>();
    }

    /***
     * creates a notification group structure from given notifications
     * @param notifications
     */
    public NotificationGroup(Collection<StatusBarNotification> notifications){
        this();
        for (StatusBarNotification sbn : notifications)
            addGroupMember(sbn);
    }

    /**
     * gets the members of the passed group
     * @param group should be the return value of StatusBarNotification.getGroupKey()
     * @return a string set of notification keys
     */
    public Set<String> getGroupMembers(String group){
        if (group == null)
            return null;
        return groups.get(group);
    }

    /**
     * returns the notification key of the group summary
     * @param group should be the return value of StatusBarNotification.getGroupKey()
     * @return a notification key
     */
    public String getGroupSummary (String group){
        if (group == null)
            return null;
        return summaries.get(group);
    }


    /**
     * adds the notification to the corrisponding group
     * @param sbn
     */
    public void addGroupMember (StatusBarNotification sbn){
        //if it has no groupKey, then it's part of no group, so nothing to do
        if (sbn.getGroupKey() == null)
            return;

        String nKey = NotificationFilter.getNotificationKey(sbn);
        Set<String> group = groups.get(sbn.getGroupKey());

        //should the group not exists, it has to be creatd
        if (group == null){
            group = new HashSet<>();
        }

        group.add(nKey);
        groups.put(sbn.getGroupKey(),group);

        //if the notification is the summary of this group, then it has to be added to the summaries
        if (NotificationFilter.isSummary(sbn))
            summaries.put(sbn.getGroupKey(),nKey);
    }

    public int groupSize (String group){
        if (group == null)
            return 0;

        Set<String> groupSet = groups.get(group);
        if (groupSet == null)
            return 0;
        else return groupSet.size();
    }

    /**
     * removes the current notification from the groups
     * @param sbn
     */
    public void removeGroupMember (StatusBarNotification sbn){
        //if it has no groupKey, then it's part of no group, so nothing to do
        if (sbn.getGroupKey() == null)
            return;

        Set<String> group = groups.get(sbn.getGroupKey());

        //should the group not exists, nothing to do
        if (group == null){
            return;
        }

        //Removes the notification from the group
        String nKey = NotificationFilter.getNotificationKey(sbn);
        group.remove(nKey);

        //if the group is now empty, it should be removed
        if (group.isEmpty()) {
            groups.remove(sbn.getGroupKey());
            summaries.remove(sbn.getGroupKey());
        }

        //if the notification was the summary of this group, then it has to be removed from the summaries
        if (NotificationFilter.isSummary(sbn))
            summaries.remove(sbn.getGroupKey());
    }
}
