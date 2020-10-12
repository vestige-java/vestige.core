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

import java.util.regex.Pattern;

/**
 * @author Gael Lalire
 */
public class PatternStringParser implements StringParser {

    private static final long serialVersionUID = -8603419945790653901L;

    private Pattern pattern;

    private int matchValue;

    private int unmatchValue;

    public PatternStringParser(final Pattern pattern, final int matchValue, final int unmatchValue) {
        this.pattern = pattern;
        this.matchValue = matchValue;
        this.unmatchValue = unmatchValue;
    }

    @Override
    public int match(final CharSequence sequence) {
        if (pattern.matcher(sequence).matches()) {
            return matchValue;
        }
        return unmatchValue;
    }

}
