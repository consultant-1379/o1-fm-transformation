
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

import java.util.Arrays;
import java.util.List;

import com.ericsson.oss.mediation.event.buffering.api.EventBuffer;
import com.ericsson.oss.mediation.event.buffering.impl.EventBufferSingleton;
import com.ericsson.oss.mediation.translator.model.EventNotification;
import com.ericsson.oss.mediation.translator.model.EventNotificationBatch;
import com.ericsson.oss.services.fm.service.util.EventNotificationConverter;

import lombok.extern.slf4j.Slf4j;

/**
 * Facade for the FM Common Event Buffering Service.
 */
@Slf4j
public class FmEventBufferFacade {

    private final EventBuffer eventBuffer = EventBufferSingleton.getInstance();

    /**
     * Sends the event to the FM Event Buffering service. The event will be then buffered and sent to the 'ClusteredFMMediationChannel'.
     *
     * @param eventNotification
     *            the event containing the details of the alarm
     */
    public void sendEvent(final EventNotification eventNotification) {
        log.debug("Sending event to FM [{}].", eventNotification.toString());
        final EventNotificationBatch eventBatch = EventNotificationConverter.serializeObject(Arrays.asList(eventNotification));
        eventBuffer.bufferAndSend(eventBatch);
    }

    public void sendEventList(final List<EventNotification> eventNotifications) {
        log.debug("Sending events to FM [{}].", eventNotifications.toString());
        final EventNotificationBatch eventBatch = EventNotificationConverter.serializeObject(eventNotifications);
        eventBuffer.bufferAndSend(eventBatch);
    }
}
