package com.khmelenko.lab.miband.listeners

/**
 * Listener for realtime steps notifications
 *
 * @author Dmytro Khmelenko
 */
interface RealtimeStepsNotifyListener {

    /**
     * Called when new notification arrived
     *
     * @param steps Steps amount
     */
    fun onNotify(steps: Int)
}
