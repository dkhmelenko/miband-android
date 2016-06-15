package com.khmelenko.lab.miband.listeners;

/**
 * Listener for hear rate notifications
 *
 * @author Dmytro Khmelenko
 */
public interface HeartRateNotifyListener {

    /**
     * Called when new hear rate data received
     *
     * @param heartRate Hear rate
     */
    void onNotify(int heartRate);
}
