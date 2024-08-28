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

import com.ericsson.oss.mediation.netconf.session.api.operation.NetconfSessionOperation;
import com.ericsson.oss.mediation.netconf.session.api.operation.NetconfSessionOperationErrorCode;
import com.ericsson.oss.mediation.netconf.session.api.operation.NetconfSessionOperationStatus;
import com.ericsson.oss.mediation.util.netconf.api.NetconfResponse;

/**
 * O1 specific class that extends the NetconfSessionOperationStatus to provide additional information
 * to the SystemRecorder when logging error messages.
 */
public class O1NetconfSessionOperationStatus extends NetconfSessionOperationStatus {

    private final String handlerName;
    private final boolean heartbeatControlMoAlreadyExists;

    public O1NetconfSessionOperationStatus(final String handlerName, final NetconfSessionOperation netconfSessionOperation,
                                           final NetconfResponse netconfResponse, final NetconfSessionOperationErrorCode errorCode,
                                           final boolean heartbeatControlMoAlreadyExists) {
        super(netconfSessionOperation, errorCode, netconfResponse);
        this.handlerName = handlerName;
        this.heartbeatControlMoAlreadyExists = heartbeatControlMoAlreadyExists;
    }

    public String getHandlerName() {
        return handlerName;
    }

    public boolean heartbeatControlMoAlreadyExists() {
        return heartbeatControlMoAlreadyExists;
    }

    @Override
    public String toString() {
        return "O1NetconfSessionOperationStatus{" +
                "handlerName='" + handlerName + '\'' +
                ", heartbeatControlMoAlreadyExists=" + heartbeatControlMoAlreadyExists +
                "} " + super.toString();
    }
}
