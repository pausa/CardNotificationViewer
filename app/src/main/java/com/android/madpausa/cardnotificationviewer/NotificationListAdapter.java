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
import java.util.List;

/**
 * Created by ANTPETRE on 02/10/2015.
 */
public class NotificationListAdapter  extends RecyclerView.Adapter<NotificationListAdapter.ViewHolder> {
    private static final String TAG = NotificationListAdapter.class.getSimpleName();

    List <StatusBarNotification> nList = null;
    ConcreteNotificationListenerService nService;

    Context context;
    public NotificationListAdapter(Context c) {
        super();
        context = c;
        nList = new ArrayList<StatusBarNotification>();

    }
    public void setNotificationService(ConcreteNotificationListenerService nService) {
        this.nService = nService;
        changeDataSet();
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
        // TODO usare il tema per impostare il colore o trovare un modo per fare l'inversione dinamica
        // TODO testare con kitkat o usando un tema scuro

        StatusBarNotification sbn = nList.get(position);
        RemoteViews nRemote = sbn.getNotification().bigContentView;
        if (nRemote == null)
            nRemote = sbn.getNotification().contentView;

        Log.d(TAG, "id notifica: " + ConcreteNotificationListenerService.getNotificationKey(sbn));

        //rimuovo eventuali viste già caricate
        holder.getCardView().removeAllViews();
        //imposto lo sfondo invertito, se necessario
        if (isInvertedBackground(sbn))
            holder.getCardView().setBackgroundColor(getColor(R.color.cardview_dark_background));
        else holder.getCardView().setBackgroundColor(getColor(R.color.cardview_light_background));
        //holder.getCardView().setBackgroundColor(getColor(sbn.getNotification().color));

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
        //va aggiornata prima la lista, altrimenti potrebbero essere resituiti risultati non aggiornati
        //la lista viene aggiornata in modo che le notifiche più recenti siano in cima
        //TODO gestire bene i gruppi e i summary
        nList = new ArrayList<StatusBarNotification>();
        if (nService != null)
            for (StatusBarNotification sbn : nService.getNotificationMap().values()){
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH &&
                        (sbn.getNotification().flags & Notification.FLAG_GROUP_SUMMARY) == Notification.FLAG_GROUP_SUMMARY)
                    continue;
                else
                    nList.add(0, sbn);
            }

        super.notifyDataSetChanged();

    }

    private boolean isInvertedBackground(StatusBarNotification sbn){
        //TODO rimuovere il cablatone
        //TODO gestire sfondo scuro per alcune notifiche

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
            Log.d(TAG, "intent della notifica: " + pendingIntent);
            if (pendingIntent != null) {
                try {
                    pendingIntent.send();
                } catch (PendingIntent.CanceledException e) {
                    Log.e(TAG, "impossibile mandare l'intent");
                }
            }
            //rimuovo la notifica dal service, altrimenti non verrebbe rimossa sul click
            //TODO implementare questa logica con lo swype e animazione
            if (nService != null) {
                Log.d (TAG, "Notifiche nel service: " + nService.getNotificationMap().size());
                nService.cancelNotification(sbn);
                nService.removeServiceNotification(sbn);
                changeDataSet();
            }
            else Log.d(TAG, "Service null");
        }

        private class NotificationOnClickListener implements View.OnClickListener{

            @Override
            public void onClick(View v) {
                Log.d(TAG, "ricevuto click!");
                performOnClick();
            }
        }

    }
}
