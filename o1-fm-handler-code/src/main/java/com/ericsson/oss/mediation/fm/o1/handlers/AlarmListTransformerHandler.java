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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.ericsson.oss.itpf.common.event.handler.annotation.EventHandler;
import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;
import com.ericsson.oss.mediation.flow.events.MediationComponentEvent;
import com.ericsson.oss.mediation.fm.o1.common.util.FdnUtil;
import com.ericsson.oss.mediation.fm.o1.engine.api.O1AlarmService;
import com.ericsson.oss.mediation.fm.o1.handlers.util.EventNotificationUtil;
import com.ericsson.oss.mediation.translator.model.EventNotification;

import lombok.extern.slf4j.Slf4j;

/**
 * Handler translates the list of alarm objects received to an {@code EventNotification} and adds it to the payload of the
 * {@code MediationComponentEvent} sent to the next handler.
 */
@Slf4j
@EventHandler
public class AlarmListTransformerHandler extends AbstractFmMediationHandler {

    @EServiceRef
    private O1AlarmService alarmService;

    private static final String FDN = "fdn";
    private static final String SYNCHRONIZATION_STARTED = "SYNCHRONIZATION_STARTED";
    private static final String SYNCHRONIZATION_ENDED = "SYNCHRONIZATION_ENDED";

    @Override
    public Object onEventWithResult(final Object inputEvent) {
        super.onEventWithResult(inputEvent);

        if (payload instanceof List && containsInstancesOf((List<?>) payload, Map.class)) {
            @SuppressWarnings("unchecked")
            final String meContextFdn = FdnUtil.getMeContextFdn(getHeader(FDN));
            final List<EventNotification> eventNotifications = new ArrayList<>();
            eventNotifications.add(createSyncOngoingAlarm(SYNCHRONIZATION_STARTED, meContextFdn));
            eventNotifications.addAll(alarmService.translateAlarms((List<Map<String, Object>>) payload, meContextFdn));
            eventNotifications.add(createSyncOngoingAlarm(SYNCHRONIZATION_ENDED, meContextFdn));
            return new MediationComponentEvent(headers, eventNotifications);
        }
        return new MediationComponentEvent(headers, StringUtils.EMPTY);
    }

    private EventNotification createSyncOngoingAlarm(final String recType, final String fdn) {
        final EventNotification eventNotification = EventNotificationUtil.createSyncAlarm(fdn, recType);
        getLogger().debug("EventNotification ongoing sync alarm :{}", eventNotification);
        return eventNotification;
    }

    @Override
    protected Logger getLogger() {
        return log;
    }
}
