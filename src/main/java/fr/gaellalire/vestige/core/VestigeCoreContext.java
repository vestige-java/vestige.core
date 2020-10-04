/*
 * Copyright 2017 The Apache Software Foundation.
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

package fr.gaellalire.vestige.core;

import java.io.Closeable;

import fr.gaellalire.vestige.core.executor.VestigeExecutor;
import fr.gaellalire.vestige.core.url.DelegateURLStreamHandlerFactory;
import fr.gaellalire.vestige.core.weak.VestigeReaper;

/**
 * @author Gael Lalire
 */
public final class VestigeCoreContext {

    private DelegateURLStreamHandlerFactory streamHandlerFactory;

    private VestigeReaper vestigeReaper;

    private VestigeExecutor vestigeExecutor;

    private Closeable closeable;

    public static VestigeCoreContext buildDefaultInstance() {
        DelegateURLStreamHandlerFactory streamHandlerFactory = new DelegateURLStreamHandlerFactory();

        VestigeReaper vestigeReaper = new VestigeReaper();
        Thread workerCreatorReaperThread = new Thread(vestigeReaper, "vestige-reaper");
        workerCreatorReaperThread.setDaemon(true);
        workerCreatorReaperThread.start();

        VestigeExecutor vestigeExecutor = new VestigeExecutor();
        vestigeReaper.addReapable(vestigeExecutor, vestigeExecutor.getThreadReaperHelper());

        return new VestigeCoreContext(streamHandlerFactory, vestigeReaper, vestigeExecutor);
    }

    public VestigeCoreContext(final DelegateURLStreamHandlerFactory streamHandlerFactory, final VestigeReaper vestigeReaper, final VestigeExecutor vestigeExecutor) {
        this.streamHandlerFactory = streamHandlerFactory;
        this.vestigeReaper = vestigeReaper;
        this.vestigeExecutor = vestigeExecutor;
    }

    public void setCloseable(final Closeable closeable) {
        this.closeable = closeable;
    }

    public DelegateURLStreamHandlerFactory getStreamHandlerFactory() {
        return streamHandlerFactory;
    }

    public VestigeReaper getVestigeReaper() {
        return vestigeReaper;
    }

    public VestigeExecutor getVestigeExecutor() {
        return vestigeExecutor;
    }

    public Closeable getCloseable() {
        return closeable;
    }

}
