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
                XposedBridge.log("Inside: " + classNotificationData + ".shouldFilterOut");
                StatusBarNotification sbnParam = (StatusBarNotification)param.args[0];
                if (archivedNotifications != null && archivedNotifications.contains(ConcreteNotificationListenerService.getNotificationKey(sbnParam))) {
                    XposedBridge.log("this should be hidden: " + ConcreteNotificationListenerService.getNotificationKey(sbnParam));
                    param.setResult(true);
                    return;
                }
            }
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                XposedBridge.log("Inside: " + classNotificationData + ".shouldFilterOut");
                StatusBarNotification sbnParam = (StatusBarNotification)param.args[0];
                if((boolean)param.getResult())
                    XposedBridge.log("this should be hidden: " + ConcreteNotificationListenerService.getNotificationKey(sbnParam));
                else
                    XposedBridge.log("this should be shown: " + ConcreteNotificationListenerService.getNotificationKey(sbnParam));
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
                XposedBridge.log("Inside: " + classNotificationData + ".add");
                entryParam = param.args[0];
                StatusBarNotification sbn = (StatusBarNotification)XposedHelpers.getObjectField(entryParam,"notification");
                if (ConcreteNotificationListenerService.SERVICE_NOTIFICATION.equals(sbn.getTag())){
                    XposedBridge.log("Found ConcreteServer notification!");
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
                String keyParam = (String)param.args[0];;
                XposedBridge.log("Inside: " + classNotificationData + ".remove");
                Map<String,Object> mEntries = (Map)XposedHelpers.getObjectField(param.thisObject,"mEntries");
                Object entry = mEntries.get(keyParam);
                if (entry != null){
                    StatusBarNotification sbn = (StatusBarNotification)XposedHelpers.getObjectField(entry, "notification");
                    if (ConcreteNotificationListenerService.SERVICE_NOTIFICATION.equals(sbn.getTag())){
                        XposedBridge.log("Found ConcreteServer notification!");
                        XposedBridge.log ("Archived Notifications: " + archivedNotifications.toString());
                        archivedNotifications = null;
                        XposedBridge.log("dumped archived notifications set!");
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
                StatusBarNotification sbnParam = (StatusBarNotification)param.args[0];;
                XposedBridge.log("Inside: " + classBaseStatusBar + ".updateNotification");
                if (ConcreteNotificationListenerService.SERVICE_NOTIFICATION.equals(sbnParam.getTag())){
                    XposedBridge.log("Found ConcreteServer notification!");
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
        archivedNotifications = (Set)notificationIntent.getSerializableExtra(ConcreteNotificationListenerService.ARCHIVED_NOTIFICATIONS_EXTRA);
        XposedBridge.log("loaded archived notifications set!" + archivedNotifications.toString());
    }
}