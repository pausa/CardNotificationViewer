package com.android.madpausa.cardnotificationviewer;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
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
import java.util.Collection;
import java.util.List;

/**
 * Created by ANTPETRE on 02/10/2015.
 */
public class NotificationListAdapter  extends RecyclerView.Adapter<NotificationListAdapter.ViewHolder> {
    private static final String TAG = NotificationListAdapter.class.getSimpleName();

    List <StatusBarNotification> nList = null;
    ConcreteNotificationListenerService nService;

    //the notification filter for this adapter
    NotificationFilter nFilter;

    Context context;
    public NotificationListAdapter(Context c) {
        super();
        context = c;
        nList = new ArrayList<StatusBarNotification>();
        nFilter = new NotificationFilter();
    }

    public void setNotificationService(ConcreteNotificationListenerService nService) {
        this.nService = nService;
        changeDataSet();
    }

    public void setNotificationFilter (NotificationFilter filter){
        nFilter = filter;
    }

    public void addNotification (StatusBarNotification sbn){
        changeDataSet();
    }

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
        ViewHolder vh = new ViewHolder(view);
        return vh;
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

        //setting dark background, when needed
        if (isInvertedBackground(sbn))
            holder.getCardView().setBackgroundColor(getColor(R.color.cardview_dark_background));
        else holder.getCardView().setBackgroundColor(getColor(R.color.cardview_light_background));

        holder.setSbn(sbn);
        holder.getCardView().addView(nRemote.apply(context, holder.getCardView()));

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        if(sp.getBoolean(SettingsActivityFragment.TEST_MODE,false)) {
            TextView debugText = new TextView(context);
            debugText.setText("id: " + sbn.getId() + "\n");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                debugText.setText(debugText.getText() + "group: " + sbn.getGroupKey() + "\n");
            debugText.setText(debugText.getText() + "color: " + sbn.getNotification().color + "\n");
            debugText.setText(debugText.getText() + "tag: " + sbn.getTag() + "\n");
            debugText.setTextColor(getColor(R.color.red));
            holder.getCardView().addView(debugText);
        }
    }

    @Override
    public int getItemCount() {
        return nList.size();
    }

    public void changeDataSet() {
        //updating list first, like a stack
        //TODO handle groups using dedicated view
        nList = new ArrayList<StatusBarNotification>();

        if (nService != null){
            Collection<StatusBarNotification> notifications = nService.getNotificationMap().values();
            NotificationGroup nGroup = nService.getNotificationGroups();

            for (StatusBarNotification sbn : notifications){
                //if matches filter, adding it to the list
                if (nFilter.matchFilter(sbn,nGroup,true))
                    nList.add(0, sbn);
            }
        }
        super.notifyDataSetChanged();

    }

    private boolean isInvertedBackground(StatusBarNotification sbn){
        //TODO remove fixed string
        //TODO find a way to handle proper background

        if (sbn.getPackageName().equals("com.google.android.music"))
            return true;
        return false;
    }


    private int getColor (int id){
        //TODO aggiustare quando preparo il completo per lollipop
        if (Build.VERSION.SDK_INT < 23)
            return context.getResources().getColor(id);
        else
            return context.getResources().getColor(id,null);
    }


    public class ViewHolder extends RecyclerView.ViewHolder {
        StatusBarNotification sbn;
        View root;

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

        public void performOnClick(){
            //TODO aprire un dialog con le notifiche grouppate
            PendingIntent pendingIntent = sbn.getNotification().contentIntent;
            if (pendingIntent != null) {
                try {
                    pendingIntent.send();
                } catch (PendingIntent.CanceledException e) {
                    Log.e(TAG, "error sending the intent");
                }
            }
            //rimuovo la notifica dal service, altrimenti non verrebbe rimossa sul click
            //TODO implementare questa logica con lo swype e animazione
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
