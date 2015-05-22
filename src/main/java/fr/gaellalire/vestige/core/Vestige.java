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

package fr.gaellalire.vestige.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import fr.gaellalire.vestige.core.executor.VestigeExecutor;
import fr.gaellalire.vestige.core.parser.NoStateStringParser;
import fr.gaellalire.vestige.core.parser.StringParser;

/**
 * @author Gael Lalire
 */
public final class Vestige {

    private static void addClasspath(final File directory, final List<URL> urlList, final String classpath) throws MalformedURLException {
        int pindex = 0;
        int index = classpath.indexOf(File.pathSeparatorChar);
        while (index != -1) {
            if (pindex != index) {
                urlList.add(new File(directory, classpath.substring(pindex, index)).toURI().toURL());
            }
            pindex = index + 1;
            index = classpath.indexOf(File.pathSeparatorChar, pindex);
        }
        if (pindex != classpath.length()) {
            urlList.add(new File(directory, classpath.substring(pindex)).toURI().toURL());
        }
    }

    public static void main(final String[] args) throws Exception {
        int argIndex = 0;
        File directory = null;
        File classpathFile = null;
        if ("cp".equals(args[argIndex])) {
        } else if ("rcp".equals(args[argIndex])) {
            directory = new File(args[++argIndex]);
        } else if ("fcp".equals(args[argIndex])) {
            classpathFile = new File(args[++argIndex]);
        } else if ("frcp".equals(args[argIndex])) {
            directory = new File(args[++argIndex]);
            classpathFile = new File(args[++argIndex]);
        } else {
            throw new IllegalArgumentException();
        }

        List<URL> urlList = new ArrayList<URL>();
        if (classpathFile == null) {
            String classpath = args[++argIndex];
            addClasspath(directory, urlList, classpath);
        } else {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(classpathFile));
            try {
                String line = bufferedReader.readLine();
                while (line != null) {
                    addClasspath(directory, urlList, line);
                    line = bufferedReader.readLine();
                }
            } finally {
                bufferedReader.close();
            }
        }
        String mainclass = args[++argIndex];
        URL[] urls = new URL[urlList.size()];
        urlList.toArray(urls);

        String[] dargs = new String[args.length - argIndex];
        System.arraycopy(args, argIndex, dargs, 0, dargs.length);

        StringParser stringParser = new NoStateStringParser(0);
        VestigeClassLoader<Void> vestigeClassLoader = new VestigeClassLoader<Void>(ClassLoader.getSystemClassLoader(), Collections.singletonList(Collections
                .<VestigeClassLoader<Void>> singletonList(null)), stringParser, stringParser, urls);
        runMain(vestigeClassLoader, mainclass, null, dargs);
    }

    public static void runMain(final ClassLoader classLoader, final String mainclass, final VestigeExecutor vestigeExecutor, final String[] dargs) throws Exception {
        Class<?> loadClass = classLoader.loadClass(mainclass);
        try {
            Method method = loadClass.getMethod("vestigeCoreMain", VestigeExecutor.class, String[].class);
            Thread.currentThread().setContextClassLoader(classLoader);
            try {
                if (vestigeExecutor == null) {
                    method.invoke(null, new Object[] {new VestigeExecutor(), dargs});
                } else {
                    method.invoke(null, new Object[] {vestigeExecutor, dargs});
                }
            } finally {
                Thread.currentThread().setContextClassLoader(null);
            }
        } catch (NoSuchMethodException e) {
            Method method = loadClass.getMethod("main", String[].class);
            Thread.currentThread().setContextClassLoader(classLoader);
            try {
                method.invoke(null, new Object[] {dargs});
            } finally {
                Thread.currentThread().setContextClassLoader(null);
            }
        }
    }

}
