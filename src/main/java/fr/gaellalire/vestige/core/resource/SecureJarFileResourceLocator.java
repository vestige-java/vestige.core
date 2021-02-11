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
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * @author Gael Lalire
 */
public class SecureJarFileResourceLocator implements VestigeResourceLocator, PackageMetadata, Closeable {

    private static final String MULTI_RELEASE = "Multi-Release";

    private static final String META_INF = "META-INF/";

    private static final String META_INF_VERSIONS = META_INF + "versions/";

    private SecureJarFile secureJarFile;

    private Manifest manifest;

    private String specTitle, specVersion, specVendor, implTitle, implVersion, implVendor;

    private boolean sealed;

    private URL codeSourceURL;

    private static final int VERSION;

    static {
        int version;
        try {
            Method method = Runtime.class.getMethod("version");
            Object versionObject = method.invoke(null);
            version = (Integer) versionObject.getClass().getMethod("feature").invoke(versionObject);
        } catch (Exception e) {
            version = -1;
        }
        VERSION = version;
    }

    private Map<String, SecureJarEntryResource> jarEntries = new HashMap<String, SecureJarEntryResource>();

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

    public SecureJarFileResourceLocator(final SecureJarFile secureJarFile) {
        this.secureJarFile = secureJarFile;
        try {
            codeSourceURL = secureJarFile.getFile().toURI().toURL();
        } catch (MalformedURLException e) {
            // not possible
        }
    }

    public SecureJarFileResourceLocator(final SecureJarFile secureJarFile, final URL codeSourceURL) {
        this.secureJarFile = secureJarFile;
        this.codeSourceURL = codeSourceURL;
    }

    private void openIfNot() throws IOException {
        int position = 0;

        ZipInputStream zipInputStream = new ZipInputStream(secureJarFile.getInputStream());
        ZipEntry nextZipEntry = zipInputStream.getNextEntry();
        if (nextZipEntry != null && nextZipEntry.getName().equalsIgnoreCase(META_INF)) {
            nextZipEntry = zipInputStream.getNextEntry();
            position++;
        }
        if (nextZipEntry != null && nextZipEntry.getName().equalsIgnoreCase(JarFile.MANIFEST_NAME)) {
            jarEntries.put(nextZipEntry.getName(), new SecureJarEntryResource(this, position, new JarEntry(nextZipEntry), nextZipEntry.getName(), codeSourceURL));
            position++;
        }

        Map<String, Integer> versionByName = new HashMap<String, Integer>();
        JarInputStream jarInputStream = new JarInputStream(secureJarFile.getInputStream());
        manifest = jarInputStream.getManifest();
        boolean multiReleaseJar = Boolean.parseBoolean(manifest.getMainAttributes().getValue(MULTI_RELEASE));
        JarEntry nextJarEntry = jarInputStream.getNextJarEntry();
        while (nextJarEntry != null) {
            String name = nextJarEntry.getName();
            if (versionByName.get(name) == null) {
                jarEntries.put(name, new SecureJarEntryResource(this, position, nextJarEntry, name, codeSourceURL));
            }
            if (multiReleaseJar && VERSION != -1 && !nextJarEntry.isDirectory() && name.startsWith(META_INF_VERSIONS)) {
                int sep = name.indexOf('/', META_INF_VERSIONS.length() + 1);
                if (sep != -1) {
                    try {
                        int localVersion = Integer.parseInt(name.substring(META_INF_VERSIONS.length(), sep));
                        if (localVersion <= VERSION) {
                            Integer currentVersion = versionByName.get(name);
                            if (currentVersion == null || localVersion > currentVersion) {
                                name = name.substring(sep + 1);
                                versionByName.put(name, localVersion);
                                jarEntries.put(name, new SecureJarEntryResource(this, position, nextJarEntry, name, codeSourceURL));
                            }
                        }
                    } catch (NumberFormatException e) {
                    }
                }
            }
            jarInputStream.closeEntry();
            nextJarEntry = jarInputStream.getNextJarEntry();
            position++;
        }
        jarInputStream.close();

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
    }

    @Override
    public VestigeResource findResource(final String resourceName) {
        try {
            openIfNot();
        } catch (IOException e) {
            return null;
        }
        return jarEntries.get(resourceName);
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
                    return SecureJarFileResourceLocator.this.sealed;
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
                    return SecureJarFileResourceLocator.this.specVersion;
                }
                return value;
            }

            @Override
            public String getSpecVendor() {
                String value = attr.getValue(Attributes.Name.SPECIFICATION_VENDOR);
                if (value == null) {
                    return SecureJarFileResourceLocator.this.specVendor;
                }
                return value;
            }

            @Override
            public String getSpecTitle() {
                String value = attr.getValue(Attributes.Name.SPECIFICATION_TITLE);
                if (value == null) {
                    return SecureJarFileResourceLocator.this.specTitle;
                }
                return value;
            }

            @Override
            public String getImplVersion() {
                String value = attr.getValue(Attributes.Name.IMPLEMENTATION_VERSION);
                if (value == null) {
                    return SecureJarFileResourceLocator.this.implVersion;
                }
                return value;
            }

            @Override
            public String getImplVendor() {
                String value = attr.getValue(Attributes.Name.IMPLEMENTATION_VENDOR);
                if (value == null) {
                    return SecureJarFileResourceLocator.this.implVendor;
                }
                return value;
            }

            @Override
            public String getImplTitle() {
                String value = attr.getValue(Attributes.Name.IMPLEMENTATION_TITLE);
                if (value == null) {
                    return SecureJarFileResourceLocator.this.implTitle;
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
        secureJarFile.close();
    }

    public InputStream getInputStream(final SecureJarEntryResource jarEntryResource) throws IOException {
        ZipInputStream jarInputStream = new ZipInputStream(secureJarFile.getInputStream());
        int position = jarEntryResource.getPosition();
        jarInputStream.getNextEntry();
        while (position != 0) {
            jarInputStream.closeEntry();
            jarInputStream.getNextEntry();
            position--;
        }
        return jarInputStream;
    }

}
