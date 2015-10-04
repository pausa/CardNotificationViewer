package com.android.madpausa.cardnotificationviewer;

import android.app.ActionBar;
import android.app.Notification;
import android.content.Context;
import android.database.DataSetObserver;
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
import java.util.List;
import java.util.zip.Inflater;

/**
 * Created by ANTPETRE on 02/10/2015.
 */
public class NotificationListAdapter  extends BaseAdapter  {
    private static final String TAG = NotificationListAdapter.class.getSimpleName();
    List<StatusBarNotification> nList = null;
    Context context;
    public NotificationListAdapter(Context c) {
        super();
        context = c;
        nList = new ArrayList<StatusBarNotification>();
    }

    public void setList(List<StatusBarNotification> list) {
        nList = list;
        notifyDataSetChanged();
    }

    public void addNotification (StatusBarNotification sbn){
        nList.add(sbn);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return nList.size();
    }

    @Override
    public Object getItem(int position) {
        return nList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return nList.get(position).getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        //TODO trovare un modo di fare il recycle
        Log.d(TAG, "ricevuta richiesta per vista!");
        View nRemote = nList.get(position).getNotification().contentView.apply(context,parent);
        Log.d(TAG, "elementi in lista: " + getCount());
        return nRemote;
        /*LayoutInflater inflater = LayoutInflater.from(context);
        return inflater.inflate(R.layout.notification_list_element, parent, false);*/
    }

    @Override
    public boolean isEmpty() {
        return nList.isEmpty();
    }
}
