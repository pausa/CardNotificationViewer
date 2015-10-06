package com.android.madpausa.cardnotificationviewer;

import android.app.ActionBar;
import android.app.Notification;
import android.content.Context;
import android.database.DataSetObserver;
import android.net.wifi.WifiConfiguration;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RemoteViews;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.Inflater;

/**
 * Created by ANTPETRE on 02/10/2015.
 */
public class NotificationListAdapter  extends BaseAdapter  {
    private static final String TAG = NotificationListAdapter.class.getSimpleName();
    Map<String,StatusBarNotification> nMap = null;
    List <StatusBarNotification> nList = null;
    Context context;
    public NotificationListAdapter(Context c) {
        super();
        context = c;
        nMap = new LinkedHashMap<String, StatusBarNotification>();
        nList = new ArrayList<StatusBarNotification>();
    }

    public void setList(List<StatusBarNotification> list) {
        //nList = list;
        for (StatusBarNotification sbn : list){
            nMap.put(ConcreteNotificationListenerService.getNotificationKey(sbn), sbn);
        }
        notifyDataSetChanged();
    }

    public void addNotification (StatusBarNotification sbn){
        nMap.put(ConcreteNotificationListenerService.getNotificationKey(sbn), sbn);
        notifyDataSetChanged();
    }

    public void removeNotification(StatusBarNotification sbn){
        nMap.remove (ConcreteNotificationListenerService.getNotificationKey(sbn));
        notifyDataSetChanged();
    }

    public void clearList (){
        nMap.clear();
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return nMap.size();
    }

    @Override
    public Object getItem(int position) {
        return nList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        //TODO trovare un modo di fare il recycle
        //TODO gestire sfondo scuro per alcune notifiche
        //TODO mostrarel e viste in cards

        Log.d(TAG, "ricevuta richiesta per vista!");
        RemoteViews nRemote = nList.get(position).getNotification().bigContentView;
        if (nRemote == null)
            nRemote = nList.get(position).getNotification().contentView;
        Log.d(TAG, "elementi in lista: " + getCount());

        View nView = nRemote.apply(context, parent);
        nView.setTag(nList.get(position).getNotification());
        return nView;
    }

    @Override
    public void notifyDataSetChanged() {
        //va aggiornata prima la lista, altrimenti potrebbero essere resituiti risultati non aggiornati
        nList = new ArrayList<StatusBarNotification>(nMap.values());
        super.notifyDataSetChanged();

    }

    @Override
    public boolean isEmpty() {
        return nList.isEmpty();
    }


}
