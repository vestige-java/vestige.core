/*
 * Copyright 2020 The Apache Software Foundation.
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.Timer;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import fr.gaellalire.vestige.core.ModuleEncapsulationEnforcer;
import fr.gaellalire.vestige.core.VestigeClassLoader;
import fr.gaellalire.vestige.core.VestigeClassLoaderConfiguration;
import fr.gaellalire.vestige.core.executor.callable.ClassForName;
import fr.gaellalire.vestige.core.executor.callable.CreateThread;
import fr.gaellalire.vestige.core.executor.callable.CreateTimer;
import fr.gaellalire.vestige.core.executor.callable.CreateVestigeClassLoader;
import fr.gaellalire.vestige.core.executor.callable.InvokeMethod;
import fr.gaellalire.vestige.core.parser.StringParser;
import fr.gaellalire.vestige.core.resource.VestigeResourceLocator;

/**
 * @author Gael Lalire
 */
public class VestigeWorker {

    private Thread thread;

    private LinkedList<Runnable> tasks;

    public VestigeWorker(final Thread thread, final LinkedList<Runnable> tasks) {
        this.thread = thread;
        this.tasks = tasks;
    }

    public <V> Future<V> submit(final Callable<V> callable) {
        FutureTask<V> futureTask = new FutureTask<V>(callable);
        synchronized (tasks) {
            tasks.addLast(futureTask);
            tasks.notifyAll();
        }
        return futureTask;
    }

    public Object invoke(final ClassLoader contextClassLoader, final Method method, final Object obj, final Object... args)
            throws InterruptedException, IllegalAccessException, InvocationTargetException {
        Future<Object> submit = submit(new InvokeMethod(contextClassLoader, method, obj, args));
        try {
            return submit.get();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else if (cause instanceof Error) {
                throw (Error) cause;
            } else if (cause instanceof IllegalAccessException) {
                throw (IllegalAccessException) cause;
            } else if (cause instanceof InvocationTargetException) {
                throw (InvocationTargetException) cause;
            }
            throw new Error("Unknown throwable", cause);
        }
    }

    public <E> VestigeClassLoader<E> createVestigeClassLoader(final ClassLoader parent, final VestigeClassLoaderConfiguration[][] vestigeClassloadersList,
            final StringParser classStringParser, final StringParser resourceStringParser, final ModuleEncapsulationEnforcer moduleEncapsulationEnforcer,
            final VestigeResourceLocator... urls) throws InterruptedException {
        Future<VestigeClassLoader<E>> submit = submit(
                new CreateVestigeClassLoader<E>(parent, vestigeClassloadersList, classStringParser, resourceStringParser, moduleEncapsulationEnforcer, urls));
        try {
            return submit.get();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw new Error("Unknown throwable", cause);
        }
    }

    /**
     * Thread get inheritedAccessControlContext.
     * @throws InterruptedException
     */
    public Thread createThread(final ThreadGroup group, final Runnable target, final String name, final long stackSize) throws InterruptedException {
        Future<Thread> submit = submit(new CreateThread(group, target, name, stackSize));
        try {
            return submit.get();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw new Error("Unknown throwable", cause);
        }
    }

    /**
     * Thread get inheritedAccessControlContext.
     * @throws InterruptedException
     */
    public Timer createTimer(final String name, final boolean isDaemon) throws InterruptedException {
        Future<Timer> submit = submit(new CreateTimer(name, isDaemon));
        try {
            return submit.get();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw new Error("Unknown throwable", cause);
        }
    }

    /**
     * Some class keep stack trace.
     * @throws InterruptedException
     */
    public Class<?> classForName(final ClassLoader loader, final String className) throws ClassNotFoundException, InterruptedException {
        Future<Class<?>> submit = submit(new ClassForName(loader, className));
        try {
            return submit.get();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else if (cause instanceof Error) {
                throw (Error) cause;
            } else if (cause instanceof ClassNotFoundException) {
                throw (ClassNotFoundException) cause;
            }
            throw new Error("Unknown throwable", cause);
        }
    }

    public void interrupt() {
        thread.interrupt();
    }

    public void join() throws InterruptedException {
        thread.join();
    }

}
