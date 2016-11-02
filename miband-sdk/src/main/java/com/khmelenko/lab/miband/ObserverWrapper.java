package com.khmelenko.lab.miband;

import io.reactivex.Emitter;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

/**
 * Wraps {@link Observer} with {@link Emitter}
 *
 * @author Dmytro Khmelenko (d.khmelenko@gmail.com)
 */
class ObserverWrapper<T> implements Observer<T> {

    private final Emitter<T> mEmitter;

    public ObserverWrapper(Emitter<T> emitter) {
        mEmitter = emitter;
    }

    @Override
    public void onSubscribe(Disposable d) {
        // do nothing
    }

    @Override
    public void onNext(T value) {
        mEmitter.onNext(value);
    }

    @Override
    public void onError(Throwable e) {
        mEmitter.onError(e);
    }

    @Override
    public void onComplete() {
        mEmitter.onComplete();
    }
}
