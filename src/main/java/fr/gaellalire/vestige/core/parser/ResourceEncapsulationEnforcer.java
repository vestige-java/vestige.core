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

package fr.gaellalire.vestige.core.parser;

import java.util.Set;

import fr.gaellalire.vestige.core.VestigeClassLoader;

/**
 * @author Gael Lalire
 */
public final class ResourceEncapsulationEnforcer implements StringParser {

    private int enforceValue;

    private StringParser delegate;

    private Set<String> encapsulatedPackageNames;

    public ResourceEncapsulationEnforcer(final StringParser delegate, final Set<String> encapsulatedPackageNames, final int enforceValue) {
        this.delegate = delegate;
        this.encapsulatedPackageNames = encapsulatedPackageNames;
        this.enforceValue = enforceValue;
    }

    @Override
    public int match(final CharSequence sequence) {
        String resourceName = sequence.toString();
        if (resourceName.endsWith(".class")) {
            // .class are never encapsulated
            return delegate.match(sequence);
        }
        String packageName = VestigeClassLoader.getPackageNameFromResourceName(resourceName);
        if (packageName != null && encapsulatedPackageNames.contains(packageName)) {
            return enforceValue;
        }
        // not in a package
        return delegate.match(sequence);
    }

}
