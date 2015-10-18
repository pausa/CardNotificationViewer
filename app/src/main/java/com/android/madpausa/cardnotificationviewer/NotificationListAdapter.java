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
import java.util.List;

/**
 * Created by ANTPETRE on 02/10/2015.
 *
 * this implements the adapter to show the notification. The Aim is to make it as generic as possible and to provide android-like notification views.
 */
public class NotificationListAdapter  extends RecyclerView.Adapter<NotificationListAdapter.ViewHolder> {
    private static final String TAG = NotificationListAdapter.class.getSimpleName();

    private static final String[] forceDarkBackgroung = new String[]{"PACKAGE_NAME"};
    private static final String[] forceLightBackgroung = new String[]{"PACKAGE_NAME"};

    List <StatusBarNotification> nList = null;
    ConcreteNotificationListenerService nService;

    //the notification filter for this adapter
    NotificationFilter nFilter;

    Context context;
    public NotificationListAdapter(Context c) {
        super();
        context = c;
        nList = new ArrayList<>();
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

    @SuppressWarnings("UnusedParameters")
    public void addNotification (StatusBarNotification sbn){
        changeDataSet();
    }

    @SuppressWarnings("UnusedParameters")
    public void removeNotification(StatusBarNotification sbn){
        changeDataSet();
    }

    public void clearList (){
        changeDataSet();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.notification_list_element, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // TODO find a way to dinamically change card color, without knowing the package
        // TODO testing using a dark theme

        StatusBarNotification sbn = nList.get(position);
        RemoteViews nRemote = sbn.getNotification().bigContentView;
        if (nRemote == null)
            nRemote = sbn.getNotification().contentView;

        //removing loeaded views
        holder.getCardView().removeAllViews();

        //setting notification to holder
        holder.setSbn(sbn);
        //adding new view
        holder.setNotificationView(nRemote.apply(context, holder.getCardView()));

        //setting dark background, when needed
        if (isDarkBackground(holder))
            holder.getCardView().setBackgroundColor(getColor(R.color.cardview_dark_background));
        else holder.getCardView().setBackgroundColor(getColor(R.color.cardview_light_background));




    }

    @Override
    public int getItemCount() {
        return nList.size();
    }

    public void changeDataSet() {
        //updating list first, like a stack
        //TODO handle groups using dedicated view
        nList = new ArrayList<>();

        if (nService != null){
            Collection<StatusBarNotification> notifications = nService.getNotificationMap().values();
            NotificationGroups nGroup = nService.getNotificationGroups();

            for (StatusBarNotification sbn : notifications){
                //if matches filter, adding it to the list
                if (nFilter.matchFilter(sbn,nGroup,true))
                    nList.add(0, sbn);
            }
        }
        super.notifyDataSetChanged();

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
            int titleViewId = nViewResources.getIdentifier("android:id/title",null,null);
            if (titleViewId != 0){
                TextView titleView = (TextView)holder.getNotificationView().findViewById(titleViewId);
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


    public class ViewHolder extends RecyclerView.ViewHolder {
        StatusBarNotification sbn;
        View root;
        View notificationView;

        public ViewHolder ( View r){
            super(r);
            root = r;
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
                changeDataSet();
            }
        }

        private class NotificationOnClickListener implements View.OnClickListener{

            @Override
            public void onClick(View v) {
                performOnClick();
            }
        }

    }
}
