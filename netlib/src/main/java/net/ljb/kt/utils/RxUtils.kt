package net.ljb.kt.utils

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import cn.nekocode.rxlifecycle3.LifecycleEvent
import cn.nekocode.rxlifecycle3.RxLifecycle
import cn.nekocode.rxlifecycle3.androidx.RxLifecycleCompact
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.ObservableTransformer
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers

/**
 * Author:Ljb
 * Time:2018/12/28
 * There is a lot of misery in life
 **/
object RxUtils {

    /**
     * rx 取消订阅
     * */
    fun dispose(disposable: Disposable?) {
        if (disposable != null && !disposable.isDisposed) {
            disposable.dispose()
        }
    }

    /**
     * rx 线程调度
     *  io -> android.main
     * */
    fun <T> schedulerIO2Main(): ObservableTransformer<T, T> {
        return ObservableTransformer { upstream ->
            upstream.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
        }
    }


    /**
     * rx 生命周期管理
     * */
    fun <T> bindToLife(obj: Any): ObservableTransformer<T, T> {
        return when (obj) {
            is AppCompatActivity -> {
                RxLifecycleCompact.bind(obj).disposeObservableWhen(
                    LifecycleEvent.DESTROY_VIEW)
            }
            is FragmentActivity -> {
                RxLifecycle.bind(obj).disposeObservableWhen(
                    LifecycleEvent.DESTROY_VIEW)
            }
            is Activity -> {
                RxLifecycle.bind(obj).disposeObservableWhen(
                    LifecycleEvent.DESTROY_VIEW)
            }
            is Fragment -> {
                RxLifecycleCompact.bind(obj).disposeObservableWhen(
                    LifecycleEvent.DESTROY_VIEW)
            }
            else -> {
                throw IllegalArgumentException("obj isn't activity or fragment")
            }
        }
    }

    /**
     * rx 生命周期管理
     * */
    fun <T> bindToLife(obj: Any, event: LifecycleEvent): ObservableTransformer<T, T> {
        return when (obj) {
            is AppCompatActivity -> {
                RxLifecycleCompact.bind(obj).disposeObservableWhen(event)
            }
            is FragmentActivity -> {
                RxLifecycle.bind(obj).disposeObservableWhen(event)
            }
            is Activity -> {
                RxLifecycle.bind(obj).disposeObservableWhen(event)
            }
            is Fragment -> {
                RxLifecycleCompact.bind(obj).disposeObservableWhen(event)
            }
            else -> {
                throw IllegalArgumentException("obj isn't activity or fragment")
            }
        }
    }

}