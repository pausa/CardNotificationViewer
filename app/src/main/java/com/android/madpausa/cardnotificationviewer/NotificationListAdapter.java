package com.android.madpausa.cardnotificationviewer;

import android.app.PendingIntent;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.service.notification.StatusBarNotification;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.RemoteViews;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ANTPETRE on 02/10/2015.
 */
public class NotificationListAdapter  extends BaseAdapter  {
    private static final String TAG = NotificationListAdapter.class.getSimpleName();

    List <StatusBarNotification> nList = null;
    ConcreteNotificationListenerService nService;

    Context context;
    public NotificationListAdapter(Context c) {
        super();
        context = c;
        nList = new ArrayList<StatusBarNotification>();

    }
    public void setnService(ConcreteNotificationListenerService nService) {
        this.nService = nService;
        notifyDataSetChanged();
    }

    public void addNotification (StatusBarNotification sbn){
        notifyDataSetChanged();
    }

    public void removeNotification(StatusBarNotification sbn){
        notifyDataSetChanged();
    }

    public void clearList (){
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
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // TODO usare il tema per impostare il colore o trovare un modo per fare l'inversione dinamica
        // TODO testare con kitkat o usando un tema scuro

        Log.d(TAG, "ricevuta richiesta per vista!");

        StatusBarNotification sbn = nList.get(position);
        ViewHolder holder = null;

        if (convertView == null){
            LayoutInflater inflater = LayoutInflater.from(context);
            convertView = (ViewGroup)inflater.inflate(R.layout.notification_list_element, parent, false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        }
        else{
            holder = (ViewHolder)convertView.getTag();
            holder.getCardView().removeAllViews();
        }

        RemoteViews nRemote = sbn.getNotification().bigContentView;
        if (nRemote == null)
            nRemote = sbn.getNotification().contentView;
        Log.d(TAG, "id notifica: " + ConcreteNotificationListenerService.getNotificationKey(sbn));

        //imposto lo sfondo invertito, se necessario
        if (isInvertedBackground(sbn))
            holder.getCardView().setBackgroundColor(getColor(R.color.cardview_dark_background));
        else holder.getCardView().setBackgroundColor(getColor(R.color.cardview_light_background));

        holder.setSbn(sbn);
        holder.getCardView().addView(nRemote.apply(context, parent));

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        if(sp.getBoolean(SettingsActivityFragment.TEST_MODE,false)) {
            TextView debugText = new TextView(context);
            debugText.setText(ConcreteNotificationListenerService.getNotificationKey(sbn));
            debugText.setTextColor(getColor(R.color.red));
            holder.getCardView().addView(debugText);
        }
        return convertView;
    }

    @Override
    public void notifyDataSetChanged() {
        //va aggiornata prima la lista, altrimenti potrebbero essere resituiti risultati non aggiornati
        //la lista viene aggiornata in modo che le notifiche più recenti siano in cima
        nList = new ArrayList<StatusBarNotification>();
        if (nService != null)
            for (StatusBarNotification sbn : nService.getNotificationMap().values()) nList.add(0, sbn);

        super.notifyDataSetChanged();

    }

    @Override
    public boolean isEmpty() {
        return nList.isEmpty();
    }

    private boolean isInvertedBackground(StatusBarNotification sbn){
        //TODO rimuovere il cablatone
        //TODO gestire sfondo scuro per alcune notifiche

        if (sbn.getPackageName().equals("com.google.android.music"))
            return true;
        return false;
    }

    private int getColor (int id){
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return context.getResources().getColor(id);
        else
            return context.getResources().getColor(id,null);
    }


    public class ViewHolder {
        StatusBarNotification sbn;
        View root;

        public ViewHolder ( View r){
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
            PendingIntent pendingIntent = sbn.getNotification().contentIntent;
            Log.d(TAG, "intent della notifica: " + pendingIntent);
            if (pendingIntent != null) {
                try {
                    pendingIntent.send();
                } catch (PendingIntent.CanceledException e) {
                    Log.e(TAG, "impossibile mandare l'intent");
                }
            }
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
