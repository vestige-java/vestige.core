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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import fr.gaellalire.vestige.core.executor.callable.CreateThread;
import fr.gaellalire.vestige.core.weak.ThreadReaperHelper;

/**
 * @author Gael Lalire
 */
public final class VestigeExecutor {

    private LinkedList<FutureTask<Thread>> workerCreationTasks = new LinkedList<FutureTask<Thread>>();

    private ThreadReaperHelper threadReaperHelper;

    public VestigeExecutor() {
        Thread workerCreatorThread = new Thread(new VestigeExecutorWorkerFactory(workerCreationTasks), "vestige-worker-creator");
        workerCreatorThread.setDaemon(true);
        workerCreatorThread.start();
        threadReaperHelper = new ThreadReaperHelper(workerCreatorThread);
    }

    public ThreadReaperHelper getThreadReaperHelper() {
        return threadReaperHelper;
    }

    public VestigeWorker createWorker(final String name, final boolean daemon, final int maxActions) throws InterruptedException {
        if (maxActions < 0) {
            throw new IllegalArgumentException("maxActions must be positive integer");
        }
        final LinkedList<Runnable> tasks = new LinkedList<Runnable>();

        Runnable runnable;
        if (maxActions != 0) {
            runnable = new Runnable() {
                public void run() {
                    int remain = maxActions;
                    mainloop: while (remain > 0) {
                        Runnable task;
                        synchronized (tasks) {
                            while (tasks.isEmpty()) {
                                try {
                                    tasks.wait();
                                } catch (InterruptedException e) {
                                    break mainloop;
                                }
                            }
                            task = tasks.removeFirst();
                        }
                        task.run();
                        remain--;
                    }
                }
            };
        } else {
            runnable = new Runnable() {
                public void run() {
                    mainloop: while (true) {
                        Runnable task;
                        synchronized (tasks) {
                            while (tasks.isEmpty()) {
                                try {
                                    tasks.wait();
                                } catch (InterruptedException e) {
                                    break mainloop;
                                }
                            }
                            task = tasks.removeFirst();
                        }
                        task.run();
                    }

                }
            };
        }
        FutureTask<Thread> futureTask;
        synchronized (workerCreationTasks) {
            futureTask = new FutureTask<Thread>(new CreateThread(null, runnable, name, 0));
            workerCreationTasks.addLast(futureTask);
            workerCreationTasks.notifyAll();
        }
        Thread thread;
        try {
            thread = futureTask.get();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw new Error("Unknown throwable", cause);
        }
        thread.setContextClassLoader(null);
        thread.setDaemon(daemon);
        thread.start();
        return new VestigeWorker(thread, tasks);
    }

}
