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

package fr.gaellalire.vestige.core.resource;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipFile;

/**
 * @author Gael Lalire
 */
public class JarFileResourceLocator implements VestigeResourceLocator, PackageMetadata, Closeable {

    private static final Constructor<JarFile> RUNTIME_JAR_FILE_CONSTRUCTOR;

    private static final Object RUNTIME_VERSION;

    static {
        Constructor<JarFile> runtimeJarFileConstructor;
        Object runtimeVersion;
        try {
            Class<?> rvc = Class.forName("java.lang.Runtime$Version");
            runtimeJarFileConstructor = JarFile.class.getConstructor(File.class, boolean.class, int.class, rvc);
            runtimeVersion = JarFile.class.getMethod("runtimeVersion").invoke(null);
        } catch (Exception e) {
            // not in jdk 9
            runtimeJarFileConstructor = null;
            runtimeVersion = null;
        }
        RUNTIME_JAR_FILE_CONSTRUCTOR = runtimeJarFileConstructor;
        RUNTIME_VERSION = runtimeVersion;
    }

    private File file;

    private AtomicReference<JarFile> jarFileAtomicReference = new AtomicReference<JarFile>(null);

    private Manifest manifest;

    private String specTitle, specVersion, specVendor, implTitle, implVersion, implVendor;

    private boolean sealed;

    private URL codeSourceURL;

    public String getSpecTitle() {
        return specTitle;
    }

    public String getSpecVersion() {
        return specVersion;
    }

    public String getSpecVendor() {
        return specVendor;
    }

    public String getImplTitle() {
        return implTitle;
    }

    public String getImplVersion() {
        return implVersion;
    }

    public String getImplVendor() {
        return implVendor;
    }

    public boolean isSealed() {
        return sealed;
    }

    public JarFileResourceLocator(final File file) {
        this.file = file;
        try {
            codeSourceURL = file.toURI().toURL();
        } catch (MalformedURLException e) {
            // not possible
        }
    }

    public JarFileResourceLocator(final File file, final URL codeSourceURL) {
        this.file = file;
        this.codeSourceURL = codeSourceURL;
    }

    private JarFile openIfNot() throws IOException {
        JarFile jarFile = jarFileAtomicReference.get();
        if (jarFile == null) {
            if (RUNTIME_VERSION != null) {
                try {
                    jarFile = RUNTIME_JAR_FILE_CONSTRUCTOR.newInstance(file, true, ZipFile.OPEN_READ, RUNTIME_VERSION);
                } catch (Exception e) {
                    throw new IOException("Unable to create jar file", e);
                }
            } else {
                jarFile = new JarFile(file, true, ZipFile.OPEN_READ);
            }
            while (true) {
                // compare&exchange (but java 9)
                if (jarFileAtomicReference.compareAndSet(null, jarFile)) {
                    break;
                } else {
                    JarFile nJarFile = jarFileAtomicReference.get();
                    if (nJarFile == null) {
                        continue;
                    }
                    jarFile.close();
                    jarFile = nJarFile;
                    break;
                }
            }
        }
        manifest = jarFile.getManifest();

        Attributes attr = manifest.getMainAttributes();
        if (attr != null) {
            specTitle = attr.getValue(Attributes.Name.SPECIFICATION_TITLE);
            specVersion = attr.getValue(Attributes.Name.SPECIFICATION_VERSION);
            specVendor = attr.getValue(Attributes.Name.SPECIFICATION_VENDOR);
            implTitle = attr.getValue(Attributes.Name.IMPLEMENTATION_TITLE);
            implVersion = attr.getValue(Attributes.Name.IMPLEMENTATION_VERSION);
            implVendor = attr.getValue(Attributes.Name.IMPLEMENTATION_VENDOR);
            if ("true".equalsIgnoreCase(attr.getValue(Attributes.Name.SEALED))) {
                this.sealed = true;
            }
        }
        return jarFile;
    }

    @Override
    public VestigeResource findResource(final String resourceName) {
        JarFile jarFile;
        try {
            jarFile = openIfNot();
        } catch (IOException e) {
            return null;
        }
        JarEntry jarEntry = jarFile.getJarEntry(resourceName);
        if (jarEntry == null) {
            return null;
        }
        return new JarEntryResource(this, jarFile, jarEntry, codeSourceURL);
    }

    @Override
    public PackageMetadata getPackageMetadata(final String pn) {
        if (manifest == null) {
            return this;
        }
        final Attributes attr = manifest.getAttributes(pn.replace('.', '/').concat("/"));
        if (attr == null) {
            return this;
        }
        return new PackageMetadata() {

            @Override
            public boolean isSealed() {
                String value = attr.getValue(Attributes.Name.SEALED);
                if (value == null) {
                    return JarFileResourceLocator.this.sealed;
                }
                if ("true".equalsIgnoreCase(value)) {
                    return true;
                }
                return false;
            }

            @Override
            public String getSpecVersion() {
                String value = attr.getValue(Attributes.Name.SPECIFICATION_VERSION);
                if (value == null) {
                    return JarFileResourceLocator.this.specVersion;
                }
                return value;
            }

            @Override
            public String getSpecVendor() {
                String value = attr.getValue(Attributes.Name.SPECIFICATION_VENDOR);
                if (value == null) {
                    return JarFileResourceLocator.this.specVendor;
                }
                return value;
            }

            @Override
            public String getSpecTitle() {
                String value = attr.getValue(Attributes.Name.SPECIFICATION_TITLE);
                if (value == null) {
                    return JarFileResourceLocator.this.specTitle;
                }
                return value;
            }

            @Override
            public String getImplVersion() {
                String value = attr.getValue(Attributes.Name.IMPLEMENTATION_VERSION);
                if (value == null) {
                    return JarFileResourceLocator.this.implVersion;
                }
                return value;
            }

            @Override
            public String getImplVendor() {
                String value = attr.getValue(Attributes.Name.IMPLEMENTATION_VENDOR);
                if (value == null) {
                    return JarFileResourceLocator.this.implVendor;
                }
                return value;
            }

            @Override
            public String getImplTitle() {
                String value = attr.getValue(Attributes.Name.IMPLEMENTATION_TITLE);
                if (value == null) {
                    return JarFileResourceLocator.this.implTitle;
                }
                return value;
            }
        };
    }

    @Override
    public boolean isPackageSealed(final String packageName) {
        String path = packageName.replace('.', '/').concat("/");
        Attributes attr = manifest.getAttributes(path);
        String sealed = null;
        if (attr != null) {
            sealed = attr.getValue(Attributes.Name.SEALED);
        }
        if (sealed == null) {
            return this.sealed;
        }
        return "true".equalsIgnoreCase(sealed);
    }

    @Override
    public String toString() {
        return codeSourceURL.toExternalForm();
    }

    public void close() throws IOException {
        JarFile jarFile = jarFileAtomicReference.getAndSet(null);
        if (jarFile != null) {
            jarFile.close();
        }
    }

    public InputStream getInputStream(final JarEntryResource jarEntryResource, final JarFile entryJarFile) throws IOException {
        JarFile jarFile = openIfNot();
        JarEntry jarEntry = jarEntryResource.getJarEntry();
        if (entryJarFile != jarFile) {
            // not cool ...
            jarEntry = jarFile.getJarEntry(jarEntryResource.getName());
            jarEntryResource.setJarEntry(jarEntry);
        }
        return jarFile.getInputStream(jarEntry);
    }

}
