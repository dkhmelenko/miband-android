package com.khmelenko.lab.miband.listeners;

/**
 * Listener for data notifications
 *
 * @author Dmytro Khmelenko
 */
public interface NotifyListener {

    /**
     * Called when new data arrived
     *
     * @param data Binary data
     */
    void onNotify(byte[] data);
}
