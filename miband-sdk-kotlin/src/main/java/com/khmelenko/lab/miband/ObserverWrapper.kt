package com.khmelenko.lab.miband

import io.reactivex.Emitter
import io.reactivex.Observer
import io.reactivex.disposables.Disposable

/**
 * Wraps [Observer] with [Emitter]

 * @author Dmytro Khmelenko (d.khmelenko@gmail.com)
 */
internal class ObserverWrapper<T>(private val mEmitter: Emitter<T>) : Observer<T> {

    override fun onSubscribe(d: Disposable) {
        // do nothing
    }

    override fun onNext(value: T) {
        mEmitter.onNext(value)
    }

    override fun onError(e: Throwable) {
        mEmitter.onError(e)
    }

    override fun onComplete() {
        mEmitter.onComplete()
    }


}
