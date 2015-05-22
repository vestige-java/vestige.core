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

import java.util.LinkedList;
import java.util.concurrent.FutureTask;

/**
 * @author Gael Lalire
 */
public class VestigeExecutorWorkerFactory implements Runnable {

    private LinkedList<FutureTask<Thread>> workerCreationTasks;

    public VestigeExecutorWorkerFactory(final LinkedList<FutureTask<Thread>> workerCreationTasks) {
        this.workerCreationTasks = workerCreationTasks;
    }

    public void run() {
        mainloop: while (true) {
            Runnable task;
            synchronized (workerCreationTasks) {
                while (workerCreationTasks.isEmpty()) {
                    try {
                        workerCreationTasks.wait();
                    } catch (InterruptedException e) {
                        break mainloop;
                    }
                }
                task = workerCreationTasks.removeFirst();
            }
            task.run();
        }
    }

}
