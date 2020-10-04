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
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import fr.gaellalire.vestige.core.parser.ClassStringParser;
import fr.gaellalire.vestige.core.parser.NoStateStringParser;
import fr.gaellalire.vestige.core.parser.PatternStringParser;
import fr.gaellalire.vestige.core.parser.StringParser;
import fr.gaellalire.vestige.core.resource.DirectoryResourceLocator;
import fr.gaellalire.vestige.core.resource.JarFileResourceLocator;
import fr.gaellalire.vestige.core.resource.VestigeResourceLocator;
import fr.gaellalire.vestige.core.url.DelegateURLStreamHandlerFactory;

/**
 * @author Gael Lalire
 */
public final class Vestige {

    public static void addClasspath(final File directory, final List<File> urlList, final String classpath) throws MalformedURLException {
        int pindex = 0;
        int index = classpath.indexOf(File.pathSeparatorChar);
        while (index != -1) {
            if (pindex != index) {
                urlList.add(new File(directory, classpath.substring(pindex, index)));
            }
            pindex = index + 1;
            index = classpath.indexOf(File.pathSeparatorChar, pindex);
        }
        if (pindex != classpath.length()) {
            urlList.add(new File(directory, classpath.substring(pindex)));
        }
    }

    public static void main(final String[] args) throws Exception {
        String consoleEncoding = System.getProperty("console.encoding");
        if (consoleEncoding != null) {
            System.setOut(new PrintStream(System.out, true, consoleEncoding));
            System.setErr(new PrintStream(System.err, true, consoleEncoding));
        }

        int argIndex = 0;
        File directory = null;
        File classpathFile = null;

        Pattern before = null;
        String name = null;

        String option = args[argIndex];
        while (option.startsWith("--")) {
            if ("--before".equals(option)) {
                before = Pattern.compile(args[++argIndex]);
            } else if ("--name".equals(option)) {
                name = args[++argIndex];
            } else if ("--env-to-prop".equals(option)) {
                String envName = args[++argIndex];
                String propName = args[++argIndex];
                String value = System.getenv(envName);
                if (value != null) {
                    System.setProperty(propName, value);
                }
            } else {
                throw new IllegalArgumentException("Unknown option " + option);
            }
            option = args[++argIndex];
        }

        String fileEncoding = System.getenv("VESTIGE_CORE_FILE_ENCODING");
        String vestigeCoreRelativeDirectory = System.getenv("VESTIGE_CORE_RELATIVE_DIRECTORY");
        if (vestigeCoreRelativeDirectory != null) {
            directory = new File(vestigeCoreRelativeDirectory);
        }
        String vestigeCoreClasspathFile = System.getenv("VESTIGE_CORE_CLASSPATH_FILE");
        if (vestigeCoreClasspathFile != null) {
            classpathFile = new File(vestigeCoreClasspathFile);
        }

        if ("cp".equals(option)) {
        } else if ("ecp".equals(option)) {
            if (classpathFile == null) {
                throw new IllegalArgumentException("Expect at least VESTIGE_CORE_CLASSPATH_FILE in env mode");
            }
        } else {
            throw new IllegalArgumentException("Unknown option " + option);
        }

        List<File> urlList = new ArrayList<File>();
        if (classpathFile == null) {
            String classpath = args[++argIndex];
            addClasspath(directory, urlList, classpath);
        } else {
            FileInputStream fileInputStream = new FileInputStream(classpathFile);
            BufferedReader bufferedReader;
            if (fileEncoding == null) {
                bufferedReader = new BufferedReader(new InputStreamReader(fileInputStream));
            } else {
                bufferedReader = new BufferedReader(new InputStreamReader(fileInputStream, fileEncoding));
            }
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
        String mainClass = args[++argIndex];
        VestigeResourceLocator[] urls = new VestigeResourceLocator[urlList.size()];
        int i = 0;
        for (File file : urlList) {
            if (file.isDirectory()) {
                urls[i] = new DirectoryResourceLocator(file);
            } else {
                urls[i] = new JarFileResourceLocator(file);
            }
            i++;
        }

        String[] dargs = new String[args.length - argIndex - 1];
        System.arraycopy(args, argIndex + 1, dargs, 0, dargs.length);

        run(before, name, urls, mainClass, dargs);
    }

    public static VestigeClassLoader<String> run(final Pattern before, final String name, final VestigeResourceLocator[] urls, final String mainClass, final String[] dargs)
            throws Exception {
        ModuleEncapsulationEnforcer moduleEncapsulationEnforcer = null;
        final VestigeClassLoader<String> vestigeClassLoader;
        if (before != null) {
            StringParser resourceStringParser = new PatternStringParser(before, 1, 0);
            StringParser classStringParser = new ClassStringParser(resourceStringParser);
            VestigeClassLoaderConfiguration[][] vestigeClassLoaderConfigurationsArray = new VestigeClassLoaderConfiguration[2][];
            vestigeClassLoaderConfigurationsArray[0] = new VestigeClassLoaderConfiguration[] {VestigeClassLoaderConfiguration.THIS_PARENT_SEARCHED};
            vestigeClassLoaderConfigurationsArray[1] = new VestigeClassLoaderConfiguration[] {VestigeClassLoaderConfiguration.THIS_PARENT_UNSEARCHED, null};

            vestigeClassLoader = new VestigeClassLoader<String>(ClassLoader.getSystemClassLoader(), vestigeClassLoaderConfigurationsArray, classStringParser, resourceStringParser,
                    moduleEncapsulationEnforcer, urls);
        } else {
            VestigeClassLoaderConfiguration[][] vestigeClassLoaderConfigurationsArray = new VestigeClassLoaderConfiguration[1][];
            vestigeClassLoaderConfigurationsArray[0] = new VestigeClassLoaderConfiguration[] {VestigeClassLoaderConfiguration.THIS_PARENT_SEARCHED};
            StringParser stringParser = new NoStateStringParser(0);
            vestigeClassLoader = new VestigeClassLoader<String>(ClassLoader.getSystemClassLoader(), vestigeClassLoaderConfigurationsArray, stringParser, stringParser,
                    moduleEncapsulationEnforcer, urls);
        }

        final VestigeCoreContext vestigeCoreContext = VestigeCoreContext.buildDefaultInstance();
        DelegateURLStreamHandlerFactory streamHandlerFactory = vestigeCoreContext.getStreamHandlerFactory();
        URL.setURLStreamHandlerFactory(streamHandlerFactory);

        vestigeClassLoader.setDataProtector(null, vestigeCoreContext);
        vestigeClassLoader.setData(vestigeCoreContext, name);
        vestigeCoreContext.setCloseable(new Closeable() {

            @Override
            public void close() throws IOException {
                vestigeClassLoader.close(vestigeCoreContext);
            }
        });
        streamHandlerFactory.setDelegate(new URLStreamHandlerFactory() {

            @Override
            public URLStreamHandler createURLStreamHandler(final String protocol) {
                if (VestigeCoreURLStreamHandler.PROTOCOL.equals(protocol)) {
                    return VestigeClassLoader.URL_STREAM_HANDLER;
                }
                return null;
            }
        });
        runMain(vestigeClassLoader, vestigeClassLoader.loadClass(mainClass), vestigeCoreContext, dargs);
        return vestigeClassLoader;
    }

    public static void runMain(final ClassLoader classLoader, final Class<?> mainClass, final VestigeCoreContext vestigeCoreContext, final String[] dargs) throws Exception {
        if (classLoader == null) {
            try {
                Method method = mainClass.getMethod("vestigeCoreMain", VestigeCoreContext.class, String[].class);
                method.invoke(null, new Object[] {vestigeCoreContext, dargs});
            } catch (NoSuchMethodException e) {
                Method method = mainClass.getMethod("main", String[].class);
                method.invoke(null, new Object[] {dargs});
            }
        } else {
            Thread currentThread = Thread.currentThread();
            ClassLoader contextClassLoader = currentThread.getContextClassLoader();
            try {
                Method method = mainClass.getMethod("vestigeCoreMain", VestigeCoreContext.class, String[].class);
                currentThread.setContextClassLoader(classLoader);
                try {
                    method.invoke(null, new Object[] {vestigeCoreContext, dargs});
                } finally {
                    currentThread.setContextClassLoader(contextClassLoader);
                }
            } catch (NoSuchMethodException e) {
                Method method = mainClass.getMethod("main", String[].class);
                currentThread.setContextClassLoader(classLoader);
                try {
                    method.invoke(null, new Object[] {dargs});
                } finally {
                    currentThread.setContextClassLoader(contextClassLoader);
                }
            }
        }
    }
}
