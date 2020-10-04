/*
 * Copyright 2013 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.gaellalire.vestige.core.weak;

import java.lang.ref.WeakReference;
import java.util.concurrent.Callable;

/**
 * @author Gael Lalire
 */
public final class WeakCallable<V> implements Callable<V> {

    private WeakReference<Callable<V>> weakReference;

    public WeakCallable(final Callable<V> callable) {
        weakReference = new WeakReference<Callable<V>>(callable);
    }

    @Override
    public V call() throws Exception {
        Callable<V> callable = weakReference.get();
        if (callable != null) {
            return callable.call();
        }
        return null;
    }

}
