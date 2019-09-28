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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLStreamHandler;
import java.nio.charset.Charset;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.gaellalire.vestige.core.resource.VestigeResource;
import fr.gaellalire.vestige.core.resource.VestigeResourceLocator;

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

    private static final Pattern URL_PATH_PATTERN = Pattern.compile("/(\\d+)/(\\d+)!/(.*)");

    @Override
    protected void parseURL(final URL u, final String spec, final int start, final int limit) {
        super.parseURL(u, spec, start, limit);
        String authority = u.getAuthority();
        if (authority != null && authority.length() != 0) {
            new IllegalArgumentException("vrt does not support authority part (//[userinfo@]host[:port])");
        }
        if (u.getQuery() != null) {
            new IllegalArgumentException("vrt does not support query part (?query)");
        }
        if (u.getRef() != null) {
            new IllegalArgumentException("vrt does not support fragment part (#fragment)");
        }
        Matcher matcher = URL_PATH_PATTERN.matcher(u.getPath());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("vrt path must starts with /[integer]/[integer]!/[entry-name]");
        }
        try {
            URLDecoder.decode(matcher.group(3), Charset.defaultCharset().name());
        } catch (Exception e) {
            new IllegalArgumentException("vrt entry name in path is illegal", e);
        }
    }

    @Override
    protected URLConnection openConnection(final URL url) throws IOException {
        final String path = url.getPath();
        Matcher matcher = URL_PATH_PATTERN.matcher(path);
        if (!matcher.matches()) {
            throw new IOException("Invalid vrt URL (" + path + ")");
        }
        int gpIndex = 1;
        final int classLoaderIndex = Integer.parseInt(matcher.group(gpIndex++));
        final int locatorIndex = Integer.parseInt(matcher.group(gpIndex++));
        final String entryName = URLDecoder.decode(matcher.group(gpIndex), Charset.defaultCharset().name());

        return new URLConnection(url) {

            private boolean connected;

            private VestigeResourceLocator vestigeResourceLocator;

            private VestigeResource entry;

            @Override
            public void connect() throws IOException {
                WeakReference<VestigeClassLoader<?>> weakReference;
                synchronized (urlReferencedVestigeClassLoader) {
                    if (connected) {
                        return;
                    }
                    weakReference = urlReferencedVestigeClassLoader.get(classLoaderIndex);
                }
                if (weakReference == null) {
                    throw new IOException("ClassLoader of URL does not exists (" + path + ")");
                }
                VestigeClassLoader<?> classLoader = weakReference.get();
                if (classLoader == null) {
                    throw new IOException("ClassLoader of URL does not exists anymore (" + path + ")");
                }

                vestigeResourceLocator = classLoader.getVestigeResourceLocator(locatorIndex);

                if (vestigeResourceLocator == null) {
                    entry = null;
                } else {
                    entry = vestigeResourceLocator.findResource(entryName);
                }
                synchronized (urlReferencedVestigeClassLoader) {
                    connected = true;
                }
            }

            @Override
            public InputStream getInputStream() throws IOException {
                if (!connected) {
                    connect();
                }
                if (vestigeResourceLocator == null) {
                    throw new FileNotFoundException("JAR not found at locator " + locatorIndex + " of classloader " + classLoaderIndex);
                }
                if (entry == null) {
                    throw new FileNotFoundException("JAR entry " + entryName + " not found in locator " + locatorIndex + " of classloader " + classLoaderIndex);
                }
                return entry.getInputStream();
            }

            @Override
            public int getContentLength() {
                return (int) getContentLengthLong();
            }

            public long getContentLengthLong() {
                if (!connected) {
                    try {
                        connect();
                    } catch (IOException e) {
                        // ignore
                    }
                }
                if (entry == null) {
                    return -1;
                }
                return entry.getSize();
            }
        };
    }

}
