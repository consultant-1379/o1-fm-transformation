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

package com.ericsson.oss.mediation.o1.event.service;

import javax.inject.Inject;

import com.ericsson.oss.mediation.o1.event.jms.MessageSender;
import com.ericsson.oss.mediation.o1.event.model.O1Notification;
import com.ericsson.oss.mediation.o1.event.validator.O1NotificationValidator;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class O1ProcessingService {
    @Inject
    private O1NotificationValidator generalEventValidator;

    @Inject
    private MessageSender messageSender;

    public void processNotification(final O1Notification o1Notification) {
        generalEventValidator.validate(o1Notification);
        messageSender.send(o1Notification);
        log.debug("Notification sent Successfully");
    }
}
