package com.android.madpausa.cardnotificationviewer;

import android.app.Notification;
import android.net.wifi.p2p.nsd.WifiP2pServiceResponse;
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
    public NotificationGroup(Collection<StatusBarNotification> notifications){
        this();
        for (StatusBarNotification sbn : notifications)
            addGroupMember(sbn);
    }
    public Set<String> getGroupMembers(String group){
        if (group == null)
            return null;
        return groups.get(group);
    }
    public String getGroupSummary (String group){
        if (group == null)
            return null;
        return summaries.get(group);
    }

    public void addGroupMember (StatusBarNotification sbn){
        if (sbn.getGroupKey() == null)
            return;

        Set<String> group = groups.get(sbn.getGroupKey());
        if (group == null){
            group = new HashSet<>();
        }
        group.add(ConcreteNotificationListenerService.getNotificationKey(sbn));
        groups.put(sbn.getGroupKey(),group);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH &&
                (sbn.getNotification().flags & Notification.FLAG_GROUP_SUMMARY) == Notification.FLAG_GROUP_SUMMARY)
            summaries.put(sbn.getGroupKey(),ConcreteNotificationListenerService.getNotificationKey(sbn));
    }

    public int groupSize (String group){
        if (group == null)
            return 0;
        Set<String> groupSet = groups.get(group);
        if (groupSet == null)
            return 0;
        else return groupSet.size();
    }


}
