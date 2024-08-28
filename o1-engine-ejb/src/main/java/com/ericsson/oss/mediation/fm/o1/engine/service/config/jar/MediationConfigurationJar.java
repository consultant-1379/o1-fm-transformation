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

package com.ericsson.oss.mediation.fm.o1.engine.service.config.jar;

import java.io.IOException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import com.ericsson.oss.mediation.fm.o1.engine.service.config.MediationConfigurationException;
import com.ericsson.oss.mediation.fm.o1.engine.service.config.NetworkElementConfiguration;
import com.ericsson.oss.mediation.fm.o1.engine.service.config.NetworkElementId;
import com.ericsson.oss.mediation.fm.o1.engine.service.config.xml.MediationConfigurationXmlLoader;
import com.ericsson.oss.mediation.fm.o1.engine.service.config.xml.NetworkElementConfig;
import com.ericsson.oss.mediation.fm.o1.engine.service.config.xml.O1MediationConfig;

import lombok.extern.slf4j.Slf4j;

/**
 * A jar utility class for reading information from the o1-fm-transformation jar and creating a
 * NetworkElementConfiguration object.
 */
@Slf4j
public class MediationConfigurationJar {

    private static final String DEFAULT_JAR_FILE_FILTER = "**";
    private static final String O1_CONFIG_FILE_FILTER = "**/META-INF/o1-mediation-config.xml";
    private static final String TRANSFORMER_CONFIG_FILE_FILTER = "**/transformer/transformer-config.xml";
    private static final FileSystem fileSystem = FileSystems.getDefault();

    @Inject
    private MediationConfigurationXmlLoader mediationConfigurationXmlLoader;

    public List<Path> getJars(final Path configRepoPath) {

        final List<Path> jars = new ArrayList<>();
        final DirectoryStream.Filter<Path> fileFilter = JarNioUtil.createFilter(fileSystem, DEFAULT_JAR_FILE_FILTER);
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(configRepoPath, fileFilter)) {
            for (final Path path : directoryStream) {
                if (JarNioUtil.isJarFile(path)) {
                    jars.add(path);
                    log.info("Adding jar: {}", path);
                }
            }
        } catch (final IOException ex) {
            throw new MediationConfigurationException(ex);
        }
        return jars;
    }

    public List<NetworkElementConfiguration> createNetworkElementConfigurations(final Path dir, final Path jarPath) {

        final List<NetworkElementConfiguration> configurations = new ArrayList<>();
        try {
            final URL configFileURL = getConfigFileUrl(jarPath);

            final O1MediationConfig o1MediationConfig = mediationConfigurationXmlLoader.load(configFileURL);
            final List<NetworkElementConfig> networkElementConfigs = o1MediationConfig.getNetworkElementConfigurations();

            if (networkElementConfigs == null) {
                throw new MediationConfigurationException("networkElementConfig is null processing jarPath: " + jarPath);
            }

            for (final NetworkElementConfig networkElementConfig : networkElementConfigs) {
                checkTypeAndVersion(jarPath, networkElementConfig);

                final NetworkElementId networkElementId = new NetworkElementId(networkElementConfig.getType(), networkElementConfig.getVersion());
                final NetworkElementConfiguration configuration = new NetworkElementConfiguration(networkElementId);

                if (networkElementConfig.getTransformerConfig() != null) {
                    // adds the transformer configuration file from the jarPath specified in the tag <transformer-config>
                    final Path externalTransformerPath = getTransformerConfigFromExternalJarPath(networkElementConfig, dir);
                    addTransformerConfigFromJar(configuration, externalTransformerPath);
                    log.debug("File 'transformer-config.xml' successfully added from transformer config jarPath: {}", externalTransformerPath);
                } else {
                    // add the transformer config file from default location inside this jarPath
                    addTransformerConfigFromJar(configuration, jarPath);
                    log.debug("File 'transformer-config.xml' successfully added from default location from jarPath: {}", jarPath);
                }
                configurations.add(configuration);
                log.info("Added network element configuration type:{} version:{}", networkElementId.getType(), networkElementId.getVersion());
            }
            return configurations;
        } catch (final Exception e) {
            throw new MediationConfigurationException("Error creating NetworkElementConfiguration from jarPath: " + jarPath, e);
        }
    }

    /**
     * Checks that the jar exists for the o1-mediation-config Xml file.
     *
     * @param jarPath
     *            the path to the jar.
     * @return
     */
    public boolean isO1MediationConfigurationJar(final Path jarPath) {
        try {
            final List<Path> entries = JarNioUtil.getJarEntries(jarPath, O1_CONFIG_FILE_FILTER);
            final boolean result = entries != null && !entries.isEmpty();
            if (result) {
                log.info("Found O1 jar with {}", O1_CONFIG_FILE_FILTER);
            }
            return result;
        } catch (final Exception | VirtualMachineError ex) {
            throw new MediationConfigurationException("Error verifying jar: " + jarPath, ex);
        }
    }

    private Path getTransformerConfigFromExternalJarPath(final NetworkElementConfig networkElementConfig, final Path dir) {
        log.debug("Adding 'transformer-config.xml' from external jars...");
        final String transformerStr = networkElementConfig.getTransformerConfig();
        return dir.resolve(Paths.get(transformerStr.trim()));
    }

    private void checkTypeAndVersion(final Path jarPath, final NetworkElementConfig networkElementConfig) {
        if (networkElementConfig.getType() == null) {
            throw new MediationConfigurationException("type is null processing jarPath: " + jarPath);
        }
        if (networkElementConfig.getVersion() == null) {
            throw new MediationConfigurationException("version is null processing jarPath: " + jarPath);
        }
    }

    private URL getConfigFileUrl(final Path jarPath) throws IOException {
        final List<Path> entries = JarNioUtil.getJarEntries(jarPath, O1_CONFIG_FILE_FILTER);
        if (entries == null || entries.size() != 1) {
            throw new MediationConfigurationException("Invalid mediation jarPath file. File META-INF/o1-mediation-config.xml not found in jarPath: "
                    + jarPath);
        }
        return JarNioUtil.toURL(entries.get(0));
    }

    private NetworkElementConfiguration addTransformerConfigFromJar(final NetworkElementConfiguration networkElementConfiguration, final Path jar) {
        try {
            final List<Path> entries = JarNioUtil.getJarEntries(jar, TRANSFORMER_CONFIG_FILE_FILTER);
            if (entries == null || entries.size() != 1) {
                throw new MediationConfigurationException(
                        "Invalid mediation set jar file. File META-INF/o1-mediation-config.xml not found in jar: " + jar);
            }
            final Path path = entries.get(0);
            networkElementConfiguration.setTransformerFile(JarNioUtil.toURL(path));
            log.debug("File 'transformer-config.xml' successfully added from jar file: {}", jar);
        } catch (final Exception ex) {
            throw new MediationConfigurationException("Error adding 'transformer-config.xml' from jar: " + jar, ex);
        }
        return networkElementConfiguration;
    }
}
