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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.optimizely.ab.UnknownEventTypeException;
import com.optimizely.ab.UnknownExperimentException;
import com.optimizely.ab.config.audience.Audience;
import com.optimizely.ab.config.parser.ConfigParseException;
import com.optimizely.ab.config.parser.DefaultConfigParser;
import com.optimizely.ab.error.ErrorHandler;
import com.optimizely.ab.error.NoOpErrorHandler;
import com.optimizely.ab.error.RaiseExceptionErrorHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents the Optimizely Project configuration.
 *
 * @see <a href="http://developers.optimizely.com/server/reference/index.html#json">Project JSON</a>
 */
@Immutable
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProjectConfig {

    public enum Version {
        V2("2"),
        V3("3"),
        V4("4");

        private final String version;

        Version(String version) {
            this.version = version;
        }

        @Override
        public String toString() {
            return version;
        }
    }

    private static final List<String> supportedVersions = Arrays.asList(
        Version.V2.version,
        Version.V3.version,
        Version.V4.version
    );

    // logger
    private static final Logger logger = LoggerFactory.getLogger(ProjectConfig.class);

    // ProjectConfig properties
    private final String accountId;
    private final String projectId;
    private final String revision;
    private final String version;
    private final boolean anonymizeIP;
    private final Boolean botFiltering;
    private final List<Attribute> attributes;
    private final List<Audience> audiences;
    private final List<Audience> typedAudiences;
    private final List<EventType> events;
    private final List<Experiment> experiments;
    private final List<FeatureFlag> featureFlags;
    private final List<Group> groups;
    private final List<Rollout> rollouts;

    // key to entity mappings
    private final Map<String, Attribute> attributeKeyMapping;
    private final Map<String, EventType> eventNameMapping;
    private final Map<String, Experiment> experimentKeyMapping;
    private final Map<String, FeatureFlag> featureKeyMapping;

    // id to entity mappings
    private final Map<String, Audience> audienceIdMapping;
    private final Map<String, Experiment> experimentIdMapping;
    private final Map<String, Group> groupIdMapping;
    private final Map<String, Rollout> rolloutIdMapping;

    // other mappings
    private final Map<String, Experiment> variationIdToExperimentMapping;

    public final static String RESERVED_ATTRIBUTE_PREFIX = "$opt_";

    /**
     * Forced variations supersede any other mappings.  They are transient and are not persistent or part of
     * the actual datafile. This contains all the forced variations
     * set by the user by calling {@link ProjectConfig#setForcedVariation(String, String, String)} (it is not the same as the
     * whitelisting forcedVariations data structure in the Experiments class).
     */
    private transient ConcurrentHashMap<String, ConcurrentHashMap<String, String>> forcedVariationMapping = new ConcurrentHashMap<String, ConcurrentHashMap<String, String>>();

    // v2 constructor
    public ProjectConfig(String accountId, String projectId, String version, String revision, List<Group> groups,
                         List<Experiment> experiments, List<Attribute> attributes, List<EventType> eventType,
                         List<Audience> audiences) {
        this(accountId, projectId, version, revision, groups, experiments, attributes, eventType, audiences, false);
    }

    // v3 constructor
    public ProjectConfig(String accountId, String projectId, String version, String revision, List<Group> groups,
                         List<Experiment> experiments, List<Attribute> attributes, List<EventType> eventType,
                         List<Audience> audiences, boolean anonymizeIP) {
        this(
            accountId,
            anonymizeIP,
            null,
            projectId,
            revision,
            version,
            attributes,
            audiences,
            null,
            eventType,
            experiments,
            null,
            groups,
            null
        );
    }

    // v4 constructor
    public ProjectConfig(String accountId,
                         boolean anonymizeIP,
                         Boolean botFiltering,
                         String projectId,
                         String revision,
                         String version,
                         List<Attribute> attributes,
                         List<Audience> audiences,
                         List<Audience> typedAudiences,
                         List<EventType> events,
                         List<Experiment> experiments,
                         List<FeatureFlag> featureFlags,
                         List<Group> groups,
                         List<Rollout> rollouts) {

        this.accountId = accountId;
        this.projectId = projectId;
        this.version = version;
        this.revision = revision;
        this.anonymizeIP = anonymizeIP;
        this.botFiltering = botFiltering;

        this.attributes = Collections.unmodifiableList(attributes);
        this.audiences = Collections.unmodifiableList(audiences);

        if (typedAudiences != null) {
            this.typedAudiences = Collections.unmodifiableList(typedAudiences);
        } else {
            this.typedAudiences = Collections.emptyList();
        }

        this.events = Collections.unmodifiableList(events);
        if (featureFlags == null) {
            this.featureFlags = Collections.emptyList();
        } else {
            this.featureFlags = Collections.unmodifiableList(featureFlags);
        }
        if (rollouts == null) {
            this.rollouts = Collections.emptyList();
        } else {
            this.rollouts = Collections.unmodifiableList(rollouts);
        }

        this.groups = Collections.unmodifiableList(groups);

        List<Experiment> allExperiments = new ArrayList<Experiment>();
        allExperiments.addAll(experiments);
        allExperiments.addAll(aggregateGroupExperiments(groups));
        this.experiments = Collections.unmodifiableList(allExperiments);

        Map<String, Experiment> variationIdToExperimentMap = new HashMap<String, Experiment>();
        for (Experiment experiment : this.experiments) {
            for (Variation variation : experiment.getVariations()) {
                variationIdToExperimentMap.put(variation.getId(), experiment);
            }
        }
        this.variationIdToExperimentMapping = Collections.unmodifiableMap(variationIdToExperimentMap);

        // generate the name mappers
        this.attributeKeyMapping = ProjectConfigUtils.generateNameMapping(attributes);
        this.eventNameMapping = ProjectConfigUtils.generateNameMapping(this.events);
        this.experimentKeyMapping = ProjectConfigUtils.generateNameMapping(this.experiments);
        this.featureKeyMapping = ProjectConfigUtils.generateNameMapping(this.featureFlags);

        // generate audience id to audience mapping
        if (typedAudiences == null) {
            this.audienceIdMapping = ProjectConfigUtils.generateIdMapping(audiences);
        } else {
            List<Audience> combinedList = new ArrayList<>(audiences);
            combinedList.addAll(typedAudiences);
            this.audienceIdMapping = ProjectConfigUtils.generateIdMapping(combinedList);
        }
        this.experimentIdMapping = ProjectConfigUtils.generateIdMapping(this.experiments);
        this.groupIdMapping = ProjectConfigUtils.generateIdMapping(groups);
        this.rolloutIdMapping = ProjectConfigUtils.generateIdMapping(this.rollouts);
    }

    /**
     * Helper method to retrieve the {@link Experiment} for the given experiment key.
     * If {@link RaiseExceptionErrorHandler} is provided, either an experiment is returned,
     * or an exception is sent to the error handler
     * if there are no experiments in the project config with the given experiment key.
     * If {@link NoOpErrorHandler} is used, either an experiment or {@code null} is returned.
     *
     * @param experimentKey the experiment to retrieve from the current project config
     * @param errorHandler  the error handler to send exceptions to
     * @return the experiment for given experiment key
     */
    @CheckForNull
    public Experiment getExperimentForKey(@Nonnull String experimentKey,
                                          @Nonnull ErrorHandler errorHandler) {

        Experiment experiment =
            getExperimentKeyMapping()
                .get(experimentKey);

        // if the given experiment key isn't present in the config, log an exception to the error handler
        if (experiment == null) {
            String unknownExperimentError = String.format("Experiment \"%s\" is not in the datafile.", experimentKey);
            logger.error(unknownExperimentError);
            errorHandler.handleError(new UnknownExperimentException(unknownExperimentError));
        }

        return experiment;
    }

    /**
     * Helper method to retrieve the {@link EventType} for the given event name.
     * If {@link RaiseExceptionErrorHandler} is provided, either an event type is returned,
     * or an exception is sent to the error handler if there are no event types in the project config with the given name.
     * If {@link NoOpErrorHandler} is used, either an event type or {@code null} is returned.
     *
     * @param eventName    the event type to retrieve from the current project config
     * @param errorHandler the error handler to send exceptions to
     * @return the event type for the given event name
     */
    @CheckForNull
    public EventType getEventTypeForName(String eventName, ErrorHandler errorHandler) {

        EventType eventType = getEventNameMapping().get(eventName);

        // if the given event name isn't present in the config, log an exception to the error handler
        if (eventType == null) {
            String unknownEventTypeError = String.format("Event \"%s\" is not in the datafile.", eventName);
            logger.error(unknownEventTypeError);
            errorHandler.handleError(new UnknownEventTypeException(unknownEventTypeError));
        }

        return eventType;
    }


    @Nullable
    public Experiment getExperimentForVariationId(String variationId) {
        return this.variationIdToExperimentMapping.get(variationId);
    }

    private List<Experiment> aggregateGroupExperiments(List<Group> groups) {
        List<Experiment> groupExperiments = new ArrayList<Experiment>();
        for (Group group : groups) {
            groupExperiments.addAll(group.getExperiments());
        }

        return groupExperiments;
    }

    /**
     * Checks is attributeKey is reserved or not and if it exist in attributeKeyMapping
     *
     * @param attributeKey
     * @return AttributeId corresponding to AttributeKeyMapping, AttributeKey when it's a reserved attribute and
     * null when attributeKey is equal to BOT_FILTERING_ATTRIBUTE key.
     */
    public String getAttributeId(ProjectConfig projectConfig, String attributeKey) {
        String attributeIdOrKey = null;
        com.optimizely.ab.config.Attribute attribute = projectConfig.getAttributeKeyMapping().get(attributeKey);
        boolean hasReservedPrefix = attributeKey.startsWith(RESERVED_ATTRIBUTE_PREFIX);
        if (attribute != null) {
            if (hasReservedPrefix) {
                logger.warn("Attribute {} unexpectedly has reserved prefix {}; using attribute ID instead of reserved attribute name.",
                    attributeKey, RESERVED_ATTRIBUTE_PREFIX);
            }
            attributeIdOrKey = attribute.getId();
        } else if (hasReservedPrefix) {
            attributeIdOrKey = attributeKey;
        } else {
            logger.debug("Unrecognized Attribute \"{}\"", attributeKey);
        }
        return attributeIdOrKey;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getVersion() {
        return version;
    }

    public String getRevision() {
        return revision;
    }

    public boolean getAnonymizeIP() {
        return anonymizeIP;
    }

    public Boolean getBotFiltering() {
        return botFiltering;
    }

    public List<Group> getGroups() {
        return groups;
    }

    public List<Experiment> getExperiments() {
        return experiments;
    }

    public List<Experiment> getExperimentsForEventKey(String eventKey) {
        EventType event = eventNameMapping.get(eventKey);
        if (event != null) {
            List<String> experimentIds = event.getExperimentIds();
            List<Experiment> experiments = new ArrayList<Experiment>(experimentIds.size());
            for (String experimentId : experimentIds) {
                experiments.add(experimentIdMapping.get(experimentId));
            }

            return experiments;
        }

        return Collections.emptyList();
    }

    public List<FeatureFlag> getFeatureFlags() {
        return featureFlags;
    }

    public List<Rollout> getRollouts() {
        return rollouts;
    }

    public List<Attribute> getAttributes() {
        return attributes;
    }

    public List<EventType> getEventTypes() {
        return events;
    }

    public List<Audience> getAudiences() {
        return audiences;
    }

    public List<Audience> getTypedAudiences() {
        return typedAudiences;
    }

    public Audience getAudience(String audienceId) {
        return audienceIdMapping.get(audienceId);
    }

    public Map<String, Experiment> getExperimentKeyMapping() {
        return experimentKeyMapping;
    }

    public Map<String, Attribute> getAttributeKeyMapping() {
        return attributeKeyMapping;
    }

    public Map<String, EventType> getEventNameMapping() {
        return eventNameMapping;
    }

    public Map<String, Audience> getAudienceIdMapping() {
        return audienceIdMapping;
    }

    public Map<String, Experiment> getExperimentIdMapping() {
        return experimentIdMapping;
    }

    public Map<String, Group> getGroupIdMapping() {
        return groupIdMapping;
    }

    public Map<String, Rollout> getRolloutIdMapping() {
        return rolloutIdMapping;
    }

    public Map<String, FeatureFlag> getFeatureKeyMapping() {
        return featureKeyMapping;
    }

    public ConcurrentHashMap<String, ConcurrentHashMap<String, String>> getForcedVariationMapping() {
        return forcedVariationMapping;
    }

    /**
     * Force a user into a variation for a given experiment.
     * The forced variation value does not persist across application launches.
     * If the experiment key is not in the project file, this call fails and returns false.
     *
     * @param experimentKey The key for the experiment.
     * @param userId        The user ID to be used for bucketing.
     * @param variationKey  The variation key to force the user into.  If the variation key is null
     *                      then the forcedVariation for that experiment is removed.
     * @return boolean A boolean value that indicates if the set completed successfully.
     */
    public boolean setForcedVariation(@Nonnull String experimentKey,
                                      @Nonnull String userId,
                                      @Nullable String variationKey) {

        // if the experiment is not a valid experiment key, don't set it.
        Experiment experiment = getExperimentKeyMapping().get(experimentKey);
        if (experiment == null) {
            logger.error("Experiment {} does not exist in ProjectConfig for project {}", experimentKey, projectId);
            return false;
        }

        Variation variation = null;

        // keep in mind that you can pass in a variationKey that is null if you want to
        // remove the variation.
        if (variationKey != null) {
            variation = experiment.getVariationKeyToVariationMap().get(variationKey);
            // if the variation is not part of the experiment, return false.
            if (variation == null) {
                logger.error("Variation {} does not exist for experiment {}", variationKey, experimentKey);
                return false;
            }
        }

        // if the user id is invalid, return false.
        if (!validateUserId(userId)) {
            return false;
        }

        ConcurrentHashMap<String, String> experimentToVariation;
        if (!forcedVariationMapping.containsKey(userId)) {
            forcedVariationMapping.putIfAbsent(userId, new ConcurrentHashMap<String, String>());
        }
        experimentToVariation = forcedVariationMapping.get(userId);

        boolean retVal = true;
        // if it is null remove the variation if it exists.
        if (variationKey == null) {
            String removedVariationId = experimentToVariation.remove(experiment.getId());
            if (removedVariationId != null) {
                Variation removedVariation = experiment.getVariationIdToVariationMap().get(removedVariationId);
                if (removedVariation != null) {
                    logger.debug("Variation mapped to experiment \"{}\" has been removed for user \"{}\"", experiment.getKey(), userId);
                } else {
                    logger.debug("Removed forced variation that did not exist in experiment");
                }
            } else {
                logger.debug("No variation for experiment {}", experimentKey);
                retVal = false;
            }
        } else {
            String previous = experimentToVariation.put(experiment.getId(), variation.getId());
            logger.debug("Set variation \"{}\" for experiment \"{}\" and user \"{}\" in the forced variation map.",
                variation.getKey(), experiment.getKey(), userId);
            if (previous != null) {
                Variation previousVariation = experiment.getVariationIdToVariationMap().get(previous);
                if (previousVariation != null) {
                    logger.debug("forced variation {} replaced forced variation {} in forced variation map.",
                        variation.getKey(), previousVariation.getKey());
                }
            }
        }

        return retVal;
    }

    /**
     * Gets the forced variation for a given user and experiment.
     *
     * @param experimentKey The key for the experiment.
     * @param userId        The user ID to be used for bucketing.
     * @return The variation the user was bucketed into. This value can be null if the
     * forced variation fails.
     */
    @Nullable
    public Variation getForcedVariation(@Nonnull String experimentKey,
                                        @Nonnull String userId) {

        // if the user id is invalid, return false.
        if (!validateUserId(userId)) {
            return null;
        }

        if (experimentKey == null || experimentKey.isEmpty()) {
            logger.error("experiment key is invalid");
            return null;
        }

        Map<String, String> experimentToVariation = getForcedVariationMapping().get(userId);
        if (experimentToVariation != null) {
            Experiment experiment = getExperimentKeyMapping().get(experimentKey);
            if (experiment == null) {
                logger.debug("No experiment \"{}\" mapped to user \"{}\" in the forced variation map ", experimentKey, userId);
                return null;
            }
            String variationId = experimentToVariation.get(experiment.getId());
            if (variationId != null) {
                Variation variation = experiment.getVariationIdToVariationMap().get(variationId);
                if (variation != null) {
                    logger.debug("Variation \"{}\" is mapped to experiment \"{}\" and user \"{}\" in the forced variation map",
                        variation.getKey(), experimentKey, userId);
                    return variation;
                }
            } else {
                logger.debug("No variation for experiment \"{}\" mapped to user \"{}\" in the forced variation map ", experimentKey, userId);
            }
        }
        return null;
    }

    /**
     * Helper function to check that the provided userId is valid
     *
     * @param userId the userId being validated
     * @return whether the user ID is valid
     */
    private boolean validateUserId(String userId) {
        if (userId == null) {
            logger.error("User ID is invalid");
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        return "ProjectConfig{" +
            "accountId='" + accountId + '\'' +
            ", projectId='" + projectId + '\'' +
            ", revision='" + revision + '\'' +
            ", version='" + version + '\'' +
            ", anonymizeIP=" + anonymizeIP +
            ", botFiltering=" + botFiltering +
            ", attributes=" + attributes +
            ", audiences=" + audiences +
            ", typedAudiences=" + typedAudiences +
            ", events=" + events +
            ", experiments=" + experiments +
            ", featureFlags=" + featureFlags +
            ", groups=" + groups +
            ", rollouts=" + rollouts +
            ", attributeKeyMapping=" + attributeKeyMapping +
            ", eventNameMapping=" + eventNameMapping +
            ", experimentKeyMapping=" + experimentKeyMapping +
            ", featureKeyMapping=" + featureKeyMapping +
            ", audienceIdMapping=" + audienceIdMapping +
            ", experimentIdMapping=" + experimentIdMapping +
            ", groupIdMapping=" + groupIdMapping +
            ", rolloutIdMapping=" + rolloutIdMapping +
            ", forcedVariationMapping=" + forcedVariationMapping +
            ", variationIdToExperimentMapping=" + variationIdToExperimentMapping +
            '}';
    }

    public static class Builder {
        private String datafile;

        public Builder withDatafile(String datafile) {
            this.datafile = datafile;
            return this;
        }

        /**
         * @return a {@link ProjectConfig} instance given a JSON string datafile
         */
        public ProjectConfig build() throws ConfigParseException {
            if (datafile == null) {
                throw new ConfigParseException("Unable to parse null datafile.");
            }
            if (datafile.isEmpty()) {
                throw new ConfigParseException("Unable to parse empty datafile.");
            }

            ProjectConfig projectConfig = DefaultConfigParser.getInstance().parseProjectConfig(datafile);

            if (!supportedVersions.contains(projectConfig.getVersion())) {
                throw new ConfigParseException("This version of the Java SDK does not support the given datafile version: " + projectConfig.getVersion());
            }

            return projectConfig;
        }
    }
}
