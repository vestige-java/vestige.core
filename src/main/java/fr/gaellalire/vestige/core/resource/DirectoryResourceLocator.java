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

package fr.gaellalire.vestige.core.resource;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author Gael Lalire
 */
public class DirectoryResourceLocator implements VestigeResourceLocator, PackageMetadata {

    private File directory;

    private URL baseURL;

    public DirectoryResourceLocator(final File directory) {
        this.directory = directory;
        try {
            baseURL = directory.toURI().toURL();
        } catch (MalformedURLException e) {
            // not possible
        }
    }

    @Override
    public void close() throws IOException {
    }

    @Override
    public VestigeResource findResource(final String resourceName) {
        final File file = new File(directory, resourceName);
        if (!file.isFile()) {
            return null;
        }
        return new FileResource(baseURL, file);
    }

    @Override
    public PackageMetadata getPackageMetadata(final String packageName) {
        return this;
    }

    @Override
    public boolean isPackageSealed(final String packageName) {
        return false;
    }

    @Override
    public String getSpecTitle() {
        return null;
    }

    @Override
    public String getSpecVersion() {
        return null;
    }

    @Override
    public String getSpecVendor() {
        return null;
    }

    @Override
    public String getImplTitle() {
        return null;
    }

    @Override
    public String getImplVersion() {
        return null;
    }

    @Override
    public String getImplVendor() {
        return null;
    }

    @Override
    public boolean isSealed() {
        return false;
    }

}
