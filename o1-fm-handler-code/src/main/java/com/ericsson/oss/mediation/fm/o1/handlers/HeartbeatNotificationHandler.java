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

import java.time.Instant;
import java.util.HashMap;

import com.ericsson.oss.mediation.fm.o1.instrumentation.O1HandlerStatistics;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.ericsson.oss.itpf.common.event.handler.annotation.EventHandler;
import com.ericsson.oss.itpf.common.event.handler.exception.EventHandlerException;
import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;
import com.ericsson.oss.mediation.flow.events.MediationComponentEvent;
import com.ericsson.oss.mediation.fm.o1.common.Constants;
import com.ericsson.oss.mediation.fm.o1.common.util.FdnUtil;
import com.ericsson.oss.mediation.fm.o1.common.util.HrefUtil;
import com.ericsson.oss.mediation.fm.o1.engine.api.O1AlarmService;
import com.ericsson.oss.mediation.o1.heartbeat.api.HeartbeatDetails;
import com.ericsson.oss.mediation.o1.heartbeat.api.O1HeartbeatService;

import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;

/**
 * Handler responsible for receiving messages from the 'O1FmHeartbeat' queue. It extracts the object from the message,
 * and then verifies if contains the "href" field.
 * <p>
 * The 'href' is used to extract the MeContext FDN i.e. the dnPrefix. The NetworkElement FDN is constructed using the
 * id of the MeContext FDN. The NetworkElement FDN is used to fetch the heartbeatDetails,
 * and these are both then used to remove the old heartbeat timer, and create a new one.
 * </p>
 * <p>
 * This Handler must be triggered as follows:
 * <ul>
 * <li>via JMS messages received on the 'O1FmHeartbeat' queue.</li>
 * </ul>
 * </p>
 * <ul>
 * In order to terminate the flow, an EventHandlerException will be thrown if an exception occurs in this handler.
 * </ul>
 */
@Slf4j
@EventHandler
public class HeartbeatNotificationHandler extends AbstractFmMediationHandler {

    private static final String HANDLER = HeartbeatNotificationHandler.class.getSimpleName();

    @EServiceRef
    private O1AlarmService o1AlarmService;

    @EServiceRef
    private O1HeartbeatService o1HeartbeatService;

    @Inject
    O1HandlerStatistics o1HandlerStatistics;

    @Override
    public Object onEventWithResult(final Object inputEvent) {
        o1HandlerStatistics.incrementTotalNoOfHeartbeatsReceived();

        super.onEventWithResult(inputEvent);
        final String networkElementFdn = getNetworkElementFdn();

        try {
            HeartbeatDetails heartbeatDetails = o1HeartbeatService.getEntryFromHeartbeatCache(networkElementFdn);
            if (heartbeatDetails.isHeartbeatAlarmRaised()) {
                getLogger().debug("Heartbeat resumed for {}, alarm will be cleared.", networkElementFdn);
                o1AlarmService.clearHeartbeatAlarm(networkElementFdn);
                heartbeatDetails.setHeartbeatAlarmRaised(false);
            }
            heartbeatDetails.setLastHeartbeatRecievedEpochTimestamp(Instant.now().getEpochSecond());
            o1HeartbeatService.putEntryInHeartbeatCache(networkElementFdn, heartbeatDetails);

        } catch (final Exception e) {
            getRecorder().recordError("HeartbeatNotificationHandler", "", "", "Could not remove or add heartbeat timer for node");
            throw new EventHandlerException("Could not remove or add heartbeat timer for node " + networkElementFdn, e);
        }
        return new MediationComponentEvent(headers, "");
    }

    @Override
    protected Logger getLogger() {
        return log;
    }

    private String getNetworkElementFdn() {
        final HashMap<String, Object> alarmObject = getNotificationObject();
        final String hrefValue = (String) alarmObject.get(Constants.HREF);

        final String dnPrefix = HrefUtil.extractDnPrefix(hrefValue);
        if (StringUtils.isBlank(dnPrefix)) {
            getRecorder().recordError(HANDLER, "", "",
                    "Unable to associate a notifyHeartbeat to a network element as the NE FDN could not be retrieved from the href attribute. The notification will be discarded.");
            throw new EventHandlerException("dnPrefix was null or empty.");
        }
        return FdnUtil.getNetworkElementFdn(dnPrefix);
    }

}
