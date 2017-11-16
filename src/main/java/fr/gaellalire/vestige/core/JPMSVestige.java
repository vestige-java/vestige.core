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

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.ModuleLayer.Controller;
import java.lang.module.Configuration;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.lang.module.ResolvedModule;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import fr.gaellalire.vestige.core.executor.VestigeExecutor;
import fr.gaellalire.vestige.core.parser.ListIndexStringParser;
import fr.gaellalire.vestige.core.parser.NoStateStringParser;
import fr.gaellalire.vestige.core.parser.ResourceEncapsulationEnforcer;
import fr.gaellalire.vestige.core.parser.StringParser;
import fr.gaellalire.vestige.core.resource.JarFileResourceLocator;
import fr.gaellalire.vestige.core.resource.VestigeResourceLocator;
import fr.gaellalire.vestige.core.url.DelegateURLStreamHandlerFactory;

/**
 * @author Gael Lalire
 */
public final class JPMSVestige {

    public static void addModulepath(final File directory, final List<Path> urlList, final String classpath) throws MalformedURLException {
        int pindex = 0;
        int index = classpath.indexOf(File.pathSeparatorChar);
        while (index != -1) {
            if (pindex != index) {
                urlList.add(new File(directory, classpath.substring(pindex, index)).toPath());
            }
            pindex = index + 1;
            index = classpath.indexOf(File.pathSeparatorChar, pindex);
        }
        if (pindex != classpath.length()) {
            urlList.add(new File(directory, classpath.substring(pindex)).toPath());
        }
    }

    public static void createEnforcerConfiguration(final Map<File, String> moduleNamesByFile, final Map<String, String> moduleNameByPackageName,
            final Set<String> encapsulatedPackageNames, final Configuration configuration) throws IOException {
        for (ResolvedModule resolvedModule : configuration.modules()) {
            ModuleReference reference = resolvedModule.reference();
            ModuleDescriptor descriptor = reference.descriptor();
            String name = descriptor.name();
            moduleNamesByFile.put(new File(reference.location().get()), name);
            Set<String> packages = descriptor.packages();
            if (!descriptor.isAutomatic() && !descriptor.isOpen()) {
                Set<String> openPackageNames = descriptor.opens().stream().filter(opens -> opens.targets().size() == 0).map(opens -> opens.source()).collect(Collectors.toSet());
                packages.forEach(packageName -> {
                    moduleNameByPackageName.put(packageName, name);
                    if (!openPackageNames.contains(packageName)) {
                        encapsulatedPackageNames.add(packageName);
                    }
                });
            } else {
                packages.forEach(packageName -> {
                    moduleNameByPackageName.put(packageName, name);
                });
            }
        }
    }

    public static void main(final String[] args) throws Exception {
        int argIndex = 0;

        File directory = null;
        File modulePathFile = null;
        List<String> addModules = Collections.emptyList();

        boolean bind = false;
        String before = null;
        boolean jdk = false;
        boolean manyLoaders = false;
        String name = null;

        String option = args[argIndex];
        while (option.startsWith("--")) {
            if (option.equals("--bind")) {
                bind = true;
            } else if ("--name".equals(option)) {
                name = args[++argIndex];
            } else if ("--add-modules".equals(option)) {
                addModules = Arrays.asList(args[++argIndex].split(","));
            } else if ("--before".equals(option)) {
                before = args[++argIndex];
            } else if ("--many-loaders".equals(option)) {
                jdk = true;
                manyLoaders = true;
            } else if ("--jdk".equals(option)) {
                jdk = true;
            } else {
                throw new IllegalArgumentException("Unknown option " + option);
            }
            option = args[++argIndex];
        }

        if ("mp".equals(option)) {
        } else if ("rmp".equals(option)) {
            directory = new File(args[++argIndex]);
        } else if ("fmp".equals(option)) {
            modulePathFile = new File(args[++argIndex]);
        } else if ("frmp".equals(option)) {
            directory = new File(args[++argIndex]);
            modulePathFile = new File(args[++argIndex]);
        } else {
            throw new IllegalArgumentException("Unknown option " + option);
        }

        List<Path> beforePathList = new ArrayList<Path>();
        List<Path> pathList = new ArrayList<Path>();
        if (modulePathFile == null) {
            String modulePath = args[++argIndex];
            addModulepath(directory, pathList, modulePath);
            if (before != null) {
                addModulepath(directory, beforePathList, before);
            }
        } else {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(modulePathFile));
            try {
                String line = bufferedReader.readLine();
                while (line != null) {
                    addModulepath(directory, pathList, line);
                    line = bufferedReader.readLine();
                }
            } finally {
                bufferedReader.close();
            }
            if (before != null) {
                bufferedReader = new BufferedReader(new FileReader(new File(before)));
                try {
                    String line = bufferedReader.readLine();
                    while (line != null) {
                        addModulepath(directory, beforePathList, line);
                        line = bufferedReader.readLine();
                    }
                } finally {
                    bufferedReader.close();
                }
            }
        }

        String mainModule = args[++argIndex];
        String mainClass = null;
        int indexOf = mainModule.indexOf('/');
        if (indexOf != -1) {
            mainClass = mainModule.substring(indexOf + 1);
            mainModule = mainModule.substring(0, indexOf);
        }
        Path[] paths = new Path[pathList.size()];
        pathList.toArray(paths);

        Path[] beforePaths = new Path[beforePathList.size()];
        beforePathList.toArray(beforePaths);

        String[] dargs = new String[args.length - argIndex - 1];
        System.arraycopy(args, argIndex + 1, dargs, 0, dargs.length);

        List<String> roots = new ArrayList<>(addModules.size() + 1);
        roots.add(mainModule);
        roots.addAll(addModules);

        VestigeResourceLocator[] urls = new VestigeResourceLocator[beforePaths.length + paths.length];
        for (int i = 0; i < beforePaths.length; i++) {
            urls[i] = new JarFileResourceLocator(beforePaths[i].toFile());
        }
        for (int i = 0; i < paths.length; i++) {
            urls[beforePaths.length + i] = new JarFileResourceLocator(paths[i].toFile());
        }

        ModuleLayer boot = ModuleLayer.boot();
        Configuration cf;
        if (bind) {
            cf = Configuration.resolveAndBind(ModuleFinder.of(beforePaths), Collections.singletonList(boot.configuration()), ModuleFinder.of(paths), roots);
        } else {
            cf = Configuration.resolve(ModuleFinder.of(beforePaths), Collections.singletonList(boot.configuration()), ModuleFinder.of(paths), roots);
        }
        Controller controller;
        ClassLoader classLoader = null;
        DelegateURLStreamHandlerFactory streamHandlerFactory = new DelegateURLStreamHandlerFactory();
        URL.setURLStreamHandlerFactory(streamHandlerFactory);
        VestigeCoreContext vestigeCoreContext;
        if (jdk) {
            if (name != null) {
                throw new IllegalArgumentException("--name has to be used without --jdk");
            }
            vestigeCoreContext = new VestigeCoreContext(streamHandlerFactory, new VestigeExecutor());
            if (manyLoaders) {
                controller = ModuleLayer.defineModulesWithManyLoaders(cf, Collections.singletonList(boot), ClassLoader.getSystemClassLoader());
            } else {
                controller = ModuleLayer.defineModulesWithOneLoader(cf, Collections.singletonList(boot), ClassLoader.getSystemClassLoader());
            }
        } else {
            if (manyLoaders) {
                throw new IllegalArgumentException("--many-loaders has to be used with --jdk");
            }
            if (before != null) {
                throw new IllegalArgumentException("--before has to be used with --jdk");
            }
            Map<String, String> moduleNameByPackageName = new HashMap<>();
            Set<String> encapsulatedPackageNames = new HashSet<>();
            Map<File, String> moduleNamesByFile = new HashMap<>();
            List<String> moduleNames = new ArrayList<>(urls.length);

            createEnforcerConfiguration(moduleNamesByFile, moduleNameByPackageName, encapsulatedPackageNames, cf);
            for (Path path : beforePaths) {
                moduleNames.add(moduleNamesByFile.get(path.toFile().getAbsoluteFile()));
            }
            for (Path path : paths) {
                moduleNames.add(moduleNamesByFile.get(path.toFile().getAbsoluteFile()));
            }

            StringParser classStringParser = new NoStateStringParser(0);
            StringParser resourceStringParser = new ResourceEncapsulationEnforcer(classStringParser, encapsulatedPackageNames, -1);
            ModuleEncapsulationEnforcer moduleEncapsulationEnforcer = new ModuleEncapsulationEnforcer(moduleNameByPackageName, new ListIndexStringParser(moduleNames, -2), null);

            VestigeClassLoaderConfiguration[][] vestigeClassLoaderConfigurationsArray = new VestigeClassLoaderConfiguration[][] {
                    new VestigeClassLoaderConfiguration[] {VestigeClassLoaderConfiguration.THIS_PARENT_SEARCHED}};
            final VestigeClassLoader<String> vestigeClassLoader = new VestigeClassLoader<String>(ClassLoader.getSystemClassLoader(), vestigeClassLoaderConfigurationsArray,
                    classStringParser, resourceStringParser, moduleEncapsulationEnforcer, urls);
            controller = ModuleLayer.defineModules(cf, Collections.singletonList(boot), moduleName -> vestigeClassLoader);

            vestigeCoreContext = new VestigeCoreContext(streamHandlerFactory, new VestigeExecutor());
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
            classLoader = vestigeClassLoader;
        }
        Optional<Module> optionalModule = controller.layer().findModule(mainModule);
        if (!optionalModule.isPresent()) {
            throw new IllegalArgumentException("Module " + mainModule + " not found");
        }
        Module module = optionalModule.get();
        if (classLoader == null) {
            classLoader = module.getClassLoader();
        }

        if (mainClass == null) {
            Optional<String> optionalMainClass = module.getDescriptor().mainClass();
            if (!optionalMainClass.isPresent()) {
                throw new IllegalArgumentException("No MainClass attribute in module " + mainModule + ", use <module>/<main-class>");
            }
            mainClass = optionalMainClass.get();
        }
        runMain(classLoader, mainClass, controller, vestigeCoreContext, dargs);
    }

    public static void runMain(final ClassLoader classLoader, final String mainclass, final Controller controller, final VestigeCoreContext vestigeCoreContext,
            final String[] dargs) throws Exception {
        Class<?> loadClass = classLoader.loadClass(mainclass);
        try {
            Method method = loadClass.getMethod("vestigeCoreMain", Controller.class, VestigeCoreContext.class, String[].class);
            Thread currentThread = Thread.currentThread();
            ClassLoader contextClassLoader = currentThread.getContextClassLoader();
            currentThread.setContextClassLoader(classLoader);
            try {
                method.invoke(null, new Object[] {controller, vestigeCoreContext, dargs});
            } finally {
                currentThread.setContextClassLoader(contextClassLoader);
            }
        } catch (NoSuchMethodException e) {
            Vestige.runMain(classLoader, mainclass, vestigeCoreContext, dargs);
        }
    }
}
