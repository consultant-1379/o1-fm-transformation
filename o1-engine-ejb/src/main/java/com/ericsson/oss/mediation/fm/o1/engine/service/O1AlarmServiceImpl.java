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

package com.ericsson.oss.mediation.fm.o1.engine.service;

import static com.ericsson.oss.itpf.sdk.recording.EventLevel.DETAILED;
import static com.ericsson.oss.mediation.fm.o1.common.Constants.O1_NODE;
import static com.ericsson.oss.mediation.fm.o1.common.Constants.SYNCHRONIZATION_ALARM;

import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;
import javax.inject.Inject;

import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.mediation.fm.o1.engine.api.O1AlarmService;
import com.ericsson.oss.mediation.fm.o1.engine.instrumentation.O1EngineStatistics;
import com.ericsson.oss.mediation.fm.o1.engine.nodesuspender.Master;
import com.ericsson.oss.mediation.fm.o1.engine.nodesuspender.cache.O1NodeSuspenderCache;
import com.ericsson.oss.mediation.fm.o1.engine.nodesuspender.config.NodeSuspenderConfigurationListener;
import com.ericsson.oss.mediation.fm.o1.engine.transform.TransformManager;
import com.ericsson.oss.mediation.fm.util.EventNotificationUtil;
import com.ericsson.oss.mediation.translator.model.EventNotification;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Stateless
public class O1AlarmServiceImpl implements O1AlarmService {

    @Inject
    private FmEventBufferFacade fmEventBufferFacade;

    @Inject
    private TransformManager transformManager;

    @Inject
    private O1NodeSuspenderCache o1NodeSuspenderCache;

    @Inject
    private SystemRecorder systemRecorder;

    @Inject
    private NodeSuspenderConfigurationListener nodeSuspenderConfigurationListener;

    @Inject
    O1EngineStatistics o1EngineStatistics;

    @Override
    public void sendAlarm(final EventNotification eventNotification) {
        log.debug("Send event [{}]", eventNotification);
        fmEventBufferFacade.sendEvent(eventNotification);
        o1EngineStatistics.incrementTotalNoOfForwardedAlarmEventNotifications();
    }

    @Override
    public void sendAlarms(final List<EventNotification> eventNotifications) {
        log.debug("Send events [{}]", eventNotifications);
        fmEventBufferFacade.sendEventList(eventNotifications);
        o1EngineStatistics.incrementTotalNoOfForwardedSyncAlarmEventNotifications(eventNotifications.stream()
                .filter(eventNotification -> eventNotification.getRecordType().equals(SYNCHRONIZATION_ALARM))
                .count());
    }

    @Override
    public EventNotification translateAlarm(final Map<String, Object> alarm) {
        log.debug("Transform alarm data [{}]", alarm);
        return transformManager.transformAlarm(alarm);
    }

    @Override
    public List<EventNotification> translateAlarms(final List<Map<String, Object>> alarms, String meContextFdn) {
        log.debug("Transform alarm data [{}] for FDN [{}]", alarms, meContextFdn);
        return transformManager.transformAlarms(alarms, meContextFdn);
    }

    @Override
    public void raiseHeartbeatAlarm(final String networkElementFdn) {
        log.debug("Raising heartbeat alarm for node: {}", networkElementFdn);
        final EventNotification eventNotification = EventNotificationUtil.createHeartbeatAlarm(networkElementFdn, "", O1_NODE, networkElementFdn);
        fmEventBufferFacade.sendEvent(eventNotification);
        o1EngineStatistics.incrementTotalNoOfHeartbeatFailures();
        systemRecorder.recordEvent("FM_O1_ALARM_SERVICE", DETAILED, "", networkElementFdn,
                "Heartbeat Failed : Alarm raised for " + networkElementFdn);

    }

    @Override
    public void clearHeartbeatAlarm(final String networkElementFdn) {
        log.debug("Clearing heartbeat alarm for node: {}", networkElementFdn);
        final EventNotification eventNotification =
                EventNotificationUtil.createHeartbeatClearAlarm(networkElementFdn, "", O1_NODE, networkElementFdn);
        fmEventBufferFacade.sendEvent(eventNotification);
        o1EngineStatistics.decrementTotalNoOfHeartbeatFailures();
        systemRecorder.recordEvent("FM_O1_ALARM_SERVICE", DETAILED, "", networkElementFdn, "Clearing heartbeat alarm for " + networkElementFdn);

    }

    @Override
    public void updateNodeSuspenderCache(String networkElementId) {
        log.debug("Increment count for " + networkElementId);
        o1NodeSuspenderCache.updateNodeSuspenderCache(networkElementId);
    }

    @Override
    public void addToNodeSuspenderCache(final String networkElementId) {
        log.debug("Add node to NodeSuspenderCache: " + networkElementId);
        o1NodeSuspenderCache.addNodeToCache(networkElementId);
    }

    @Override
    public void removeFromNodeSuspenderCache(final String networkElementId) {
        log.debug("Remove node from NodeSuspenderCache: " + networkElementId);
        o1NodeSuspenderCache.removeNodeFromCache(networkElementId);
    }

    @Override
    @Master
    public void resetNodeSuspenderCache() {
        log.debug("Reset node suspender cache for all NEs ");
        o1NodeSuspenderCache.resetCountForAllNodes();
    }

    @Override
    public boolean isAlarmFlowRateEnabled() {
        return nodeSuspenderConfigurationListener.getAlarmRateFlowControl();
    }

    @Override
    public boolean isNodeSuspended(String networkElementId) {
        log.trace("Get Node suspension status: " + networkElementId);
        return o1NodeSuspenderCache.isNodeSuspended(networkElementId);
    }
}
