package com.khmelenko.lab.miband.listeners;

/**
 * Listener for realtime steps notifications
 *
 * @author Dmytro Khmelenko
 */
public interface RealtimeStepsNotifyListener {

    /**
     * Called when new notification arrived
     *
     * @param steps Steps amount
     */
    void onNotify(int steps);
}
