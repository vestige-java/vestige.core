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

package fr.gaellalire.vestige.core.callable;

import java.util.concurrent.Callable;

/**
 * @author Gael Lalire
 */
public class ClassForName implements Callable<Class<?>> {

    private ClassLoader classLoader;

    private String className;

    public ClassForName(final ClassLoader classLoader, final String className) {
        this.classLoader = classLoader;
        this.className = className;
    }

    public Class<?> call() throws ClassNotFoundException {
        return Class.forName(className, true, classLoader);
    }

}
