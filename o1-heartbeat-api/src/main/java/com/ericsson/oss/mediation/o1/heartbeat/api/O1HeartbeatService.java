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

package com.ericsson.oss.mediation.o1.heartbeat.api;

import javax.ejb.Remote;

import com.ericsson.oss.itpf.sdk.core.annotation.EService;

/**
 * Service that provides HeartBeat Management for O1Node types.
 */
@EService
@Remote
public interface O1HeartbeatService {

    /**
     * Sends a request to put the cache for {@code networkElementFdn} provided.
     *
     * @param networkElementFdn
     *            The FDN of the nodes NetworkElement MO
     * @param heartbeatDetails
     *            {@code HeartbeatDetails} for particular Network element
     */
    void putEntryInHeartbeatCache(final String networkElementFdn, final HeartbeatDetails heartbeatDetails);

    /**
     * Sends a request to remove the entry from the cache for {@code networkElementFdn} provided.
     *
     * @param networkElementFdn
     *            The FDN of the nodes NetworkElement MO
     */
    void removeEntryFromHeartbeatCache(final String networkElementFdn);

    /**
     * Retrieve the entry from the cache for the {@code networkElementFdn} provided.
     *
     * @param networkElementFdn
     *            The FDN of the nodes NetworkElement MO
     */
    HeartbeatDetails getEntryFromHeartbeatCache(final String networkElementFdn);

}
