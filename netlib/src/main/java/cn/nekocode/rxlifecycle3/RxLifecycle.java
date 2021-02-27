/*
 * Copyright 2016 nekocode
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.nekocode.rxlifecycle3;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import cn.nekocode.rxlifecycle3.transformer.BindLifecycleCompletableTransformer;
import cn.nekocode.rxlifecycle3.transformer.BindLifecycleFlowableTransformer;
import cn.nekocode.rxlifecycle3.transformer.BindLifecycleMaybeTransformer;
import cn.nekocode.rxlifecycle3.transformer.BindLifecycleObservableTransformer;
import cn.nekocode.rxlifecycle3.transformer.BindLifecycleSingleTransformer;
import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.CompletableTransformer;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.FlowableTransformer;
import io.reactivex.rxjava3.core.MaybeTransformer;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableTransformer;
import io.reactivex.rxjava3.core.SingleTransformer;

/**
 * @author nekocode (nekocode.cn@gmail.com)
 */
public class RxLifecycle {
    private static final String FRAGMENT_TAG = "_BINDING_FRAGMENT_";
    private final Observable<cn.nekocode.rxlifecycle3.LifecycleEvent> mLifecycleObservable;


    public static RxLifecycle bind(@NonNull Observable<cn.nekocode.rxlifecycle3.LifecycleEvent> lifecycleObservable) {
        return new RxLifecycle(lifecycleObservable);
    }

    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    public static RxLifecycle bind(@NonNull Activity targetActivity) {
        return bind(targetActivity.getFragmentManager());
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    public static RxLifecycle bind(@NonNull Fragment targetFragment) {
        return bind(targetFragment.getChildFragmentManager());
    }

    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    public static RxLifecycle bind(@NonNull FragmentManager fragmentManager) {
        BindingFragment fragment = (BindingFragment) fragmentManager.findFragmentByTag(FRAGMENT_TAG);
        if (fragment == null) {
            fragment = new BindingFragment();

            final FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.add(fragment, FRAGMENT_TAG);
            transaction.commit();

        } else if (Build.VERSION.SDK_INT >= 13 && fragment.isDetached()) {
            final FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.attach(fragment);
            transaction.commit();
        }

        return bind(fragment.getLifecycleBehavior());
    }

    private RxLifecycle(Observable<cn.nekocode.rxlifecycle3.LifecycleEvent> lifecycleObservable) {
        this.mLifecycleObservable = lifecycleObservable;
    }

    public Flowable<cn.nekocode.rxlifecycle3.LifecycleEvent> toFlowable(BackpressureStrategy strategy) {
        return mLifecycleObservable.toFlowable(strategy);
    }

    public Observable<cn.nekocode.rxlifecycle3.LifecycleEvent> toObservable() {
        return mLifecycleObservable;
    }

    public <T> FlowableTransformer<T, T> cancelFlowableWhen(cn.nekocode.rxlifecycle3.LifecycleEvent event) {
        return new BindLifecycleFlowableTransformer<T>(mLifecycleObservable, event);
    }

    public <T> ObservableTransformer<T, T> disposeObservableWhen(cn.nekocode.rxlifecycle3.LifecycleEvent event) {
        return new BindLifecycleObservableTransformer<T>(mLifecycleObservable, event);
    }

    public CompletableTransformer disposeCompletableWhen(cn.nekocode.rxlifecycle3.LifecycleEvent event) {
        return new BindLifecycleCompletableTransformer(mLifecycleObservable, event);
    }

    public <T> SingleTransformer<T, T> disposeSingleWhen(cn.nekocode.rxlifecycle3.LifecycleEvent event) {
        return new BindLifecycleSingleTransformer<T>(mLifecycleObservable, event);
    }

    public <T> MaybeTransformer<T, T> disposeMaybeWhen(LifecycleEvent event) {
        return new BindLifecycleMaybeTransformer<T>(mLifecycleObservable, event);
    }
}
