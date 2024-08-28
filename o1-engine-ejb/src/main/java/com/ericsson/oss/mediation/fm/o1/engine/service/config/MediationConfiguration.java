/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2023
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.oss.mediation.fm.o1.engine.service.config;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;

import com.ericsson.oss.mediation.fm.o1.engine.service.config.jar.MediationConfigurationJar;

import lombok.extern.slf4j.Slf4j;

/**
 * Class that reads and stores the mediation configuration data from the "o1-mediation-config" XML file.
 * This class is initialized on startup by the {@code MediationConfigurationBean} to read the data from the file.
 */
@Slf4j
public class MediationConfiguration {

    private static final String O1_MEDIATION_CONFIG_REPOSITORY_KEY = "o1.mediation.config.repository";
    private static final String O1_DEFAULT_CONFIG_REPOSITORY = "/etc/opt/ericsson/o1/mediation/o1-mediation-config";

    private static final Map<NetworkElementId, NetworkElementConfiguration> configurations = new ConcurrentHashMap<>();
    private Path configRepositoryPath;
    private List<Path> jarPaths;

    @Inject
    private MediationConfigurationJar mediationConfigurationJar;

    public void init() {
        log.info("Initializing...getting network element configuration from jar");
        final String repositoryPath = System.getProperty(O1_MEDIATION_CONFIG_REPOSITORY_KEY, O1_DEFAULT_CONFIG_REPOSITORY);
        configRepositoryPath = Paths.get(new File(repositoryPath).toURI());
        log.info("Repository path is {}", configRepositoryPath);
        jarPaths = mediationConfigurationJar.getJars(configRepositoryPath);
        for (final Path jarPath : jarPaths) {
            addNetworkElementConfiguration(jarPath);
        }
    }

    private void addNetworkElementConfiguration(final Path jarPath) {
        if (!mediationConfigurationJar.isO1MediationConfigurationJar(jarPath)) {
            return;
        }
        log.info("Creating network element config from XML...");
        final List<NetworkElementConfiguration> networkElementConfigurations =
                mediationConfigurationJar.createNetworkElementConfigurations(configRepositoryPath, jarPath);
        for (final NetworkElementConfiguration networkElementConfiguration : networkElementConfigurations) {
            configurations.put(networkElementConfiguration.getNetworkElementId(), networkElementConfiguration);
        }
    }

    public List<Path> getJarPaths() {
        return jarPaths;
    }

    public NetworkElementConfiguration getConfiguration(final NetworkElementId networkElementId) {
        return configurations.get(networkElementId);
    }

    public List<NetworkElementConfiguration> getConfigurations() {
        return Collections.unmodifiableList(new ArrayList<>(configurations.values()));
    }
}
