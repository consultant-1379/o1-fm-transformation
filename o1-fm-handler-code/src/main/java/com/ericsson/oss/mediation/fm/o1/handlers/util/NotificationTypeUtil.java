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

package com.ericsson.oss.mediation.fm.o1.handlers.util;

import lombok.experimental.UtilityClass;
import java.util.HashMap;

@UtilityClass
public class NotificationTypeUtil {

    public static HashMap<String, Boolean> notificationTypeSupportStatus;

    static {
        notificationTypeSupportStatus = new HashMap<>();
        notificationTypeSupportStatus.put(Notifications.NOTIFY_NEW_ALARM, true);
        notificationTypeSupportStatus.put(Notifications.NOTIFY_CLEARED_ALARM, true);
        notificationTypeSupportStatus.put(Notifications.NOTIFY_CHANGED_ALARM_GENERAL, true);
        notificationTypeSupportStatus.put(Notifications.NOTIFY_ALARM_LIST_REBUILT, true);
        notificationTypeSupportStatus.put(Notifications.NOTIFY_CHANGED_ALARM, true);
        notificationTypeSupportStatus.put(Notifications.NOTIFY_EVENT, false);
        notificationTypeSupportStatus.put(Notifications.NOTIFY_MOI_CREATION, false);
        notificationTypeSupportStatus.put(Notifications.NOTIFY_MOI_DELETION, false);
        notificationTypeSupportStatus.put(Notifications.NOTIFY_MOI_ATTRIBUTE_VALUE_CHANGES, false);
        notificationTypeSupportStatus.put(Notifications.NOTIFY_MOI_CHANGES, false);
        notificationTypeSupportStatus.put(Notifications.NOTIFY_CORRELATED_NOTIFICATION_CHANGED, false);
        notificationTypeSupportStatus.put(Notifications.NOTIFY_ACK_STATE_CHANGED, false);
        notificationTypeSupportStatus.put(Notifications.NOTIFY_COMMENTS, false);
        notificationTypeSupportStatus.put(Notifications.NOTIFY_POTENTIAL_FAULTY_ALARM_LIST, false);
    }

}
