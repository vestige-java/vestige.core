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

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.gaellalire.vestige.core.resource.VestigeResource;

/**
 * @author Gael Lalire
 */
public final class VestigeCoreURLStreamHandler extends URLStreamHandler {

    protected VestigeCoreURLStreamHandler() {
    }

    public static final String PROTOCOL = "vrt";

    private List<WeakReference<VestigeClassLoader<?>>> urlReferencedVestigeClassLoader = new ArrayList<WeakReference<VestigeClassLoader<?>>>();

    protected void createIndex(final VestigeClassLoader<?> vestigeClassLoader) {
        synchronized (urlReferencedVestigeClassLoader) {
            int urlIndex = vestigeClassLoader.getUrlIndex();
            if (urlIndex != -1) {
                return;
            }
            int i = 0;
            for (WeakReference<VestigeClassLoader<?>> weakReference : urlReferencedVestigeClassLoader) {
                if (weakReference.get() == null) {
                    urlIndex = i;
                    break;
                }
                i++;
            }
            if (urlIndex == -1) {
                urlIndex = i;
                urlReferencedVestigeClassLoader.add(new WeakReference<VestigeClassLoader<?>>(vestigeClassLoader));
            } else {
                urlReferencedVestigeClassLoader.set(urlIndex, new WeakReference<VestigeClassLoader<?>>(vestigeClassLoader));
            }
            vestigeClassLoader.setUrlIndex(urlIndex);
        }
    }

    protected URL map(final int urlIndex, final int locatorIndex, final VestigeResource resource) throws IOException {
        // associate a custom stream handler to avoid a new findResource when reading
        final URLStreamHandler urlStreamHandler = new URLStreamHandler() {

            @Override
            protected URLConnection openConnection(final URL url) throws IOException {
                return new URLConnection(url) {
                    @Override
                    public void connect() throws IOException {
                    }

                    @Override
                    public InputStream getInputStream() throws IOException {
                        return resource.getInputStream();
                    }

                    @Override
                    public int getContentLength() {
                        return (int) getContentLengthLong();
                    }

                    public long getContentLengthLong() {
                        return resource.getSize();
                    }
                };
            }
        };
        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<URL>() {

                @Override
                public URL run() throws Exception {
                    return new URL(PROTOCOL, null, -1, "/" + urlIndex + "/" + locatorIndex + "!/" + resource.getName(), urlStreamHandler);
                }
            });
        } catch (PrivilegedActionException e) {
            throw new IOException(e);
        }
    }

    private static final Pattern URL_FILE_PATTERN = Pattern.compile("/(\\d+)/(\\d+)!/(.*)");

    @Override
    protected URLConnection openConnection(final URL url) throws IOException {
        String file = url.getFile();
        Matcher matcher = URL_FILE_PATTERN.matcher(file);
        if (!matcher.matches()) {
            throw new IOException("Invalid vrt URL (" + file + ")");
        }
        int gpIndex = 1;
        int urlIndex = Integer.parseInt(matcher.group(gpIndex++));
        int parseInt = Integer.parseInt(matcher.group(gpIndex++));
        VestigeClassLoader<?> classLoader = urlReferencedVestigeClassLoader.get(urlIndex).get();
        if (classLoader == null) {
            throw new IOException("ClassLoader of URL does not exists anymore (" + file + ")");
        }

        final VestigeResource entry = classLoader.getVestigeResourceLocator(parseInt).findResource(matcher.group(gpIndex));

        return new URLConnection(url) {
            @Override
            public void connect() throws IOException {
            }

            @Override
            public InputStream getInputStream() throws IOException {
                return entry.getInputStream();
            }

            @Override
            public int getContentLength() {
                return (int) getContentLengthLong();
            }

            public long getContentLengthLong() {
                return entry.getSize();
            }
        };
    }

}
