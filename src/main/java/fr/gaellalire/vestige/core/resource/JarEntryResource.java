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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.CodeSigner;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * @author Gael Lalire
 */
public class JarEntryResource implements VestigeResource {

    private String name;

    private JarFileResourceLocator jarFileResourceLocator;

    private JarFile jarFile;

    private JarEntry jarEntry;

    private URL codeSourceURL;

    public JarEntryResource(final JarFileResourceLocator jarFileResourceLocator, final JarFile jarFile, final JarEntry jarEntry, final URL codeSourceURL) {
        this.jarFileResourceLocator = jarFileResourceLocator;
        this.jarFile = jarFile;
        this.name = jarEntry.getName();
        this.jarEntry = jarEntry;
        this.codeSourceURL = codeSourceURL;
    }

    public void setJarFile(final JarFile jarFile) {
        this.jarFile = jarFile;
    }

    public JarEntry getJarEntry() {
        return jarEntry;
    }

    public void setJarEntry(final JarEntry jarEntry) {
        this.jarEntry = jarEntry;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return jarFileResourceLocator.getInputStream(this, jarFile);
    }

    @Override
    public long getSize() {
        return jarEntry.getSize();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public URL getCodeSourceURL() {
        return codeSourceURL;
    }

    @Override
    public String toString() {
        return "jar:" + codeSourceURL + "!/" + jarEntry.getName();
    }

    @Override
    public CodeSigner[] getCodeSigners() {
        return jarEntry.getCodeSigners();
    }

}
