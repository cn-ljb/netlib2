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
package cn.nekocode.rxlifecycle3.androidx;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import cn.nekocode.rxlifecycle3.LifecycleEvent;
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
public class RxLifecycleCompact {
    private static final String FRAGMENT_TAG = "_BINDING_V4_FRAGMENT_";
    private final Observable<LifecycleEvent> mLifecycleObservable;


    public static RxLifecycleCompact bind(@NonNull Observable<LifecycleEvent> lifecycleObservable) {
        return new RxLifecycleCompact(lifecycleObservable);
    }

    public static RxLifecycleCompact bind(@NonNull AppCompatActivity targetActivity) {
        return bind(targetActivity.getSupportFragmentManager());
    }

    public static RxLifecycleCompact bind(@NonNull Fragment targetFragment) {
        return bind(targetFragment.getChildFragmentManager());
    }

    public static RxLifecycleCompact bind(@NonNull FragmentManager fragmentManager) {
        AndroidXFragment fragment = (AndroidXFragment) fragmentManager.findFragmentByTag(FRAGMENT_TAG);
        if (fragment == null) {
            fragment = new AndroidXFragment();

            final FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.add(fragment, FRAGMENT_TAG);
            transaction.commit();

        } else if (fragment.isDetached()) {
            final FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.attach(fragment);
            transaction.commit();
        }

        return bind(fragment.getLifecycleBehavior());
    }

    private RxLifecycleCompact(Observable<LifecycleEvent> lifecycleObservable) {
        this.mLifecycleObservable = lifecycleObservable;
    }

    public Flowable<LifecycleEvent> toFlowable(BackpressureStrategy strategy) {
        return mLifecycleObservable.toFlowable(strategy);
    }

    public Observable<LifecycleEvent> toObservable() {
        return mLifecycleObservable;
    }

    public <T> FlowableTransformer<T, T> cancelFlowableWhen(LifecycleEvent event) {
        return new BindLifecycleFlowableTransformer<T>(mLifecycleObservable, event);
    }

    public <T> ObservableTransformer<T, T> disposeObservableWhen(LifecycleEvent event) {
        return new BindLifecycleObservableTransformer<T>(mLifecycleObservable, event);
    }

    public CompletableTransformer disposeCompletableWhen(LifecycleEvent event) {
        return new BindLifecycleCompletableTransformer(mLifecycleObservable, event);
    }

    public <T> SingleTransformer<T, T> disposeSingleWhen(LifecycleEvent event) {
        return new BindLifecycleSingleTransformer<T>(mLifecycleObservable, event);
    }

    public <T> MaybeTransformer<T, T> disposeMaybeWhen(LifecycleEvent event) {
        return new BindLifecycleMaybeTransformer<T>(mLifecycleObservable, event);
    }
}
