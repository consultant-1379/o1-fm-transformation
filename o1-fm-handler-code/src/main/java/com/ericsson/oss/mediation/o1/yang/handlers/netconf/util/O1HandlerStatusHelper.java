/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2024
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.mediation.o1.yang.handlers.netconf.util;

import static com.ericsson.oss.mediation.netconf.session.api.handler.NetconfSessionMediationHandlerHelper.getOperationsStatus;
import static com.ericsson.oss.mediation.netconf.session.api.handler.NetconfSessionMediationHandlerHelper.putOperationsStatusInHeader;

import java.util.Map;

import com.ericsson.oss.mediation.netconf.session.api.operation.NetconfSessionOperationsStatus;

/**
 * Class that provides a common method for updating the 'netconfSessionOperationsStatus' in the headers.
 */
public class O1HandlerStatusHelper {

    private O1HandlerStatusHelper() {
    }


    /**
     * Method to put the 'netconfSessionOperationsStatus' status in the headers, required for error propagation.
     *
     * @param operationStatus The O1NetconfSessionOperationStatus.
     * @param headers         The current headers.
     */
    public static void putHandlerStatusInHeaders(final O1NetconfSessionOperationStatus operationStatus,
                                                 final Map<String, Object> headers) {

        NetconfSessionOperationsStatus operationsStatus = getOperationsStatus(headers);
        if (operationsStatus == null) {
            operationsStatus = new NetconfSessionOperationsStatus();
            putOperationsStatusInHeader(operationsStatus, headers);
        }
        operationsStatus.addOperationStatus(operationStatus);
    }
}
