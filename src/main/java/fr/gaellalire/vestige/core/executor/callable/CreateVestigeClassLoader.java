/*
 * Copyright 2013 The Apache Software Foundation.
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

package fr.gaellalire.vestige.core.executor.callable;

import java.util.concurrent.Callable;

import fr.gaellalire.vestige.core.ModuleEncapsulationEnforcer;
import fr.gaellalire.vestige.core.VestigeClassLoader;
import fr.gaellalire.vestige.core.VestigeClassLoaderConfiguration;
import fr.gaellalire.vestige.core.parser.StringParser;
import fr.gaellalire.vestige.core.resource.VestigeResourceLocator;

/**
 * @author Gael Lalire
 */
public class CreateVestigeClassLoader<E> implements Callable<VestigeClassLoader<E>> {

    private ClassLoader parent;

    private VestigeResourceLocator[] urls;

    private VestigeClassLoaderConfiguration[][] vestigeClassLoaderConfigurationsList;

    private StringParser classStringParser;

    private StringParser resourceStringParser;

    private ModuleEncapsulationEnforcer moduleResourceStringParser;

    public CreateVestigeClassLoader(final ClassLoader parent, final VestigeClassLoaderConfiguration[][] vestigeClassLoaderConfigurationsList, final StringParser classStringParser,
            final StringParser resourceStringParser, final ModuleEncapsulationEnforcer moduleEncapsulationEnforcer, final VestigeResourceLocator[] urls) {
        this.parent = parent;
        this.urls = urls;
        this.vestigeClassLoaderConfigurationsList = vestigeClassLoaderConfigurationsList;
        this.classStringParser = classStringParser;
        this.resourceStringParser = resourceStringParser;
        this.moduleResourceStringParser = moduleEncapsulationEnforcer;
    }

    public VestigeClassLoader<E> call() {
        return new VestigeClassLoader<E>(parent, vestigeClassLoaderConfigurationsList, classStringParser, resourceStringParser, moduleResourceStringParser, urls);
    }

}
