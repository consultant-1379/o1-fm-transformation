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

package com.ericsson.oss.mediation.fm.o1.handlers;

import static com.ericsson.oss.mediation.fm.o1.handlers.util.Attributes.HEARTBEAT_TIMEOUT;

import java.time.Instant;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

import com.ericsson.oss.itpf.common.event.handler.annotation.EventHandler;
import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;
import com.ericsson.oss.mediation.flow.events.MediationComponentEvent;
import com.ericsson.oss.mediation.fm.o1.common.util.FdnUtil;
import com.ericsson.oss.mediation.o1.heartbeat.api.HeartbeatDetails;
import com.ericsson.oss.mediation.o1.heartbeat.api.O1HeartbeatService;

import lombok.extern.slf4j.Slf4j;

/**
 * Handler responsible for managing heartbeat of the NetworkElement.
 * <p>
 * Turning on the supervision for the O1Node, will start monitoring the heartbeat and turning off the supervision will remove the monitoring of the
 * heartbeat.
 */
@Slf4j
@EventHandler
public class HeartbeatTimerHandler extends AbstractFmMediationHandler {

    private static final String HANDLER = HeartbeatTimerHandler.class.getSimpleName();

    @EServiceRef
    private O1HeartbeatService o1HeartbeatService;

    @Override
    public Object onEventWithResult(final Object inputEvent) {

        super.onEventWithResult(inputEvent);

        final String networkElementFdn = FdnUtil.getParentFdn(getHeaderFdn());

        final int heartbeatTimeout = getHeader(HEARTBEAT_TIMEOUT);
        if (isSupervisionActive()) {

            getRecorder().recordEvent(HANDLER, getHeaderFdn(), networkElementFdn, "Adding heartbeat timer for " + networkElementFdn);
            HeartbeatDetails heartbeatdetails = new HeartbeatDetails();
            heartbeatdetails.setLastHeartbeatRecievedEpochTimestamp(Instant.now().getEpochSecond());
            heartbeatdetails.setHeartbeatTimeout(heartbeatTimeout);
            o1HeartbeatService.putEntryInHeartbeatCache(networkElementFdn, heartbeatdetails);
        } else {
            getRecorder().recordEvent(HANDLER, getHeaderFdn(), networkElementFdn, "Removing heartbeat timer for " + networkElementFdn);
            o1HeartbeatService.removeEntryFromHeartbeatCache(networkElementFdn);
        }
        return new MediationComponentEvent(headers, StringUtils.EMPTY);
    }

    @Override
    protected Logger getLogger() {
        return log;
    }
}
