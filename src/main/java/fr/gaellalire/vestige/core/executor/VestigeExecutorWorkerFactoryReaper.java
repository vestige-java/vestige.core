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

package fr.gaellalire.vestige.core.executor;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

/**
 * @author Gael Lalire
 */
public class VestigeExecutorWorkerFactoryReaper implements Runnable {

    private ReferenceQueue<VestigeExecutor> referenceQueue;

    private Thread vestigeExecutorWorkerFactoryThread;

    @SuppressWarnings("unused")
    private WeakReference<VestigeExecutor> weakReference;

    public VestigeExecutorWorkerFactoryReaper(final VestigeExecutor vestigeExecutor, final Thread vestigeExecutorWorkerFactoryThread) {
        referenceQueue = new ReferenceQueue<VestigeExecutor>();
        weakReference = new WeakReference<VestigeExecutor>(vestigeExecutor, referenceQueue);
        this.vestigeExecutorWorkerFactoryThread = vestigeExecutorWorkerFactoryThread;
    }

    public void run() {
        try {
            referenceQueue.remove();
            vestigeExecutorWorkerFactoryThread.interrupt();
        } catch (InterruptedException e) {
            vestigeExecutorWorkerFactoryThread.interrupt();
        }
    }

}
