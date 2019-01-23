/**
 *
 *    Copyright 2016-2019, Optimizely and contributors
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
package com.optimizely.ab.config.audience;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.optimizely.ab.config.ProjectConfig;
import com.optimizely.ab.config.audience.match.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.Map;

/**
 * Represents a user attribute instance within an audience's conditions.
 */
@Immutable
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserAttribute<T> implements Condition<T> {
    private static final Logger logger = LoggerFactory.getLogger(UserAttribute.class);
    private static final String CUSTOM_ATTRIBUTE = "custom_attribute";

    private final String name;
    private final String type;
    private final String match;
    private final Object value;

    @JsonCreator
    public UserAttribute(@JsonProperty("name") @Nonnull String name,
                         @JsonProperty("type") @Nonnull String type,
                         @JsonProperty("match") @Nullable String match,
                         @JsonProperty("value") @Nullable Object value) {
        this.name = name;
        this.type = type;
        this.match = match;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getMatch() {
        return match;
    }

    public Object getValue() {
        return value;
    }

    @Nullable
    public Boolean evaluate(ProjectConfig config, Map<String, ?> attributes) {
        if (!CUSTOM_ATTRIBUTE.equals(type)) {
            logger.warn("Audience condition \"{}\" has an unknown condition type. You may need to upgrade to a newer release of the Optimizely SDK", this);
            return null; // unknown type
        }

        try {
            Condition condition = getCondition();
            //noinspection unchecked
            return condition.evaluate(config, attributes);
        } catch (UnknownMatchTypeException e) {
            logger.warn("Audience condition has an unknown match type. You may need to upgrade to a newer release of the Optimizely SDK");
            return null;
        } catch (IncompatibleMatchValue e) {
            logger.warn("Audience condition has an unexpected value type. You may need to upgrade to a newer release of the Optimizely SDK", e.getMessage());
            return null;
        }
    }

    // TODO consider splitting out into MatchConditionFactory class
    Condition getCondition() throws UnknownMatchTypeException, IncompatibleMatchValue {
        Condition condition = null;

        // match is assigned when match and conditionValue are compatible
        if (match == null) {
            if (value instanceof CharSequence) {
                condition = new DefaultMatchCondition<>(name, value.toString());
            }
        } else {
            switch (match) {
                case "exists":
                    // TODO log if value != null?
                    condition = new ExistsMatch(name);
                    break;
                case "exact":
                    if (value instanceof CharSequence) {
                        condition = new StringMatchCondition(name, value.toString());
                    } else if (value != null && isValidNumber(value)) {
                        condition = new NumericMatchCondition(name, (Number) value, NumericMatchCondition.Operator.EQUAL_TO);
                    } else if (value instanceof Boolean) {
                        condition = new DefaultMatchCondition<>(name, (Boolean) value);
                    }
                    break;
                case "substring":
                    if (value instanceof CharSequence) {
                        condition = new SubstringMatchCondition(name, (CharSequence) value);
                    }
                    break;
                case "gt":
                    if (value != null && isValidNumber(value)) {
                        condition = new NumericMatchCondition(name, (Number) value, NumericMatchCondition.Operator.GREATER_THAN);
                    }
                    break;
                case "lt":
                    if (value != null && isValidNumber(value)) {
                        condition = new NumericMatchCondition(name, (Number) value, NumericMatchCondition.Operator.LESS_THAN);
                    }
                    break;
                default:
                    throw new UnknownMatchTypeException();
            }
        }

        if (match == null) {
            throw new IncompatibleMatchValue();
        }

        return condition;
    }

    private static boolean isValidNumber(Object o) {
        if (o instanceof Integer) { // TODO accept Long?
            return Math.abs((Integer) o) <= 1e53;
        } else if (o instanceof Double) { // TODO accept Float?
            Double value = ((Number) o).doubleValue();
            return !(value.isNaN() || value.isInfinite());
        }
        return false;
    }

    @Override
    public String toString() {
        final String valueStr;
        if (value == null) {
            valueStr = "null";
        } else if (value instanceof String) {
            valueStr = String.format("'%s'", value);
        } else {
            valueStr = value.toString();
        }
        return "{name='" + name + "\'" +
            ", type='" + type + "\'" +
            ", match='" + match + "\'" +
            ", value=" + valueStr +
            "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UserAttribute that = (UserAttribute) o;

        if (!name.equals(that.name)) return false;
        if (!type.equals(that.type)) return false;
        if (match != null ? !match.equals(that.match) : that.match != null) return false;
        return value != null ? value.equals(that.value) : that.value == null;
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + type.hashCode();
        result = 31 * result + (match != null ? match.hashCode() : 0);
        result = 31 * result + (value != null ? value.hashCode() : 0);
        return result;
    }

    static class UnknownMatchTypeException extends Exception {}

    static class IncompatibleMatchValue extends Exception {}
}
