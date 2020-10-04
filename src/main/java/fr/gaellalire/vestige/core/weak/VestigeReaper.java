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

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;

/**
 * @author Gael Lalire
 */
public class VestigeReaper implements Runnable {

    private ReferenceQueue<Object> referenceQueue;

    private ReapableWeakReference[] last = new ReapableWeakReference[1];

    public VestigeReaper() {
        referenceQueue = new ReferenceQueue<Object>();
    }

    public void addReapable(final Object reapable, final ReaperHelper reaperHelper) {
        synchronized (referenceQueue) {
            last[0] = new ReapableWeakReference(last, reapable, referenceQueue, reaperHelper);
        }
    }

    public void run() {
        try {
            while (true) {
                final Reference<? extends Object> remove = referenceQueue.remove();
                synchronized (referenceQueue) {
                    remove.clear();
                }
            }
        } catch (InterruptedException e) {
            synchronized (referenceQueue) {
                ReapableWeakReference previous = last[0];
                while (previous != null) {
                    previous.clear();
                    previous = previous.getPrevious();
                }
            }
        }
    }

}
