package com.khmelenko.lab.miband.listeners

/**
 * Listener for data notifications

 * @author Dmytro Khmelenko
 */
interface NotifyListener {

    /**
     * Called when new data arrived

     * @param data Binary data
     */
    fun onNotify(data: ByteArray)
}
