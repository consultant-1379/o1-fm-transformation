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

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

import com.ericsson.oss.itpf.common.event.handler.annotation.EventHandler;
import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;
import com.ericsson.oss.mediation.flow.events.MediationComponentEvent;
import com.ericsson.oss.mediation.fm.o1.engine.api.O1AlarmService;
import com.ericsson.oss.mediation.translator.model.EventNotification;

import lombok.extern.slf4j.Slf4j;

/**
 * Handler translates the alarm object received to an {@code EventNotification} and adds it to the payload of the {@code MediationComponentEvent} sent
 * to the next handler.
 */
@Slf4j
@EventHandler
public class AlarmTransformerHandler extends AbstractFmMediationHandler {

    @EServiceRef
    private O1AlarmService alarmService;

    @Override
    public Object onEventWithResult(final Object inputEvent) {
        log.debug("Received event of type {} ", inputEvent.getClass().getName());
        super.onEventWithResult(inputEvent);
        if (payload instanceof Map<?, ?>) {
            log.debug("Translating alarm");
            final EventNotification eventNotification = alarmService.translateAlarm((Map<String, Object>) payload);
            log.debug("Translating alarm done {}", eventNotification);
            return new MediationComponentEvent(headers, eventNotification);
        }
        return new MediationComponentEvent(headers, StringUtils.EMPTY);
    }

    @Override
    protected Logger getLogger() {
        return log;
    }

}
