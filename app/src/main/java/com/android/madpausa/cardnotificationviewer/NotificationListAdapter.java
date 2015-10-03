package com.android.madpausa.cardnotificationviewer;

import android.app.ActionBar;
import android.database.DataSetObserver;
import android.service.notification.StatusBarNotification;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ANTPETRE on 02/10/2015.
 */
public class NotificationListAdapter extends BaseAdapter {
    List<StatusBarNotification> nList = null;

    public NotificationListAdapter() {
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
        //TODO definire un layout per la vista, per ora textview
        TextView tw = new TextView(parent.getContext());
        tw.setWidth(ViewGroup.LayoutParams.MATCH_PARENT);
        tw.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        tw.setText(nList.get(position).toString());
        return tw;
    }

    @Override
    public boolean isEmpty() {
        return nList.isEmpty();
    }
}
