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

import android.app.PendingIntent;
import android.content.Intent;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import java.util.Map;
import java.util.Set;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;


/**
 * Created by antpetre on 16/10/2015.
 *
 * Xposed module to handle the notification hiding
 */
public class NotificationDataXposed implements IXposedHookLoadPackage {
    static final String packageName = "com.android.systemui";
    static final String classNotificationData = packageName + ".statusbar.NotificationData";
    static final String classBaseStatusBar = packageName + ".statusbar.BaseStatusBar";
    Set<String> archivedNotifications;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        if(!loadPackageParam.packageName.equals(packageName))
            return;
        XposedBridge.log("Inside: " + packageName);
        hookAddMethod(loadPackageParam);
        hookShouldFilterOutMethod(loadPackageParam);
        hookRemoveMethod(loadPackageParam);
        hookUpdateNotificationMethod(loadPackageParam);
    }

    private void hookShouldFilterOutMethod(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        XC_MethodHook methodHook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                StatusBarNotification sbnParam = (StatusBarNotification)param.args[0];
                if (archivedNotifications != null && archivedNotifications.contains(NotificationFilter.getNotificationKey(sbnParam))){
                        param.setResult(true);
                        //noinspection UnnecessaryReturnStatement Xposed javadoc says I should return
                        return;
                }
            }
        };
        try {
            XposedHelpers.findAndHookMethod(classNotificationData,
                    loadPackageParam.classLoader, "shouldFilterOut",
                    StatusBarNotification.class,
                    methodHook);
        } catch (Exception e) {
            XposedBridge.log("Unable to hook method: " + classNotificationData + ".shouldFilterOut" );
            XposedBridge.log(e);
        }
    }

    private void hookAddMethod(final XC_LoadPackage.LoadPackageParam loadPackageParam) {
        XC_MethodHook methodHook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                Object entryParam;
                entryParam = param.args[0];
                StatusBarNotification sbn = (StatusBarNotification)XposedHelpers.getObjectField(entryParam,"notification");
                if (ConcreteNotificationListenerService.SERVICE_NOTIFICATION.equals(sbn.getTag())){
                    extractArchivedNotificationsExtra(sbn);
                }
            }
        };
        try {
            XposedHelpers.findAndHookMethod(classNotificationData,
                    loadPackageParam.classLoader, "add",
                    Class.forName(classNotificationData +"$Entry",true,loadPackageParam.classLoader),
                    NotificationListenerService.RankingMap.class,
                    methodHook);
        } catch (Exception e) {
            XposedBridge.log("Unable to hook method: " + classNotificationData + ".add" );
            XposedBridge.log(e);
        }

    }



    private void hookRemoveMethod(final XC_LoadPackage.LoadPackageParam loadPackageParam) {
        XC_MethodHook methodHook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                String keyParam = (String)param.args[0];

                @SuppressWarnings("unchecked") Map<String,Object> mEntries = (Map<String,Object>)XposedHelpers.getObjectField(param.thisObject,"mEntries");
                Object entry = mEntries.get(keyParam);
                if (entry != null){
                    StatusBarNotification sbn = (StatusBarNotification)XposedHelpers.getObjectField(entry, "notification");
                    if (ConcreteNotificationListenerService.SERVICE_NOTIFICATION.equals(sbn.getTag())){
                        archivedNotifications = null;
                    }
                }
            }
        };
        try {
            XposedHelpers.findAndHookMethod(classNotificationData,
                    loadPackageParam.classLoader, "remove",
                    String.class,
                    NotificationListenerService.RankingMap.class,
                    methodHook);
        } catch (Exception e) {
            XposedBridge.log("Unable to hook method: " + classNotificationData + ".remove" );
            XposedBridge.log(e);
        }

    }
    private void hookUpdateNotificationMethod(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        XC_MethodHook methodHook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                StatusBarNotification sbnParam = (StatusBarNotification)param.args[0];
                if (ConcreteNotificationListenerService.SERVICE_NOTIFICATION.equals(sbnParam.getTag())){
                    extractArchivedNotificationsExtra(sbnParam);
                }
            }
        };
        try {
            XposedHelpers.findAndHookMethod(classBaseStatusBar,
                    loadPackageParam.classLoader, "updateNotification",
                    StatusBarNotification.class,
                    NotificationListenerService.RankingMap.class,
                    methodHook);
        } catch (Exception e) {
            XposedBridge.log("Unable to hook method: " + classBaseStatusBar+ ".updateNotification" );
            XposedBridge.log(e);
        }
    }

    private void extractArchivedNotificationsExtra(StatusBarNotification sbn) {
        PendingIntent pendingIntent = sbn.getNotification().contentIntent;
        //reflection per il metodo getIntent
        Intent notificationIntent = (Intent)XposedHelpers.callMethod(pendingIntent,"getIntent");
        //noinspection unchecked
        archivedNotifications = (Set<String>)notificationIntent.getSerializableExtra(ConcreteNotificationListenerService.ARCHIVED_NOTIFICATIONS_EXTRA);
    }
}
