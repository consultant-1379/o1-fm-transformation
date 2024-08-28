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
public class HandlerMessages {

    public static final String FAILED_TO_GET_JMS_MESSAGE_FROM_INPUT = "Failed to retrieve JMS message from input object.";
    public static final String INVALID_INPUT_MISSING_JMS_MESSAGE = "Invalid inputEvent: does not contain JMS message.";
    public static final String INVALID_INPUT_JMS_MESSAGE_IS_NOT_OF_MAP_TYPE =
            "Invalid inputEvent: JMS message does not contain the expected Map type.";
    public static final String FAILED_INITIALIZE_NOTIFICATION_HANDLING = "Failed to initialize notification handling for node [%s]";
    public static final String FAILED_TO_SET_CURRENT_SERVICE_STATE = "Failed to set FmFunction currentServiceState to [%s] for node [%s]";
    public static final String SUPERVISION_FAILED_MSG = " Supervision Request Failed ";
    public static final String SELECTOR_SUPERVISION_FAILED_MSG = " Supervision Selector Failed ";
    public static final String UNSUPPORTED_NOTIFICATION = "Unsupported notification type";
    public static final String UNKNOWN_NOTIFICATION = "Unknown notification type";
    public static final String DN_PREFIX_MISSING = "dnPrefix is missing from href";

    public static final String SUPERVISION_STATUS_OR_OAM_INTERFACE_FOR_FM_NOT_FOUND =
            "Supervision status or oamInterfaceForFM was not found in the handler header data";

}
