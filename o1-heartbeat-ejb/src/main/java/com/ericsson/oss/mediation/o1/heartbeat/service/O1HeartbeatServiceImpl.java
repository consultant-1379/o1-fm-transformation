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

package com.ericsson.oss.mediation.o1.heartbeat.service;

import javax.ejb.Stateless;
import javax.inject.Inject;

import com.ericsson.oss.mediation.o1.heartbeat.api.HeartbeatDetails;
import com.ericsson.oss.mediation.o1.heartbeat.api.O1HeartbeatService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Stateless
public class O1HeartbeatServiceImpl implements O1HeartbeatService {

    @Inject
    private O1HeartbeatCacheManager cacheManager;

    @Override
    public void putEntryInHeartbeatCache(final String networkElementFdn, final HeartbeatDetails heartbeatDetails) {
        log.debug("Received request to add entry in cache [{}]", heartbeatDetails);
        cacheManager.putEntry(networkElementFdn, heartbeatDetails);
    }

    @Override
    public void removeEntryFromHeartbeatCache(final String networkElementFdn) {
        log.debug("Received request to remove entry from cache for [{}]", networkElementFdn);
        cacheManager.removeEntry(networkElementFdn);
    }

    @Override
    public HeartbeatDetails getEntryFromHeartbeatCache(final String networkElementFdn) {
        log.debug("Received request to get entry from cache for [{}]", networkElementFdn);
        return cacheManager.getEntry(networkElementFdn);
    }

}
