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

import java.util.Map;

import fr.gaellalire.vestige.core.parser.StringParser;

/**
 * @author Gael Lalire
 */
public final class ModuleEncapsulationEnforcer {

    private Map<String, String> moduleNameByPackageName;

    private StringParser moduleNameStringParser;

    private StringParser[] resourceLocatorInModuleStringParsers;

    public ModuleEncapsulationEnforcer(final Map<String, String> moduleNameByPackageName, final StringParser moduleNameStringParser,
            final StringParser[] resourceLocatorInModuleStringParsers) {
        this.moduleNameByPackageName = moduleNameByPackageName;
        this.moduleNameStringParser = moduleNameStringParser;
        this.resourceLocatorInModuleStringParsers = resourceLocatorInModuleStringParsers;
    }

    public String getModuleName(final String packageName) {
        if (packageName == null) {
            return null;
        }
        return moduleNameByPackageName.get(packageName);
    }

    public int findLocatorIndex(final String moduleName, final String name) {
        String resourceModuleName = getModuleName(VestigeClassLoader.getPackageNameFromResourceName(name));
        if (resourceModuleName != null && !moduleName.equals(resourceModuleName)) {
            return -2;
        }
        if (moduleNameStringParser == null) {
            return -1;
        }
        int match = moduleNameStringParser.match(moduleName);
        if (match < 0) {
            return match;
        }
        if (resourceLocatorInModuleStringParsers != null) {
            match = resourceLocatorInModuleStringParsers[match].match(name);
        }
        return match;
    }

}
