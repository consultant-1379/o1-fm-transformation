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
package com.ericsson.oss.mediation.fm.o1.engine.transform;

import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;
import javax.ejb.DependsOn;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;

/**
 * Main class that loads the installed transformer data at startup.
 */
@Slf4j
@Startup
@Singleton
@DependsOn("MediationConfigurationBean")
public class TransformManagerStartup {

    @Inject
    private TransformManager transformManager;

    private boolean initialized = false;

    @PostConstruct
    private void startUp(){
        log.info("Loading transformer config on startup");
        transformManager.initialize();
        initialized = true;
    }

    public boolean isInitialized() {
        return initialized;
    }
}
