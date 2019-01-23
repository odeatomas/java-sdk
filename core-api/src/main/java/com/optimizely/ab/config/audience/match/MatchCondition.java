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

import com.optimizely.ab.config.ProjectConfig;
import com.optimizely.ab.config.audience.Condition;
import com.optimizely.ab.config.audience.UserAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

/**
 * Base class for matching operators.
 *
 * Simplifies the process of evaluating a match against arbitrary {@link Object} input value
 * by decomposing into sequence of steps.
 *
 * 1.  {@link #extract(Map)}
 * 2.  {@link #convert(Object)}
 * 3.  {@link #matches(Object)}
 *
 * @param <T> type of value that input is matched against
 * @param <U> type of converted input that is required to match
 */
abstract public class MatchCondition<T, U> implements Condition<T> {
    private static final Logger logger = LoggerFactory.getLogger(UserAttribute.class);

    protected final String name;
    protected final T value;

    MatchCondition(@Nonnull String name, @Nonnull T value) {
        this.name = name;
        this.value = value;
    }

    Object extract(Map<String, ?> attributes) {
        return attributes != null ? attributes.get(name) : null;
    }

    /**
     * Converts arbitrary {@link Object} associated to attribute being matched
     * into a type that can
     *
     * @param inputRaw object associated to attribute
     * @return a non-null value to evaluate as a match. if null is returned, the input is considered incompatible.
     */
    @Nullable
    abstract U convert(Object inputRaw);

    @Nullable
    abstract Boolean matches(U input);

    @Nullable
    @Override
    public final Boolean evaluate(ProjectConfig config, Map<String, ?> attributes) {
        Object input = extract(attributes);
        if (input == null) {
            logger.info("Unable to evaluate condition for \"{}\" attribute because no value was given", name);
            return null;
        }

        try {
            U converted = convert(input);
            if (converted == null) {
                return null;
            }

            return matches(converted);
        } catch (Exception e) {
            logger.error("Unable to evaluate condition for \"{}\" attribute", name, e);
            return null;
        }
    }

    public String getName() {
        return name;
    }

    public T getValue() {
        return value;
    }
}
