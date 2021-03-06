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
package com.optimizely.ab.config;

import ch.qos.logback.classic.Level;
import com.optimizely.ab.config.audience.AndCondition;
import com.optimizely.ab.config.audience.Condition;
import com.optimizely.ab.config.audience.NotCondition;
import com.optimizely.ab.config.audience.OrCondition;
import com.optimizely.ab.config.audience.UserAttribute;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;


import com.optimizely.ab.internal.LogbackVerifier;
import com.optimizely.ab.internal.ControlAttribute;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Tests for {@link ProjectConfig}.
 */
public class ProjectConfigTest {

    private ProjectConfig projectConfig;

    @Rule
    public LogbackVerifier logbackVerifier = new LogbackVerifier();

    @Before
    public void initialize() {
        projectConfig = ProjectConfigTestUtils.validProjectConfigV3();
    }

    /**
     * Verify that {@link ProjectConfig#toString()} doesn't throw an exception.
     */
    @Test
    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")
    public void toStringDoesNotFail() throws Exception {
        projectConfig.toString();
    }

    /**
     * Asserts that {@link ProjectConfig#getExperimentsForEventKey(String)}
     * returns the respective experiment ids for experiments using an event,
     * provided that the event parameter is valid.
     */
    @Test
    public void verifyGetExperimentsForValidEvent() throws Exception {
        Experiment experiment223 = projectConfig.getExperimentIdMapping().get("223");
        Experiment experiment118 = projectConfig.getExperimentIdMapping().get("118");
        List<Experiment> expectedSingleExperiment = asList(experiment223);
        List<Experiment> actualSingleExperiment = projectConfig.getExperimentsForEventKey("clicked_cart");
        assertThat(actualSingleExperiment, is(expectedSingleExperiment));

        List<Experiment> expectedMultipleExperiments = asList(experiment118, experiment223);
        List<Experiment> actualMultipleExperiments = projectConfig.getExperimentsForEventKey("clicked_purchase");
        assertThat(actualMultipleExperiments, is(expectedMultipleExperiments));
    }

    /**
     * Asserts that {@link ProjectConfig#getExperimentsForEventKey(String)} returns an empty List
     * when given an invalid event key.
     */
    @Test
    public void verifyGetExperimentsForInvalidEvent() throws Exception {
        List<Experiment> expectedExperiments = Collections.emptyList();
        List<Experiment> actualExperiments = projectConfig.getExperimentsForEventKey("a_fake_event");
        assertThat(actualExperiments, is(expectedExperiments));
    }

    /**
     * Asserts that getAudience returns the respective audience, provided the
     * audience ID parameter is valid.
     */
    @Test
    public void verifyGetAudienceConditionsFromValidId() throws Exception {
        List<Condition> userAttributes = new ArrayList<Condition>();
        userAttributes.add(new UserAttribute("browser_type", "custom_attribute", null, "firefox"));

        OrCondition orInner = new OrCondition(userAttributes);

        NotCondition notCondition = new NotCondition(orInner);
        List<Condition> outerOrList = new ArrayList<Condition>();
        outerOrList.add(notCondition);

        OrCondition orOuter = new OrCondition(outerOrList);
        List<Condition> andList = new ArrayList<Condition>();
        andList.add(orOuter);

        Condition expectedConditions = new AndCondition(andList);
        Condition actualConditions = projectConfig.getAudience("100").getConditions();
        assertThat(actualConditions, is(expectedConditions));
    }

    /**
     * Asserts that getAudience returns null given an invalid audience ID parameter.
     */
    @Test
    public void verifyGetAudienceFromInvalidId() throws Exception {
        assertNull(projectConfig.getAudience("invalid_id"));
    }

    /**
     * Asserts that anonymizeIP is set to false if not explicitly passed into the constructor (in the case of V2
     * projects).
     *
     * @throws Exception
     */
    @Test
    public void verifyAnonymizeIPIsFalseByDefault() throws Exception {
        ProjectConfig v2ProjectConfig = ProjectConfigTestUtils.validProjectConfigV2();
        assertFalse(v2ProjectConfig.getAnonymizeIP());
    }

    /**
     * Invalid User IDs
     * <p>
     * User ID is null
     * User ID is an empty string
     * Invalid Experiment IDs
     * <p>
     * Experiment key does not exist in the datafile
     * Experiment key is null
     * Experiment key is an empty string
     * Invalid Variation IDs [set only]
     * <p>
     * Variation key does not exist in the datafile
     * Variation key is null
     * Variation key is an empty string
     * Multiple set calls [set only]
     * <p>
     * Call set variation with different variations on one user/experiment to confirm that each set is expected.
     * Set variation on multiple variations for one user.
     * Set variations for multiple users.
     */
    /* UserID test */
    @Test
    @SuppressFBWarnings("NP")
    public void setForcedVariationNullUserId() {
        boolean b = projectConfig.setForcedVariation("etag1", null, "vtag1");
        assertFalse(b);
    }

    @Test
    @SuppressFBWarnings("NP")
    public void getForcedVariationNullUserId() {
        assertNull(projectConfig.getForcedVariation("etag1", null));
    }

    @Test
    public void setForcedVariationEmptyUserId() {
        assertTrue(projectConfig.setForcedVariation("etag1", "", "vtag1"));
    }

    @Test
    public void getForcedVariationEmptyUserId() {
        assertNotNull(projectConfig.getForcedVariation("etag1", ""));
    }

    /* Invalid Experiement */
    @Test
    @SuppressFBWarnings("NP")
    public void setForcedVariationNullExperimentKey() {
        assertFalse(projectConfig.setForcedVariation(null, "testUser1", "vtag1"));
    }

    @Test
    @SuppressFBWarnings("NP")
    public void getForcedVariationNullExperimentKey() {
        assertNull(projectConfig.getForcedVariation(null, "testUser1"));
    }

    @Test
    public void setForcedVariationWrongExperimentKey() {
        assertFalse(projectConfig.setForcedVariation("wrongKey", "testUser1", "vtag1"));

    }

    @Test
    public void getForcedVariationWrongExperimentKey() {
        assertNull(projectConfig.getForcedVariation("wrongKey", "testUser1"));
    }

    @Test
    public void setForcedVariationEmptyExperimentKey() {
        assertFalse(projectConfig.setForcedVariation("", "testUser1", "vtag1"));

    }

    @Test
    public void getForcedVariationEmptyExperimentKey() {
        assertNull(projectConfig.getForcedVariation("", "testUser1"));
    }

    /* Invalid Variation Id (set only */
    @Test
    public void setForcedVariationWrongVariationKey() {
        assertFalse(projectConfig.setForcedVariation("etag1", "testUser1", "vtag3"));
    }

    @Test
    public void setForcedVariationNullVariationKey() {
        assertFalse(projectConfig.setForcedVariation("etag1", "testUser1", null));
        assertNull(projectConfig.getForcedVariation("etag1", "testUser1"));
    }

    @Test
    public void setForcedVariationEmptyVariationKey() {
        assertFalse(projectConfig.setForcedVariation("etag1", "testUser1", ""));
    }

    /* Multiple set calls (set only */
    @Test
    public void setForcedVariationDifferentVariations() {
        assertTrue(projectConfig.setForcedVariation("etag1", "testUser1", "vtag1"));
        assertTrue(projectConfig.setForcedVariation("etag1", "testUser1", "vtag2"));
        assertEquals(projectConfig.getForcedVariation("etag1", "testUser1").getKey(), "vtag2");
        assertTrue(projectConfig.setForcedVariation("etag1", "testUser1", null));
    }

    @Test
    public void setForcedVariationMultipleVariationsExperiments() {
        assertTrue(projectConfig.setForcedVariation("etag1", "testUser1", "vtag1"));
        assertTrue(projectConfig.setForcedVariation("etag1", "testUser2", "vtag2"));
        assertTrue(projectConfig.setForcedVariation("etag2", "testUser1", "vtag3"));
        assertTrue(projectConfig.setForcedVariation("etag2", "testUser2", "vtag4"));
        assertEquals(projectConfig.getForcedVariation("etag1", "testUser1").getKey(), "vtag1");
        assertEquals(projectConfig.getForcedVariation("etag1", "testUser2").getKey(), "vtag2");
        assertEquals(projectConfig.getForcedVariation("etag2", "testUser1").getKey(), "vtag3");
        assertEquals(projectConfig.getForcedVariation("etag2", "testUser2").getKey(), "vtag4");
        assertTrue(projectConfig.setForcedVariation("etag1", "testUser1", null));
        assertTrue(projectConfig.setForcedVariation("etag1", "testUser2", null));
        assertTrue(projectConfig.setForcedVariation("etag2", "testUser1", null));
        assertTrue(projectConfig.setForcedVariation("etag2", "testUser2", null));
        assertNull(projectConfig.getForcedVariation("etag1", "testUser1"));
        assertNull(projectConfig.getForcedVariation("etag1", "testUser2"));
        assertNull(projectConfig.getForcedVariation("etag2", "testUser1"));
        assertNull(projectConfig.getForcedVariation("etag2", "testUser2"));


    }

    @Test
    public void setForcedVariationMultipleUsers() {
        assertTrue(projectConfig.setForcedVariation("etag1", "testUser1", "vtag1"));
        assertTrue(projectConfig.setForcedVariation("etag1", "testUser2", "vtag1"));
        assertTrue(projectConfig.setForcedVariation("etag1", "testUser3", "vtag1"));
        assertTrue(projectConfig.setForcedVariation("etag1", "testUser4", "vtag1"));

        assertEquals(projectConfig.getForcedVariation("etag1", "testUser1").getKey(), "vtag1");
        assertEquals(projectConfig.getForcedVariation("etag1", "testUser2").getKey(), "vtag1");
        assertEquals(projectConfig.getForcedVariation("etag1", "testUser3").getKey(), "vtag1");
        assertEquals(projectConfig.getForcedVariation("etag1", "testUser4").getKey(), "vtag1");

        assertTrue(projectConfig.setForcedVariation("etag1", "testUser1", null));
        assertTrue(projectConfig.setForcedVariation("etag1", "testUser2", null));
        assertTrue(projectConfig.setForcedVariation("etag1", "testUser3", null));
        assertTrue(projectConfig.setForcedVariation("etag1", "testUser4", null));

        assertNull(projectConfig.getForcedVariation("etag1", "testUser1"));
        assertNull(projectConfig.getForcedVariation("etag1", "testUser2"));
        assertNull(projectConfig.getForcedVariation("etag2", "testUser1"));
        assertNull(projectConfig.getForcedVariation("etag2", "testUser2"));

    }

    @Test
    public void getAttributeIDWhenAttributeKeyIsFromAttributeKeyMapping() {
        ProjectConfig projectConfig = ProjectConfigTestUtils.validProjectConfigV4();
        String attributeID = projectConfig.getAttributeId(projectConfig, "house");
        assertEquals(attributeID, "553339214");
    }

    @Test
    public void getAttributeIDWhenAttributeKeyIsUsingReservedKey() {
        ProjectConfig projectConfig = ProjectConfigTestUtils.validProjectConfigV4();
        String attributeID = projectConfig.getAttributeId(projectConfig, "$opt_user_agent");
        assertEquals(attributeID, ControlAttribute.USER_AGENT_ATTRIBUTE.toString());
    }

    @Test
    public void getAttributeIDWhenAttributeKeyUnrecognizedAttribute() {
        ProjectConfig projectConfig = ProjectConfigTestUtils.validProjectConfigV4();
        String invalidAttribute = "empty";
        String attributeID = projectConfig.getAttributeId(projectConfig, invalidAttribute);
        assertNull(attributeID);
        logbackVerifier.expectMessage(Level.DEBUG, "Unrecognized Attribute \"" + invalidAttribute + "\"");
    }

    @Test
    public void getAttributeIDWhenAttributeKeyPrefixIsMatched() {
        ProjectConfig projectConfig = ProjectConfigTestUtils.validProjectConfigV4();
        String attributeWithReservedPrefix = "$opt_test";
        String attributeID = projectConfig.getAttributeId(projectConfig, attributeWithReservedPrefix);
        assertEquals(attributeID, "583394100");
        logbackVerifier.expectMessage(Level.WARN, "Attribute " + attributeWithReservedPrefix + " unexpectedly" +
            " has reserved prefix $opt_; using attribute ID instead of reserved attribute name.");
    }

}
