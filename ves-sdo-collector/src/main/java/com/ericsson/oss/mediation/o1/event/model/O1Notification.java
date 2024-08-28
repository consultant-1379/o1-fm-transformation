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

package com.ericsson.oss.mediation.o1.event.model;

import java.util.HashMap;

import org.json.JSONObject;

import com.ericsson.oss.mediation.o1.event.utils.O1NotificationUtil;

import lombok.Getter;

@Getter
public class O1Notification {

    private final HashMap<String, Object> normalisedNotification = new HashMap<>();
    private final JSONObject notification;

    public O1Notification(final JSONObject notification) {
        this.notification = notification;
        O1NotificationUtil.normaliseO1Notification(notification.toMap(), normalisedNotification);
    }

    public HashMap<String, Object> getNormalisedNotification() {
        return normalisedNotification;
    }

    @Override
    public String toString() {
        return "O1Notification{" +
                "normalisedNotification=" + normalisedNotification +
                ", notification=" + notification +
                '}';
    }
}
