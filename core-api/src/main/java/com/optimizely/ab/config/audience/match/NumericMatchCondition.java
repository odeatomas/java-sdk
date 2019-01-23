/**
 * Copyright 2019, Optimizely and contributors
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

public class NumericMatchCondition extends MatchCondition<Number, Number> {
    public enum Operator {
        LESS_THAN(-1),
        EQUAL_TO(0),
        GREATER_THAN(1);

        final int sign;

        Operator(int sign) {
            this.sign = sign;
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(NumericMatchCondition.class);

    private final Operator operator;

    public NumericMatchCondition(@Nonnull String name, @Nonnull Number value, @Nonnull Operator operator) {
        super(name, value);
        this.operator = operator;
    }

    @Nullable
    @Override
    Number convert(Object inputRaw) {
        if (inputRaw instanceof Number) {
            return (Number) inputRaw;
        }

        // TODO use info
        logger.warn(
            "Unable to evaluate condition for \"{}\" attribute because unable to convert attribute type of \"{}\" to \"{}\"",
            name,
            inputRaw.getClass().getName(),
            Number.class.getName());

        return null;
    }

    @Nullable
    public Boolean matches(Number input) {
        return operator.sign == Integer.signum(Double.compare(input.doubleValue(), value.doubleValue()));
    }
}
