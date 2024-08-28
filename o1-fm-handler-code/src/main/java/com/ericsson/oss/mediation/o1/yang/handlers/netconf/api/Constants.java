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

package com.ericsson.oss.mediation.o1.yang.handlers.netconf.api;

import java.util.Arrays;
import java.util.List;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class Constants {

    // logging related messages - Note the naming convention.
    // Constants used for SystemRecorder API eventId start with EVENT_ID
    // Constants used for SystemRecorder API messages start with MSG_
    // The format of EVENT ID should align with the SystemRecorder API documentation.
    public static final String EVENT_ID_FM_SUPERVISION_MISSING_DATA = "O1NODE_FM_SUPERVISION.MISSING_DATA";
    public static final String EVENT_ID_FM_SUPERVISION_CONNECTION_ERROR = "O1NODE_FM_SUPERVISION.NETCONF_CONNECTION_ERROR";
    public static final String EVENT_ID_FM_SUPERVISION_NULL_RESPONSE = "O1NODE_FM_SUPERVISION.NETCONF_NULL_RESPONSE";
    public static final String EVENT_ID_FM_SUPERVISION_PARSE_ERROR = "O1NODE_FM_SUPERVISION.PARSE_RESPONSE_ERROR";
    public static final String MSG_ALARM_LIST_TYPE_MISSING = "Failed to get the AlarmList MO from the node.";
    public static final String MSG_ALARM_RECORDS_CANNOT_BE_READ = "Internal error occurred, unable to retrieve AlarmList.alarmRecords";
    public static final String MSG_HANDLER_SKIPPED = "Netconf Session Handler:%s has been skipped.";
    public static final String MSG_NETCONF_NULL_RESULT = "The Netconf connection returned a null YangNetconfOperationResult.";
    public static final String MSG_NETCONF_SETUP_FAILED = "Netconf connection setup failed in previous handlers.";

    // Domain specific (e.g. headers etc)
    public static final String ACTIVE = "active";
    public static final String CREATE = "CREATE";
    public static final String DELETE = "DELETE";
    public static final int EVENT_LISTENER_PORT = 8099;
    public static final String EVENT_LISTENER_REST_URL = "/eventListener/v1";
    public static final String FDN = "fdn";
    public static final String HEARTBEAT_INTERVAL = "heartbeatinterval";
    public static final String HEARTBEAT_NTF_PERIOD = "heartbeatNtfPeriod";
    public static final String ID = "id";
    public static final String INCLUDE_NS_PREFIX = "setIncludeNsPrefix";
    public static final String MERGE = "MERGE";
    public static final List<String> NOTIFICATION_TYPES_SUPPORTED = Arrays.asList("notifyChangedAlarm", "notifyNewAlarm",
            "notifyChangedAlarmGeneral", "notifyClearedAlarm", "notifyAlarmListRebuilt");

    public static final String READ = "READ";
    public static final String SUPERVISION_ATTRIBUTES = "supervisionAttributes";

    // Model Related
    public static final String ALARM_LIST = "AlarmList";
    public static final String ALARM_LIST_RDN = "AlarmList=1";
    public static final String ALARM_RECORDS = "alarmRecords";
    public static final String HEARTBEAT_CONTROL = "HeartbeatControl";
    public static final String HEARTBEAT_CONTROL_RDN = "HeartbeatControl=1";
    public static final String MANAGED_ELEMENT = "ManagedElement";
    public static final String MANAGED_ELEMENT_ID = "ManagedElementId";
    public static final String ME_CONTEXT = "MeContext";
    public static final String NAMESPACE = "urn:3gpp:sa5:_3gpp-common-managed-element";
    public static final String NE_RELEASE_VERSION = "1.0"; // O1Node base model version - o1node-node-model
    public static final String NETWORK_ELEMENT = "NetworkElement";
    public static final String NETWORK_ELEMENT_REF = "networkElementRef";
    public static final String NE_TYPE = "neType";
    public static final String NODE_ROOT_REF = "nodeRootRef";
    public static final String NOTIFICATION_RECIPIENT_ADDRESS = "notificationRecipientAddress";
    public static final String NOTIFICATION_TYPES = "notificationTypes";
    public static final String NTF_SUBSCRIPTION_CONTROL = "NtfSubscriptionControl";
    public static final String NTF_SUBSCRIPTION_CONTROL_ID = "ENMFM";
    public static final String NTF_SUBSCRIPTION_CONTROL_RDN = "NtfSubscriptionControl=" + NTF_SUBSCRIPTION_CONTROL_ID;
    public static final String VERSION = "2023.2.14";
    public static final String VIP = "VIP";

    // attributes
    public static final String ADMINISTRATIVE_STATE = "administrativeState";

    // attribute values
    public static final String UNLOCKED = "UNLOCKED";

}
