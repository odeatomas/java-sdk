/**
 *
 *    Copyright 2019, Optimizely and contributors
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

import com.optimizely.ab.config.ProjectConfig;
import org.junit.Test;
import org.mockito.Mockito;

import java.nio.CharBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

@SuppressWarnings("unchecked")
public class UserAttributeTest {
    @Test
    public void existsMatch() {
        Map<String, ?> attributes = attributes().add("city", "sf").get();
        UserAttribute cityExists = new UserAttribute("city", "custom_attribute", "exists", null);
        UserAttribute stateExists = new UserAttribute("state", "custom_attribute", "exists", null);
        assertThat(cityExists.evaluate(projectConfig(), attributes), is(true));
        assertThat(stateExists.evaluate(projectConfig(), attributes), is(false));
        assertThat(stateExists.evaluate(projectConfig(), null), is(false));
    }

    @Test
    public void exactMatch() {
        Map<String, ?> attributes = attributes()
            .add("str", "abc")
            .add("int", 123)
            .add("double", 42.42)
            .add("true", true)
            .add("false", false)
            .get();

        UserAttribute strMatch = new UserAttribute("str", "custom_attribute", "exact", "abc");
        UserAttribute intMatch = new UserAttribute("int", "custom_attribute", "exact", 123);
        UserAttribute doubleMatch = new UserAttribute("double", "custom_attribute", "exact", 42.42);
        UserAttribute trueMatch = new UserAttribute("true", "custom_attribute", "exact", true);
        UserAttribute falseMatch = new UserAttribute("false", "custom_attribute", "exact", false);
        assertThat(strMatch.evaluate(projectConfig(), attributes), is(true));
        assertThat(intMatch.evaluate(projectConfig(), attributes), is(true));
        assertThat(doubleMatch.evaluate(projectConfig(), attributes), is(true));
        assertThat(trueMatch.evaluate(projectConfig(), attributes), is(true));
        assertThat(falseMatch.evaluate(projectConfig(), attributes), is(true));

        attributes = attributes().add("str", CharBuffer.wrap("abc")).get();
        assertThat(strMatch.evaluate(projectConfig(), attributes), is(true));
    }

    @Test
    public void exactMatchNegative() {
        Map<String, ?> attributes = attributes()
            .add("str", "def")
            .add("int", 123.5)
            .add("double", 42)
            .add("true", false)
            .add("false", true)
            .get();

        UserAttribute strMatch = new UserAttribute("str", "custom_attribute", "exact", "abc");
        UserAttribute intMatch = new UserAttribute("int", "custom_attribute", "exact", 123);
        UserAttribute doubleMatch = new UserAttribute("double", "custom_attribute", "exact", 42.42);
        UserAttribute trueMatch = new UserAttribute("true", "custom_attribute", "exact", true);
        UserAttribute falseMatch = new UserAttribute("false", "custom_attribute", "exact", false);
        assertThat(strMatch.evaluate(projectConfig(), attributes), is(false));
        assertThat(intMatch.evaluate(projectConfig(), attributes), is(false));
        assertThat(doubleMatch.evaluate(projectConfig(), attributes), is(false));
        assertThat(trueMatch.evaluate(projectConfig(), attributes), is(false));
        assertThat(falseMatch.evaluate(projectConfig(), attributes), is(false));

        attributes = attributes()
            .add("str", "")
            .add("int", Double.POSITIVE_INFINITY)
            .add("double", Double.POSITIVE_INFINITY)
            .add("true", "true")
            .add("false", 0)
            .get();
        assertThat(strMatch.evaluate(projectConfig(), attributes), is(false));
        assertThat(intMatch.evaluate(projectConfig(), attributes), is(false));
        assertThat(doubleMatch.evaluate(projectConfig(), attributes), is(false));
        assertThat(trueMatch.evaluate(projectConfig(), attributes), is(false));
        assertThat(falseMatch.evaluate(projectConfig(), attributes), is(false));
    }

    @Test
    public void exactMatchUnknown() {
        UserAttribute attr = new UserAttribute("???", "custom_attribute", "exact", "!!!");
        assertThat(attr.evaluate(projectConfig(), attributes().get()), nullValue());
        assertThat(attr.evaluate(projectConfig(), null), nullValue());
    }

    private ProjectConfig projectConfig() {
        return Mockito.mock(ProjectConfig.class);
    }

    private AttributesBuilder attributes() {
        return new AttributesBuilder();
    }

    private static class AttributesBuilder {
        Map<String, Object> m = new HashMap<>();

        AttributesBuilder add(String name, Object value) {
            m.put(name, value);
            return this;
        }

        Map<String, ?> get() {
            return m;
        }
    }
}