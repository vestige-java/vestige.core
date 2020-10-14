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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.security.SecureClassLoader;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;

import fr.gaellalire.vestige.core.parser.StringParser;
import fr.gaellalire.vestige.core.resource.PackageMetadata;
import fr.gaellalire.vestige.core.resource.VestigeResource;
import fr.gaellalire.vestige.core.resource.VestigeResourceLocator;

/**
 * @author Gael Lalire
 */
public final class VestigeClassLoader<E> extends SecureClassLoader implements VestigeClassLoaderConfiguration {

    public static final VestigeCoreURLStreamHandler URL_STREAM_HANDLER = new VestigeCoreURLStreamHandler();

    public static final boolean GET_CLASS_LOADING_LOCK_METHOD_EXISTS;

    public static final Method GET_DEFINED_PACKAGE_METHOD;

    static {
        boolean getClassLoadingLockExists = false;
        Class<?> classLoaderClass = ClassLoader.class.getClass();
        try {
            classLoaderClass.getDeclaredMethod("getClassLoadingLock", String.class);
            getClassLoadingLockExists = true;
        } catch (Exception e) {
            // ignore
        }
        if (getClassLoadingLockExists) {
            Method registerAsParallelCapableMethod = null;
            try {
                registerAsParallelCapableMethod = classLoaderClass.getDeclaredMethod("registerAsParallelCapable");
                registerAsParallelCapableMethod.invoke(null);
            } catch (Exception e) {
                // ignore
            }
        }
        GET_CLASS_LOADING_LOCK_METHOD_EXISTS = getClassLoadingLockExists;
        Method getDefinedPackageMethod = null;
        try {
            getDefinedPackageMethod = classLoaderClass.getDeclaredMethod("getDefinedPackage", String.class);
        } catch (Exception e) {
            // ignore
        }
        GET_DEFINED_PACKAGE_METHOD = getDefinedPackageMethod;
    }

    public static final Enumeration<URL> EMPTY_URL_ENUMERATION = Collections.enumeration(Collections.<URL> emptyList());

    private VestigeClassLoaderConfiguration[][] vestigeClassLoaderConfigurationsList;

    private VestigeResourceLocator[] jarFiles;

    // encapsulation rules
    private StringParser classStringParser;

    // encapsulation rules
    private StringParser resourceStringParser;

    private E data;

    private Object dataProtector;

    private ClassLoader parent;

    private ModuleEncapsulationEnforcer moduleEncapsulationEnforcer;

    private int urlIndex = -1;

    protected int getUrlIndex() {
        return urlIndex;
    }

    protected void setUrlIndex(final int urlIndex) {
        this.urlIndex = urlIndex;
    }

    public VestigeClassLoader(final ClassLoader parent, final VestigeClassLoaderConfiguration[][] vestigeClassLoaderConfigurationsList, final StringParser classStringParser,
            final StringParser resourceStringParser, final ModuleEncapsulationEnforcer moduleEncapsulationEnforcer, final VestigeResourceLocator... jarFiles) {
        super(parent);
        this.parent = parent;
        this.vestigeClassLoaderConfigurationsList = vestigeClassLoaderConfigurationsList;
        this.classStringParser = classStringParser;
        this.resourceStringParser = resourceStringParser;
        this.moduleEncapsulationEnforcer = moduleEncapsulationEnforcer;
        this.jarFiles = jarFiles;
    }

    public E getData(final Object dataProtector) {
        if (this.dataProtector == dataProtector) {
            return data;
        }
        return null;
    }

    public void setDataProtector(final Object dataProtector, final Object newDataProtector) {
        if (this.dataProtector == dataProtector) {
            this.dataProtector = newDataProtector;
        }
    }

    public void setData(final Object dataProtector, final E data) {
        if (this.dataProtector == dataProtector) {
            this.data = data;
        }
    }

    public void close(final Object dataProtector) throws IOException {
        if (this.dataProtector == dataProtector) {
            for (VestigeResourceLocator vestigeResourceLocator : jarFiles) {
                vestigeResourceLocator.close();
            }
        }
    }

    protected Object getClassLoadingLock(final String className) {
        if (GET_CLASS_LOADING_LOCK_METHOD_EXISTS) {
            return super.getClassLoadingLock(className);
        } else {
            return this;
        }
    }

    protected Class<?> superLoadClass(final String name, final boolean parentSearched) throws ClassNotFoundException {
        if (parentSearched) {
            try {
                return parent.loadClass(name);
            } catch (ClassNotFoundException e) {
                // ignore
            }
        }
        synchronized (getClassLoadingLock(name)) {
            Class<?> loadedClass = findLoadedClass(name);
            if (loadedClass != null) {
                return loadedClass;
            }
            try {
                return findClass(name);
            } catch (ClassNotFoundException e) {
                // ignore
            }
        }
        return null;
    }

    @Override
    public Class<?> loadClass(final String name) throws ClassNotFoundException {
        int match = classStringParser.match(name);
        if (match < 0) {
            if (data != null) {
                throw new ClassNotFoundException(name + " in " + data.toString());
            } else {
                throw new ClassNotFoundException(name);
            }
        }
        VestigeClassLoaderConfiguration[] classLoaderConfigurations = vestigeClassLoaderConfigurationsList[match];
        if (classLoaderConfigurations != null) {
            for (VestigeClassLoaderConfiguration classLoaderConfiguration : classLoaderConfigurations) {
                if (classLoaderConfiguration == null) {
                    try {
                        return parent.loadClass(name);
                    } catch (ClassNotFoundException e) {
                        // ignore
                    }
                } else {
                    boolean parentSearched = classLoaderConfiguration.isParentSearched();
                    VestigeClassLoader<?> vestigeClassLoader = classLoaderConfiguration.getVestigeClassLoader();
                    if (vestigeClassLoader == null) {
                        if (parentSearched) {
                            try {
                                return parent.loadClass(name);
                            } catch (ClassNotFoundException e) {
                                // ignore
                            }
                        }
                        synchronized (getClassLoadingLock(name)) {
                            Class<?> loadedClass = findLoadedClass(name);
                            if (loadedClass != null) {
                                return loadedClass;
                            }
                            try {
                                return findClass(name);
                            } catch (ClassNotFoundException e) {
                                // ignore
                            }
                        }
                    } else {
                        try {
                            Class<?> superLoadClass = vestigeClassLoader.superLoadClass(name, parentSearched);
                            if (superLoadClass != null) {
                                return superLoadClass;
                            }
                        } catch (ClassNotFoundException e) {
                            // ignore
                        }
                    }
                }
            }
        }
        if (data != null) {
            throw new ClassNotFoundException(name + " in " + data.toString());
        } else {
            throw new ClassNotFoundException(name);
        }
    }

    public static String getPackageNameFromClassName(final String className) {
        int lastIndexOf = className.lastIndexOf('.');
        if (lastIndexOf == -1) {
            return null;
        }
        return className.substring(0, lastIndexOf);
    }

    public static String getPackageNameFromResourceName(final String resourceName) {
        int lastIndexOf = resourceName.lastIndexOf('/');
        if (lastIndexOf == -1) {
            return null;
        }
        return resourceName.substring(0, lastIndexOf).replace('/', '.');
    }

    @Override
    protected Class<?> findClass(final String className) throws ClassNotFoundException {
        String packageName = getPackageNameFromClassName(className);
        String name = className.replace('.', '/').concat(".class");
        for (VestigeResourceLocator jarFile : jarFiles) {
            VestigeResource entry = jarFile.findResource(name);
            if (entry != null) {
                try {
                    return defineClass(className, jarFile, entry, moduleEncapsulationEnforcer != null && moduleEncapsulationEnforcer.getModuleName(packageName) != null);
                } catch (IOException e) {
                    if (data != null) {
                        throw new ClassNotFoundException(name + " in " + data.toString(), e);
                    } else {
                        throw new ClassNotFoundException(name, e);
                    }
                }
            }
        }
        if (data != null) {
            throw new ClassNotFoundException(name + " in " + data.toString());
        } else {
            throw new ClassNotFoundException(name);
        }
    }

    protected Class<?> findClass(final String moduleName, final String className) {
        if (moduleName == null || moduleName.equals(moduleEncapsulationEnforcer.getModuleName(getPackageNameFromClassName(className)))) {
            // moduleName == null => unnamed module => no encapsulation
            String name = className.replace('.', '/').concat(".class");
            for (VestigeResourceLocator jarFile : jarFiles) {
                VestigeResource entry = jarFile.findResource(name);
                if (entry != null) {
                    try {
                        return defineClass(className, jarFile, entry, true);
                    } catch (IOException e) {
                        return null;
                    }
                }
            }
            return null;
        }
        return null;
    }

    private Class<?> defineClass(final String className, final VestigeResourceLocator man, final VestigeResource entry, final boolean namedModule) throws IOException {
        int size = (int) entry.getSize();
        byte[] buf = null;
        int off = 0;
        int len = 0;
        InputStream inputStream = entry.getInputStream();
        int read = -1;
        if (size != -1) {
            len = size;
            buf = new byte[size];
            read = inputStream.read(buf, off, len);
            while (read != -1) {
                off += read;
                len -= read;
                if (len == 0) {
                    // buffer is full
                    break;
                }
                read = inputStream.read(buf, off, len);
            }
            if (read != -1) {
                read = inputStream.read();
            }
            if (read == -1) {
                // no need to read more
                off = 0;
                len = size - len;
            } else {
                // buffer is full
                off = 0;
                len = size;
                size = -1;
            }
        }
        if (size == -1) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(len + 1024);
            if (read != -1) {
                baos.write(buf, off, len);
                baos.write(read);
            }
            buf = new byte[1024];
            read = inputStream.read(buf, 0, len);
            while (read != -1) {
                baos.write(buf, 0, read);
                read = inputStream.read(buf, 0, len);
            }
            buf = baos.toByteArray();
            off = 0;
            len = buf.length;
        }
        URL csURL = entry.getCodeSourceURL();
        if (!namedModule) {
            // have to define package
            int pos = className.lastIndexOf('.');
            if (pos != -1) {
                String pn = className.substring(0, pos);
                defineOrCheckPackage(pn, man, csURL);
            }
        }
        CodeSigner[] signers = entry.getCodeSigners();
        CodeSource cs = new CodeSource(csURL, signers);

        return defineClass(className, buf, off, len, cs);
    }

    /**
     * Defines a package in this ClassLoader. If the package is already defined then its sealing needs to be checked if sealed by the legacy sealing mechanism.
     * @throws SecurityException if there is a sealing violation (JAR spec)
     */
    private Package defineOrCheckPackage(final String pn, final VestigeResourceLocator man, final URL url) {
        Package pkg = getAndVerifyPackage(pn, man, url);
        if (pkg == null) {
            try {
                pkg = definePackage(pn, man, url);
            } catch (IllegalArgumentException iae) {
                // defined by another thread so need to re-verify
                pkg = getAndVerifyPackage(pn, man, url);
                if (pkg == null) {
                    throw new InternalError("Cannot find package: " + pn);
                }
            }
        }
        return pkg;
    }

    /**
     * Defines a new package in this ClassLoader. The attributes in the specified Manifest are use to get the package version and sealing information.
     * @throws IOException
     * @throws IllegalArgumentException if the package name duplicates an existing package either in this class loader or one of its ancestors
     */
    private Package definePackage(final String pn, final VestigeResourceLocator man, final URL url) {
        URL sealBase = null;

        PackageMetadata packageMetadata = man.getPackageMetadata(pn);
        if (packageMetadata.isSealed()) {
            sealBase = url;
        }
        return definePackage(pn, packageMetadata.getSpecTitle(), packageMetadata.getImplVersion(), packageMetadata.getSpecVendor(), packageMetadata.getImplTitle(),
                packageMetadata.getImplVersion(), packageMetadata.getImplVendor(), sealBase);
    }

    /**
     * Get the Package with the specified package name. If defined then verify that it against the manifest and code source.
     * @throws SecurityException if there is a sealing violation (JAR spec)
     */
    private Package getAndVerifyPackage(final String pn, final VestigeResourceLocator man, final URL url) {
        Package pkg = null;
        if (GET_DEFINED_PACKAGE_METHOD == null) {
            pkg = getPackage(pn);
        } else {
            try {
                pkg = (Package) GET_DEFINED_PACKAGE_METHOD.invoke(this, pn);
            } catch (IllegalAccessException e) {
                // ignore
            } catch (IllegalArgumentException e) {
                // ignore
            } catch (InvocationTargetException e) {
                // ignore
            }
        }
        if (pkg != null) {
            if (pkg.isSealed()) {
                if (!pkg.isSealed(url)) {
                    throw new SecurityException("sealing violation: package " + pn + " is sealed");
                }
            } else {
                // can't seal package if already defined without sealing
                if ((man != null) && man.isPackageSealed(pn)) {
                    throw new SecurityException("sealing violation: can't seal package " + pn + ": already defined");
                }
            }
        }
        return pkg;
    }

    protected VestigeResourceLocator getVestigeResourceLocator(final int index) {
        return jarFiles[index];
    }

    protected URL findResource(final String moduleName, final String name) throws IOException {
        if (moduleName == null || moduleEncapsulationEnforcer == null) {
            // moduleName == null => unnamed module => no encapsulation
            return innerFindResource(name);
        }
        int match = moduleEncapsulationEnforcer.findLocatorIndex(moduleName, name);
        if (match == -2) {
            return null;
        } else if (match == -1) {
            return innerFindResource(name);
        }
        VestigeResource findResource = jarFiles[match].findResource(name);
        if (findResource == null) {
            return null;
        }
        return map(match, findResource);
    }

    private URL map(final int locatorIndex, final VestigeResource resource) throws IOException {
        if (urlIndex == -1) {
            URL_STREAM_HANDLER.createIndex(this);
        }
        return URL_STREAM_HANDLER.map(urlIndex, locatorIndex, resource);
    }

    private URL innerFindResource(final String name) {
        int i = 0;
        for (VestigeResourceLocator jarFile : jarFiles) {
            VestigeResource entry = jarFile.findResource(name);
            if (entry != null) {
                try {
                    return map(i, entry);
                } catch (IOException e) {
                    return null;
                }
            }
            i++;
        }
        return null;
    }

    private URL innerFindResources(final Set<URL> urls, final String name) throws IOException {
        int i = 0;
        for (VestigeResourceLocator jarFile : jarFiles) {
            VestigeResource entry = jarFile.findResource(name);
            if (entry != null) {
                try {
                    urls.add(map(i, entry));
                } catch (MalformedURLException e) {
                    // ignore
                }
            }
            i++;
        }
        return null;
    }

    @Override
    protected URL findResource(final String name) {
        int match = resourceStringParser.match(name);
        if (match < 0) {
            return null;
        }
        VestigeClassLoaderConfiguration[] classLoaderConfigurations = vestigeClassLoaderConfigurationsList[match];
        if (classLoaderConfigurations != null) {
            return innerFindResource(name);
        }
        return null;
    }

    protected Enumeration<URL> findResources(final String name) throws IOException {
        int match = resourceStringParser.match(name);
        if (match < 0) {
            return EMPTY_URL_ENUMERATION;
        }
        VestigeClassLoaderConfiguration[] classLoaderConfigurations = vestigeClassLoaderConfigurationsList[match];
        if (classLoaderConfigurations != null) {
            Set<URL> urls = new LinkedHashSet<URL>();
            innerFindResources(urls, name);
            return Collections.enumeration(urls);
        }
        return EMPTY_URL_ENUMERATION;
    }

    protected URL superGetResource(final String name, final boolean parentSearched) {
        URL resource;
        if (parentSearched) {
            resource = parent.getResource(name);
            if (resource != null) {
                return resource;
            }
        }
        return findResource(name);
    }

    @Override
    public URL getResource(final String name) {
        int match = resourceStringParser.match(name);
        if (match < 0) {
            return null;
        }
        VestigeClassLoaderConfiguration[] classLoaderConfigurations = vestigeClassLoaderConfigurationsList[match];
        if (classLoaderConfigurations != null) {
            for (VestigeClassLoaderConfiguration classLoaderConfiguration : classLoaderConfigurations) {
                if (classLoaderConfiguration == null) {
                    URL resource = parent.getResource(name);
                    if (resource != null) {
                        return resource;
                    }
                } else {
                    boolean parentSearched = classLoaderConfiguration.isParentSearched();
                    VestigeClassLoader<?> vestigeClassLoader = classLoaderConfiguration.getVestigeClassLoader();
                    if (vestigeClassLoader == null) {
                        URL resource;
                        if (parentSearched) {
                            resource = parent.getResource(name);
                            if (resource != null) {
                                return resource;
                            }
                        }
                        resource = innerFindResource(name);
                        if (resource != null) {
                            return resource;
                        }
                    } else {
                        URL resource = vestigeClassLoader.superGetResource(name, parentSearched);
                        if (resource != null) {
                            return resource;
                        }
                    }
                }
            }
        }
        return null;
    }

    protected void superGetResources(final String name, final Set<URL> urls, final boolean parentSearched) throws IOException {
        if (parentSearched) {
            urls.addAll(Collections.list(parent.getResources(name)));
        }
        innerFindResources(urls, name);
    }

    @Override
    public Enumeration<URL> getResources(final String name) throws IOException {
        Set<URL> urls = new LinkedHashSet<URL>();
        VestigeClassLoaderConfiguration[] classLoaderConfigurations = vestigeClassLoaderConfigurationsList[resourceStringParser.match(name)];
        if (classLoaderConfigurations != null) {
            for (VestigeClassLoaderConfiguration classLoaderConfiguration : classLoaderConfigurations) {
                if (classLoaderConfiguration == null) {
                    urls.addAll(Collections.list(parent.getResources(name)));
                } else {
                    boolean parentSearched = classLoaderConfiguration.isParentSearched();
                    VestigeClassLoader<?> vestigeClassLoader = classLoaderConfiguration.getVestigeClassLoader();
                    if (vestigeClassLoader == null) {
                        if (parentSearched) {
                            urls.addAll(Collections.list(parent.getResources(name)));
                        }
                        innerFindResources(urls, name);
                    } else {
                        vestigeClassLoader.superGetResources(name, urls, parentSearched);
                    }
                }
            }
        }
        return Collections.enumeration(urls);
    }

    @Override
    public String toString() {
        if (data != null) {
            return data.toString();
        }
        return super.toString();
    }

    @Override
    public VestigeClassLoader<?> getVestigeClassLoader() {
        return this;
    }

    @Override
    public boolean isParentSearched() {
        return false;
    }

    public VestigeClassLoaderConfiguration getParentSeachedClassLoaderConfiguration() {
        return parentSearchedClassLoaderConfiguration;
    }

    private VestigeClassLoaderConfiguration parentSearchedClassLoaderConfiguration = new VestigeClassLoaderConfiguration() {

        @Override
        public boolean isParentSearched() {
            return true;
        }

        @Override
        public VestigeClassLoader<?> getVestigeClassLoader() {
            return VestigeClassLoader.this;
        }

        @Override
        public String toString() {
            return "ParentSearched-" + VestigeClassLoader.this.toString();
        }

    };

}
