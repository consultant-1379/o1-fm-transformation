/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2024
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.oss.mediation.fm.o1.engine.nodesuspender;

import static com.ericsson.oss.itpf.sdk.recording.EventLevel.DETAILED;
import static com.ericsson.oss.mediation.fm.o1.common.Constants.COMMA_FM_ALARM_SUPERVISION_RDN;
import static com.ericsson.oss.mediation.fm.o1.common.Constants.NETWORK_ELEMENT_EQUALS;


import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.inject.Inject;

import com.ericsson.oss.itpf.sdk.eventbus.model.EventSender;
import com.ericsson.oss.itpf.sdk.eventbus.model.annotation.Modeled;
import com.ericsson.oss.itpf.sdk.recording.ErrorSeverity;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.mediation.core.events.MediationClientType;
import com.ericsson.oss.mediation.fm.o1.dps.DpsQuery;
import com.ericsson.oss.mediation.fm.o1.engine.nodesuspender.cache.O1NodeSuspenderCache;
import com.ericsson.oss.mediation.fm.o1.engine.nodesuspender.config.NodeSuspenderConfigurationListener;
import com.ericsson.oss.mediation.fm.o1.engine.service.FmEventBufferFacade;
import com.ericsson.oss.mediation.fm.util.EventNotificationUtil;
import com.ericsson.oss.mediation.sdk.event.MediationTaskRequest;
import com.ericsson.oss.mediation.translator.model.EventNotification;
import com.ericsson.oss.services.fm.service.model.FmMediationAlarmSyncRequest;

import lombok.extern.slf4j.Slf4j;

@Singleton
@Startup
@Slf4j
public class NodeSuspenderTimer {

    private static final boolean PERSISTENT_TIMER = true;
    private static final long INITIAL_DURATION_MSECS = 1000L;

    @Inject
    private TimerService timerService;

    @Inject
    private NodeSuspenderConfigurationListener listener;

    @Inject
    private O1NodeSuspenderCache o1NodeSuspenderCache;

    @Inject
    private FmEventBufferFacade fmEventBufferFacade;

    @Inject
    private SystemRecorder systemRecorder;

    @Inject
    @Modeled
    private EventSender<MediationTaskRequest> fmMediationAlarmSyncRequestSender;

    @Inject
    DpsQuery dpsQuery;

    @PostConstruct
    private void init() {
        long intervalInMinutes = listener.getAlarmRateCheckInterval();
        long intervalInMillis = intervalInMinutes * 60 * 1000L; // Convert minutes to milliseconds

        log.debug("Node suspender timer starting in post construct with interval {}", intervalInMinutes);

        timerService.createIntervalTimer(INITIAL_DURATION_MSECS, intervalInMillis,
                new TimerConfig("Timer for node suspender cache", PERSISTENT_TIMER));
    }

    @Timeout
    public void timeout(final Timer timer) {
        log.debug("Node suspender timer interval has elapsed, resetting the cache");

        processNetworkElementIdsBelowThreshold();
        o1NodeSuspenderCache.resetCountForAllNodes();

        log.debug("Node suspender timer time-out operations done");

    }

    private void processNetworkElementIdsBelowThreshold() {

        List<String> networkElementIdsBelowThreshold = getNetworkElementIdsBelowThreshold();

        o1NodeSuspenderCache.resetNodeSuspensionStatus(networkElementIdsBelowThreshold);

        clearNodeSuspendedAlarm(networkElementIdsBelowThreshold);
    }

    private List<String> getNetworkElementIdsBelowThreshold() {
        int alarmRateThreshold = listener.getAlarmRateNormalThreshold();
        log.debug("Alarm rate threshold is {} ", alarmRateThreshold);

        Map<String, Integer> suspendedNetworkElementIds = o1NodeSuspenderCache.getSuspendedIdDetails();
        log.debug("NetworkElementIds that are suspended {}", suspendedNetworkElementIds);

        final List<String> networkElementIdsBelowThreshold = suspendedNetworkElementIds.entrySet().stream()
                .filter(entry -> entry.getValue() < alarmRateThreshold)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        log.debug("NetworkElementIds that are below the suspension threshold {}", networkElementIdsBelowThreshold);
        return networkElementIdsBelowThreshold;
    }

    private void clearNodeSuspendedAlarm(List<String> networkElementIds) {
        log.debug("Clearing node suspended alarm for network element Ids: {}", networkElementIds);

        for (String networkElementId : networkElementIds) {
            final String networkElementFdn = NETWORK_ELEMENT_EQUALS + networkElementId;
            log.debug("Clearing node suspended alarm for node: {}", networkElementFdn);

            final EventNotification eventNotification =
                    EventNotificationUtil.createNodeSuspensedClearAlarm(networkElementFdn, "", "");
            fmEventBufferFacade.sendEvent(eventNotification);

            systemRecorder.recordEvent("FM_O1_ALARM_SERVICE", DETAILED, "", networkElementFdn,
                    "Clearing node suspended alarm for " + networkElementFdn);

            if (dpsQuery.isAutomaticSyncSet(networkElementFdn)) {
                sendAlarmSyncRequest(networkElementFdn);
            }
        }
    }

    private void sendAlarmSyncRequest(final String networkElementFdn) {
        try {
            FmMediationAlarmSyncRequest event = buildRequest(networkElementFdn);
            log.info("Sending FmMediationAlarmSyncRequest event: {} ", event);
            fmMediationAlarmSyncRequestSender.send(event);
        } catch (final Exception exception) {
            log.error("Error sending FmMediationAlarmSyncRequest ", exception);
            systemRecorder.recordError("O1AlarmService", ErrorSeverity.ERROR, networkElementFdn, "",
                    "Exception when sending FmMediationAlarmSyncRequest event [" + exception.getMessage() + "]");
        }
    }

    private FmMediationAlarmSyncRequest buildRequest(final String networkElementFdn) {
        final FmMediationAlarmSyncRequest event = new FmMediationAlarmSyncRequest();
        event.setClientType(MediationClientType.EVENT_BASED.name());
        event.setProtocolInfo("FM");
        event.setJobId(UUID.randomUUID().toString());
        event.setNodeAddress(networkElementFdn.concat(COMMA_FM_ALARM_SUPERVISION_RDN));
        return event;
    }

}
