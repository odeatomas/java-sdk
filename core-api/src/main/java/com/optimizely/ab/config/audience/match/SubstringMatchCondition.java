/**
 * Copyright 2018-2019, Optimizely and contributors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.optimizely.ab.config.audience.match;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Matches {@link String} input that contains a {@link CharSequence} value.
 */
public class SubstringMatchCondition extends MatchCondition<CharSequence, String> {
    private static final Logger logger = LoggerFactory.getLogger(SubstringMatchCondition.class);

    public SubstringMatchCondition(@Nonnull String name, @Nonnull CharSequence value) {
        super(name, value);
    }

    @Nullable
    @Override
    String convert(Object inputRaw) {
        if (inputRaw instanceof CharSequence) {
            return inputRaw.toString();
        }

        logger.warn(
            "Unable to evaluate condition for \"{}\" attribute because unable to convert attribute type of \"{}\" to \"{}\"",
            name,
            inputRaw.getClass().getName(),
            String.class.getName());

        return null;
    }

    @Override
    @Nullable
    public Boolean matches(String input) {
        return input.contains(value);
    }
}

