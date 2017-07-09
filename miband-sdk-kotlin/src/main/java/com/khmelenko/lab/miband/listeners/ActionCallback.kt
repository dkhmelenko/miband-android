package com.khmelenko.lab.miband.listeners

/**
 * Action callback

 * @author Dmytro Khmelenko
 */
interface ActionCallback {

    /**
     * Called on successful completion

     * @param data Fetched data
     */
    fun onSuccess(data: Any)

    /**
     * Called on fail

     * @param errorCode Error code
     * *
     * @param msg       Error message
     */
    fun onFail(errorCode: Int, msg: String)
}
