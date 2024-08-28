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

import javax.annotation.PostConstruct;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

/**
 * Loads the o1-mediation-config.xml configuration data on deployment of bean.
 */
@Singleton
@Startup
@Lock(LockType.READ)
@Slf4j
public class MediationConfigurationBean {

    @Inject
    private MediationConfiguration mediationConfiguration;

    @PostConstruct
    private void init() {
        mediationConfiguration.init();
    }

    public NetworkElementConfiguration getConfiguration(final NetworkElementId networkElementId) {
        log.info("Getting configuration for {}", networkElementId);
        return mediationConfiguration.getConfiguration(networkElementId);
    }

    public List<NetworkElementConfiguration> getConfigurations(){
        log.info("Getting configurations");
        return mediationConfiguration.getConfigurations();
    }

    public List<URL> getTransformerFileUrls(){
        return mediationConfiguration.getConfigurations()
                .stream()
                .map(NetworkElementConfiguration::getTransformerFile)
                .collect(Collectors.toList());
    }
}
