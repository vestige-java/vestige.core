/*
 * Copyright 2021 The Apache Software Foundation.
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
import java.nio.ByteBuffer;

/**
 * @author Gael Lalire
 */
public abstract class SeekableInputStream extends InputStream {

    public abstract void seek(long position) throws IOException;

    public abstract long getPosition() throws IOException;

    public abstract int read(ByteBuffer byteBuffer) throws IOException;

    public abstract long size() throws IOException;

}
