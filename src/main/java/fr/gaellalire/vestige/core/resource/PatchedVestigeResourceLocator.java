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

import java.io.IOException;

/**
 * @author Gael Lalire
 */
public class PatchedVestigeResourceLocator implements VestigeResourceLocator {

    private VestigeResourceLocator original;

    private VestigeResourceLocator patch;

    private boolean metadataPatched;

    public PatchedVestigeResourceLocator(final VestigeResourceLocator original, final VestigeResourceLocator patch, final boolean metadataPatched) {
        this.original = original;
        this.patch = patch;
        this.metadataPatched = metadataPatched;
    }

    @Override
    public void close() throws IOException {
        try {
            patch.close();
        } finally {
            original.close();
        }
    }

    @Override
    public VestigeResource findResource(final String resourceName) {
        VestigeResource vestigeResource = patch.findResource(resourceName);
        if (vestigeResource != null) {
            return vestigeResource;
        }
        return original.findResource(resourceName);
    }

    @Override
    public PackageMetadata getPackageMetadata(final String packageName) {
        if (metadataPatched) {
            return patch.getPackageMetadata(packageName);
        }
        return original.getPackageMetadata(packageName);
    }

    @Override
    public boolean isPackageSealed(final String packageName) {
        if (metadataPatched) {
            return patch.isPackageSealed(packageName);
        }
        return original.isPackageSealed(packageName);
    }

}
