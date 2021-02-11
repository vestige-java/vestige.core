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
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;

/**
 * This class lock the file, so {@link #getInputStream()} will always return the same data allowing third party to verify its signature.
 * @author Gael Lalire
 */
public class SecureJarFile {

    public static enum Mode {

        PRIVATE_MAP,

        /**
         * Not secure on OS with advisory lock
         */
        FILE_LOCK;
    }

    private File file;

    private RandomAccessFile randomAccessFile;

    private long position = 0;

    private Object mutex = new Object();

    private MappedByteBuffer map;

    public SecureJarFile(final File file, final Mode mode) throws IOException {
        this.file = file;
        switch (mode) {
        case FILE_LOCK:
            randomAccessFile = new RandomAccessFile(file, "r");
            randomAccessFile.getChannel().lock(0, Long.MAX_VALUE, true);
            break;
        case PRIVATE_MAP:
            randomAccessFile = new RandomAccessFile(file, "rw");
            FileChannel channel = randomAccessFile.getChannel();
            map = channel.map(MapMode.PRIVATE, 0, channel.size());
            break;
        default:
            throw new IOException("Unknown mode " + mode);
        }
    }

    public File getFile() {
        return file;
    }

    public InputStream getInputStream() {
        if (map == null) {
            return new InputStream() {

                private long localPosition = 0;

                @Override
                public int read(final byte[] b, final int off, final int len) throws IOException {
                    int read;
                    synchronized (mutex) {
                        if (position != localPosition) {
                            randomAccessFile.seek(localPosition);
                            position = localPosition;
                        }
                        read = randomAccessFile.read(b, off, len);
                        if (read != -1) {
                            localPosition += read;
                            position = localPosition;
                        }
                    }
                    return read;
                }

                @Override
                public int read() throws IOException {
                    int read;
                    synchronized (mutex) {
                        if (position != localPosition) {
                            randomAccessFile.seek(localPosition);
                            position = localPosition;
                        }
                        read = randomAccessFile.read();
                        if (read != -1) {
                            localPosition += 1;
                            position = localPosition;
                        }
                    }
                    return read;
                }
            };
        }

        return new InputStream() {

            private int localPosition = 0;

            @Override
            public int read(final byte[] b, final int off, final int len) throws IOException {
                int read = len;
                synchronized (mutex) {
                    if (position != localPosition) {
                        map.position(localPosition);
                        position = localPosition;
                    }
                    int remaining = map.remaining();
                    if (remaining == 0) {
                        return -1;
                    } else if (len > remaining) {
                        read = remaining;
                    }
                    map.get(b, off, read);
                    localPosition += read;
                    position = localPosition;
                }
                return read;
            }

            @Override
            public int read() throws IOException {
                byte read;
                synchronized (mutex) {
                    if (position != localPosition) {
                        map.position(localPosition);
                        position = localPosition;
                    }
                    int remaining = map.remaining();
                    if (remaining == 0) {
                        return -1;
                    }
                    read = map.get();
                    localPosition += 1;
                    position = localPosition;
                }
                return read & 0xFF;
            }
        };
    }

    public void close() throws IOException {
        randomAccessFile.close();
    }

}
