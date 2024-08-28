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

package com.ericsson.oss.mediation.fm.o1.common;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Constants {

    // Model related constants
    public static final String FDN = "fdn";
    public static final String COMMA_FM_FUNCTION_RDN = ",FmFunction=1";
    public static final String COMMA_FM_ALARM_SUPERVISION_RDN = ",FmAlarmSupervision=1";
    public static final String ME_CONTEXT = "MeContext";
    public static final String NETWORK_ELEMENT = "NetworkElement";
    public static final String NETWORK_ELEMENT_EQUALS = "NetworkElement=";

    public static final String AUTOMATIC_SYNCHRONIZATION = "automaticSynchronization";

    // JMS related constants
    public static final String JMS = "JMS";
    public static final String OBJECT_MESSAGE = "ObjectMessage";

    // MediationTaskRequest constants
    public static final String ACTIVE = "active";
    public static final String CLIENT_TYPE = "clientType";
    public static final String EVENT_BASED = "EVENT_BASED";
    public static final String FLOW_URN = "flowUrn";
    public static final String NODE_ADDRESS = "nodeAddress";
    public static final String SUPERVISION_ATTRIBUTES = "supervisionAttributes";

    // Notification related constants
    public static final String HREF = "href";

    public static final String O1_NODE = "O1Node";

    public static final String SYNCHRONIZATION_ALARM = "SYNCHRONIZATION_ALARM";

}
