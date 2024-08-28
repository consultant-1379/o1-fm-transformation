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

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

import com.ericsson.oss.itpf.common.event.handler.annotation.EventHandler;
import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;
import com.ericsson.oss.mediation.flow.events.MediationComponentEvent;
import com.ericsson.oss.mediation.fm.o1.engine.api.O1AlarmService;
import com.ericsson.oss.mediation.translator.model.EventNotification;

import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Handler extracts the {@code EventNotification} received and sends it to FM via the {@code O1AlarmService}.
 */
@Slf4j
@EventHandler
public class EventNotificationDispatchHandler extends AbstractFmMediationHandler {

    @EServiceRef
    private O1AlarmService alarmService;

    @Override
    public Object onEventWithResult(final Object inputEvent) {
        super.onEventWithResult(inputEvent);
        if (payload instanceof EventNotification) {
            log.debug("Sending alarm");
            alarmService.sendAlarm((EventNotification) payload);
        }

        if (payload instanceof List && containsInstancesOf((List<?>)payload,EventNotification.class)){
            log.debug("Sending alarm list");
            alarmService.sendAlarms((List<EventNotification>) payload);
        }
        return new MediationComponentEvent(headers, StringUtils.EMPTY);
    }

    @Override
    protected Logger getLogger() {
        return log;
    }

}
