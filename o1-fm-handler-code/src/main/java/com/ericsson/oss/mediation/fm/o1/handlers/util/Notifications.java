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

@UtilityClass
public class Notifications {
    public static final String NOTIFY_NEW_ALARM = "notifyNewAlarm";
    public static final String NOTIFY_CLEARED_ALARM = "notifyClearedAlarm";
    public static final String NOTIFY_CHANGED_ALARM_GENERAL = "notifyChangedAlarmGeneral";
    public static final String NOTIFY_ALARM_LIST_REBUILT = "notifyAlarmListRebuilt";
    public static final String NOTIFY_CHANGED_ALARM = "notifyChangedAlarm";
    public static final String NOTIFY_EVENT = "notifyEvent";
    public static final String NOTIFY_MOI_CREATION = "notifyMOICreation";
    public static final String NOTIFY_MOI_DELETION = "notifyMOIDeletion";
    public static final String NOTIFY_MOI_ATTRIBUTE_VALUE_CHANGES = "notifyMOIAttributeValueChanges";
    public static final String NOTIFY_MOI_CHANGES = "notifyMOIChanges";
    public static final String NOTIFY_CORRELATED_NOTIFICATION_CHANGED = "notifyCorrelatedNotificationChanged";
    public static final String NOTIFY_ACK_STATE_CHANGED = "notifyAckStateChanged";
    public static final String NOTIFY_COMMENTS = "notifyComments";
    public static final String NOTIFY_POTENTIAL_FAULTY_ALARM_LIST = "notifyPotentialFaultyAlarmList";

}
