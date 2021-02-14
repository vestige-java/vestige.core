/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package fr.gaellalire.vestige.core.zip;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Re-implements {@link FilterInputStream#close()} to do nothing.
 * @since 1.14
 */
public class CloseShieldFilterInputStream extends FilterInputStream {

    public CloseShieldFilterInputStream(final InputStream in) {
        super(in);
    }

    @Override
    public void close() throws IOException {
        // NO IMPLEMENTATION.
    }

}
