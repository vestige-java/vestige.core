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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.CodeSigner;

/**
 * @author Gael Lalire
 */
public class FileResource implements VestigeResource {

    private URL baseURL;

    private File file;

    public FileResource(final URL baseURL, final File file) {
        this.file = file;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new FileInputStream(file);
    }

    @Override
    public long getSize() {
        return file.length();
    }

    @Override
    public String getName() {
        return file.getName();
    }

    @Override
    public URL getCodeSourceURL() {
        return baseURL;
    }

    @Override
    public CodeSigner[] getCodeSigners() {
        return null;
    }

}
