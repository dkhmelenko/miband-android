package com.khmelenko.lab.miband.listeners

/**
 * Listener for hear rate notifications
 *
 * @author Dmytro Khmelenko
 */
interface HeartRateNotifyListener {

    /**
     * Called when new hear rate data received
     *
     * @param heartRate Hear rate
     */
    fun onNotify(heartRate: Int)
}
