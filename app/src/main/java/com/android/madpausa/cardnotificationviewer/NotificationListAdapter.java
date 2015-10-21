package com.android.madpausa.cardnotificationviewer;

import android.app.PendingIntent;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build;
import android.service.notification.StatusBarNotification;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RemoteViews;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by ANTPETRE on 02/10/2015.
 *
 * this implements the adapter to show the notification. The Aim is to make it as generic as possible and to provide android-like notification views.
 */
public class NotificationListAdapter  extends RecyclerView.Adapter<NotificationListAdapter.ViewHolder> {
    private static final String TAG = NotificationListAdapter.class.getSimpleName();

    private static final String[] forceDarkBackgroung = new String[]{"PACKAGE_NAME"};
    private static final String[] forceLightBackgroung = new String[]{"PACKAGE_NAME"};

    List <StatusBarNotification> nList;
    Set<String> nSet;
    ConcreteNotificationListenerService nService;

    //the notification filter for this adapter
    NotificationFilter nFilter;

    Context context;
    public NotificationListAdapter(Context c) {
        super();
        context = c;
        nList = new ArrayList<>();
        nSet = new HashSet<>();
        nFilter = new NotificationFilter();
    }

    public void setNotificationService(ConcreteNotificationListenerService nService) {
        this.nService = nService;
        changeDataSet();
    }

    @SuppressWarnings("unused")
    public void setNotificationFilter (NotificationFilter filter){
        nFilter = filter;
    }

    /**
     * adds the notification to the list, handles updates too
     * @param sbn the notification to be added
     */
    public void addNotification (StatusBarNotification sbn){
        //adding the notification to the top of the list
        nList.add(0,sbn);
        //if it is added to the set, notify the adapter
        if (nSet.add(NotificationFilter.getNotificationKey(sbn))){
            notifyItemInserted(0);
        }
        else { //if not, should find where the old one was, remove it and update the adapter
            int pos = findNotificationPosition(sbn);
            nList.remove(pos);

            //Since it has been added to top, if old position was different, notify the move before the change
            if ((pos - 1) != 0){
                notifyItemMoved(pos-1, 0);
            }
            notifyItemChanged(0);
        }

    }

    /**
     * removes notification from the list
     * @param sbn the notificatio nto be removed
     */
    public void removeNotification(StatusBarNotification sbn){
        //removing from the set, if not present, nothing to do
        if(nSet.remove(NotificationFilter.getNotificationKey(sbn))){
            int pos = findNotificationPosition(sbn);
            nList.remove(pos);
            notifyItemRemoved(pos);
        }
    }

    public void clearList (){

    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.notification_list_element, parent, false);
        return new ViewHolder(view, parent);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        StatusBarNotification sbn = nList.get(position);
        RemoteViews nRemote = sbn.getNotification().bigContentView;
        if (nRemote == null)
            nRemote = sbn.getNotification().contentView;

        //removing loeaded views
        holder.getCardView().removeAllViews();

        //setting notification to holder
        holder.setSbn(sbn);
        //adding new view
        View notificationView = nRemote.apply(context, holder.parent);

        holder.setNotificationView(notificationView);

        //setting dark background, when needed
        if (isDarkBackground(holder))
            holder.getCardView().setBackgroundColor(getColor(R.color.cardview_dark_background));
        else holder.getCardView().setBackgroundColor(getColor(R.color.cardview_light_background));

    }

    @Override
    public int getItemCount() {
        return nList.size();
    }

    /**
     * this will update the whole list, meant for reload it, should not be used on punctual updates
     */
    public void changeDataSet() {
        //updating list first, like a stack
        //TODO handle groups using dedicated view

        nList = new ArrayList<>();
        nSet = new HashSet<>();

        if (nService != null){
            Collection<StatusBarNotification> notifications = nService.getNotificationMap().values();
            NotificationGroups nGroup = nService.getNotificationGroups();
            for (StatusBarNotification sbn : notifications){
                //if matches filter, adding it to the list and to the set
                if (nFilter.matchFilter(sbn,nGroup,true)) {
                    nList.add(0, sbn);
                    nSet.add(NotificationFilter.getNotificationKey(sbn));
                }
            }
        }
        notifyDataSetChanged();

    }

    private boolean isDarkBackground(ViewHolder holder){
        StatusBarNotification sbn = holder.getSbn();
        //eventual overridings should go in either Dark or Light Array
        if (Arrays.asList(forceDarkBackgroung).contains(sbn.getPackageName())) return true;
        if (Arrays.asList(forceLightBackgroung).contains(sbn.getPackageName())) return false;

        //base color, to force a light background (in line with default notification theme)
        int textColor = Color.BLACK;

        //getting notification view resources
        Resources nViewResources = holder.getNotificationView().getResources();

        if (nViewResources != null){
            //trying to understand background color from title text
            int titleViewId = nViewResources.getIdentifier("android:id/title", null, null);
            if (titleViewId != 0){
                TextView titleView = (TextView)holder.getNotificationView().findViewById(titleViewId);
                if (titleView != null)
                    textColor=titleView.getTextColors().getDefaultColor();
            }
        }

        //if text is dark, use light background and vice versa
        return !isColorDark(textColor);
    }

    /**
     * thanks stack overflow! checks if a color is dark
     * @param color the actual color
     * @return true if dark
     */
    private boolean isColorDark(int color){
        double darkness = 1-(0.299*Color.red(color) + 0.587*Color.green(color) + 0.114* Color.blue(color))/255;
        return darkness >= 0.5;
    }


    private int getColor (int id){
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            //noinspection deprecation
            return context.getResources().getColor(id);
        else
            return context.getResources().getColor(id,null);
    }

    /**
     * finds the last notification in the adapter list that matches input
     * @param sbn the notification to find
     * @return the position, or -1 if not found
     */
    private int findNotificationPosition (StatusBarNotification sbn){
        String nKey = (NotificationFilter.getNotificationKey(sbn));
        int listSize = nList.size();

        for (int pos = listSize - 1; pos >= 0; pos--){
            if (nKey.equals(NotificationFilter.getNotificationKey(nList.get(pos))))
                return pos;
        }

        return -1;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        StatusBarNotification sbn;
        ViewGroup parent;
        View root;
        View notificationView;

        public ViewHolder ( View r, ViewGroup p){
            super(r);
            root = r;
            parent = p;
            root.setOnClickListener(new NotificationOnClickListener());
        }
        public CardView getCardView (){
            return (CardView) root.findViewById(R.id.cardListitem);
        }
        public void setSbn (StatusBarNotification n){
            sbn = n;
        }
        public void setNotificationView(View view){
            notificationView = view;
            getCardView().addView(view);
        }
        public View getNotificationView(){
            return notificationView;
        }
        public StatusBarNotification getSbn (){
            return sbn;
        }

        public void performOnClick(){
            //TODO open dialog with group notifications
            PendingIntent pendingIntent = sbn.getNotification().contentIntent;
            if (pendingIntent != null) {
                try {
                    pendingIntent.send();
                } catch (PendingIntent.CanceledException e) {
                    Log.e(TAG, "error sending the intent");
                }
            }

            //TODO use swype and animation to implement this logic
            if (nService != null) {
                nService.cancelNotification(sbn);
            }
        }

        private class NotificationOnClickListener implements View.OnClickListener{

            @Override
            public void onClick(View v) {
                performOnClick();
            }
        }

        @Override
        public boolean equals (Object o){
            if (o instanceof ViewHolder){
                ViewHolder v = (ViewHolder) o;
                if (v.getSbn() != null)
                    return NotificationFilter.getNotificationKey(v.getSbn()).equals(NotificationFilter.getNotificationKey(sbn));
            }
            return false;
        }

    }

}
