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

import com.ericsson.oss.itpf.common.event.handler.exception.EventHandlerException;
import com.ericsson.oss.mediation.netconf.session.api.operation.NetconfSessionOperation;
import com.ericsson.oss.mediation.netconf.session.api.operation.NetconfSessionOperationErrorCode;
import com.ericsson.oss.mediation.util.netconf.api.NetconfResponse;

public class O1NetconfSessionOperationStatusBuilder {

    private String handlerName;
    private NetconfSessionOperationErrorCode errorCode;
    private NetconfResponse response;
    private NetconfSessionOperation operation;
    private boolean heartbeatControlMoAlreadyExists;

    public O1NetconfSessionOperationStatusBuilder() {
    }

    public O1NetconfSessionOperationStatusBuilder handlerName(final String handlerName) {
        this.handlerName = handlerName;
        return this;
    }

    public O1NetconfSessionOperationStatusBuilder errorCode(final NetconfSessionOperationErrorCode errorCode) {
        this.errorCode = errorCode;
        return this;
    }

    public O1NetconfSessionOperationStatusBuilder response(final NetconfResponse response) {
        this.response = response;
        return this;
    }

    public O1NetconfSessionOperationStatusBuilder operation(final NetconfSessionOperation operation) {
        this.operation = operation;
        return this;
    }

    public O1NetconfSessionOperationStatusBuilder setHeartbeatControlMoAlreadyExists(final boolean alreadyExists) {
        this.heartbeatControlMoAlreadyExists = alreadyExists;
        return this;
    }

    public O1NetconfSessionOperationStatus build() {
        checkRequiredProperties();
        return new O1NetconfSessionOperationStatus(handlerName, operation, response, errorCode, heartbeatControlMoAlreadyExists);
    }

    private void checkRequiredProperties() {
        if (handlerName == null) {
            throw new EventHandlerException("HandlerName is required");
        }
        if (operation == null) {
            throw new EventHandlerException("NetconfSessionOperation is required");
        }
        if (errorCode == null) {
            throw new EventHandlerException("NetconfSessionOperationErrorCode is required");
        }
    }
}
