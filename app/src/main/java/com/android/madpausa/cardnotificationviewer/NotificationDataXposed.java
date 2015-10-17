package com.android.madpausa.cardnotificationviewer;

import android.app.PendingIntent;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

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
    static final String className = packageName + ".statusbar.NotificationData";
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        if(!loadPackageParam.packageName.equals(packageName))
            return;
        XposedBridge.log("Inside: " + packageName);
        hookAddMethod(loadPackageParam);
        /*hookShouldFilterOutMethod(loadPackageParam);
        hookRemoveMethod(loadPackageParam);*/

    }

    private void hookAddMethod(final XC_LoadPackage.LoadPackageParam loadPackageParam) {
        XC_MethodHook methodHook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                Object entryParam;
                XposedBridge.log("Inside: " + className + ".add");
                entryParam = param.args[0];
                StatusBarNotification sbn = (StatusBarNotification)XposedHelpers.getObjectField(entryParam,"notification");
                if (ConcreteNotificationListenerService.SERVICE_NOTIFICATION.equals(sbn.getTag())){
                    XposedBridge.log("Found ConcreteServer notification!");
                    PendingIntent pendingIntent = sbn.getNotification().contentIntent;
                    Set<String> archivedNotifications = (Set)pendingIntent.getIntent().getSerializableExtra(ConcreteNotificationListenerService.ARCHIVED_NOTIFICATIONS_EXTRA);
                    XposedHelpers.setObjectField(param.thisObject,archivedNotificationsField,archivedNotifications);
                    XposedBridge.log("Found ConcreteServer notification!");
                }
            }
        };
        try {
            XposedHelpers.findAndHookMethod(className,
                    loadPackageParam.classLoader, "add",
                    Class.forName(className+"$Entry",true,loadPackageParam.classLoader),
                    NotificationListenerService.RankingMap.class,
                    methodHook);
        } catch (Exception e) {
            XposedBridge.log("Unable to hook method: " + className + ".add" );
            XposedBridge.log(e);
        }

    }
}
