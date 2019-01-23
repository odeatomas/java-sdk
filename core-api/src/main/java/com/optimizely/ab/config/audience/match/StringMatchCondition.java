/**
 *
 *    Copyright 2018-2019, Optimizely and contributors
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.optimizely.ab.config.audience.match;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Matches content of {@link String} against any {@link CharSequence} input.
 */
public class StringMatchCondition extends MatchCondition<String, CharSequence> {
    private static final Logger logger = LoggerFactory.getLogger(StringMatchCondition.class);

    public StringMatchCondition(@Nonnull String name, @Nonnull String value) {
        super(name, value);
    }

    @Nullable
    @Override
    CharSequence convert(Object inputRaw) {
        if (inputRaw instanceof CharSequence) {
            return (CharSequence) inputRaw;
        }

        return null;
    }

    @Nullable
    public Boolean matches(CharSequence input) {
        return value.contentEquals(input);
    }
}
