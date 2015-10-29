/**
 *  Copyright 2015 Antonio Petrella
 *
 *  This file is part of Card Notification Viewer
 *
 *   Card Notification Viewer is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Card Notification Viewer is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Card Notification Viewer.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.android.madpausa.cardnotificationviewer;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.app.PendingIntent;
import android.service.notification.StatusBarNotification;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.Scroller;

/**
 * Created by ANTPETRE on 26/10/2015.
 * This represents the viewHolder in the RecyclerView
 */
public class CardElementHolder extends RecyclerView.ViewHolder {
    private static final String TAG = CardElementHolder.class.getSimpleName();

    StatusBarNotification sbn;
    ViewGroup parent;
    View root;
    View notificationView;

    ConcreteNotificationListenerService nService;

    GestureDetectorCompat gDetector;
    Scroller scroller;
    ValueAnimator animator;

    boolean cancel = false;

    public CardElementHolder(View r, ViewGroup p, ConcreteNotificationListenerService s){
        super(r);
        root = r;
        parent = p;
        nService = s;

        //initializing objects for human interaction
        NotificationTouchListener nTouch = new NotificationTouchListener();
        gDetector = new GestureDetectorCompat(root.getContext(),nTouch);
        root.setOnTouchListener(nTouch);
        
        initAnimator();
    }

    private void initAnimator() {
        scroller = new Scroller(root.getContext());
        animator = ValueAnimator.ofFloat(1,0);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                animateScroll();
                if (cancel)
                    root.setAlpha((Float)animation.getAnimatedValue());
            }
        });
        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                root.setLayerType(View.LAYER_TYPE_HARDWARE,null);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                endAnimation();
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                endAnimation();
            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
            private void endAnimation(){
                root.setTranslationX(scroller.getFinalX());
                root.setLayerType(View.LAYER_TYPE_SOFTWARE,null);
                if(cancel)
                    nService.cancelNotification(sbn);
            }
        });
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
        //should reset, in case of recycle
        resetHolder();
    }
    public View getNotificationView(){
        return notificationView;
    }
    public StatusBarNotification getSbn (){
        return sbn;
    }

    private void animateScroll() {
        if (scroller.computeScrollOffset()){
            root.setTranslationX(scroller.getCurrX());
        }
        else {
            root.setTranslationX(scroller.getFinalX());
        }
    }

    private void resetHolder(){
        cancel = false;
        root.setTranslationX(0);
        root.setLayerType(View.LAYER_TYPE_HARDWARE,null);
        root.setAlpha(1);
        root.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    @Override
    public boolean equals (Object o){
        if (o instanceof CardElementHolder){
            CardElementHolder v = (CardElementHolder) o;
            if (v.getSbn() != null)
                return NotificationFilter.getNotificationKey(v.getSbn()).equals(NotificationFilter.getNotificationKey(sbn));
        }
        return false;
    }

    private class NotificationTouchListener extends GestureDetector.SimpleOnGestureListener implements View.OnTouchListener{
        //TODO check standard values
        static final int MIN_FLING_DISTANCE = 200;
        static final int MIN_FLING_VELOCITY = 200;
        static final int MIN_SLIDE_DISTANCE = 5;

        float startX;
        boolean isMoving=false;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            boolean result = gDetector.onTouchEvent(event);
            //handling basic
            if (!result){
                v.setPressed(false);
                switch(event.getActionMasked()){
                    case MotionEvent.ACTION_MOVE:
                        isMoving=true;
                        root.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                        int distance = (int) (event.getX() - startX);
                        //should reserve touch only after a threshold
                        if (Math.abs(distance) > MIN_SLIDE_DISTANCE)
                            parent.requestDisallowInterceptTouchEvent(true);
                        root.setTranslationX(v.getTranslationX() + distance);
                        result = true;
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        int x = Math.round(v.getTranslationX());
                        parent.requestDisallowInterceptTouchEvent(false);
                        scroller.startScroll(x, 0, -1 * x, 0, (int)animator.getDuration());
                        animator.start();
                        result = true;
                        isMoving = false;
                        break;
                }
            }
            return result;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            //shouldn't perform fling if the notification can't be removed
            boolean fling = sbn.isClearable();
            if (fling){
                float distance = Math.abs(root.getTranslationX());
                fling = distance > MIN_FLING_DISTANCE && Math.abs(velocityX) > MIN_FLING_VELOCITY;
                if (fling){
                    cancel = true;
                    //performing fling
                    scroller.fling((int) root.getTranslationX(), 0, Math.round(velocityX), 0, -1 * Integer.MAX_VALUE, Integer.MAX_VALUE, 0, 0);
                    animator.start();
                }
            }
            return fling;
        }

        @Override
        public boolean onDown(MotionEvent e) {
            root.setPressed(true);
            startX = e.getX();
            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            if (!isMoving) {
                root.setPressed(false);
                //TODO open dialog with group notifications
                PendingIntent pendingIntent = sbn.getNotification().contentIntent;
                if (pendingIntent != null) {
                    try {
                        pendingIntent.send();
                    } catch (PendingIntent.CanceledException ex) {
                        Log.e(TAG, "error sending the intent");
                    }
                }
                return true;
            }
            return false;
        }
    }
}