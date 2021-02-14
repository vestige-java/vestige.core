/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package fr.gaellalire.vestige.core.zip;

import static fr.gaellalire.vestige.core.zip.IOUtils.closeQuietly;

import java.io.IOException;
import java.io.InputStream;

/**
 * Deflate64 decompressor.
 * @since 1.16
 * @NotThreadSafe
 */
public class Deflate64CompressorInputStream extends CompressorInputStream implements InputStreamStatistics {
    private InputStream originalStream;

    private HuffmanDecoder decoder;

    private long compressedBytesRead;

    private final byte[] oneByte = new byte[1];

    /**
     * Constructs a Deflate64CompressorInputStream.
     * @param in the stream to read from
     */
    public Deflate64CompressorInputStream(final InputStream in) {
        this(new HuffmanDecoder(in));
        originalStream = in;
    }

    Deflate64CompressorInputStream(final HuffmanDecoder decoder) {
        this.decoder = decoder;
    }

    /**
     * @throws java.io.EOFException if the underlying stream is exhausted before the end of defalted data was reached.
     */
    @Override
    public int read() throws IOException {
        while (true) {
            int r = read(oneByte);
            switch (r) {
            case 1:
                return oneByte[0] & 0xFF;
            case -1:
                return -1;
            case 0:
                continue;
            default:
                throw new IllegalStateException("Invalid return value from read: " + r);
            }
        }
    }

    /**
     * @throws java.io.EOFException if the underlying stream is exhausted before the end of deflated data was reached.
     */
    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        if (len == 0) {
            return 0;
        }
        int read = -1;
        if (decoder != null) {
            read = decoder.decode(b, off, len);
            compressedBytesRead = decoder.getBytesRead();
            count(read);
            if (read == -1) {
                closeDecoder();
            }
        }
        return read;
    }

    @Override
    public int available() throws IOException {
        return decoder != null ? decoder.available() : 0;
    }

    @Override
    public void close() throws IOException {
        try {
            closeDecoder();
        } finally {
            if (originalStream != null) {
                originalStream.close();
                originalStream = null;
            }
        }
    }

    /**
     * @since 1.17
     */
    @Override
    public long getCompressedCount() {
        return compressedBytesRead;
    }

    private void closeDecoder() {
        closeQuietly(decoder);
        decoder = null;
    }
}
