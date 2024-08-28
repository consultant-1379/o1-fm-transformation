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

package com.ericsson.oss.mediation.o1.yang.handlers.netconf;

import static com.ericsson.oss.mediation.netconf.session.api.handler.NetconfSessionMediationHandlerHelper.getOperationsStatus;
import static com.ericsson.oss.mediation.netconf.session.api.handler.NetconfSessionMediationHandlerHelper.isNetconfSessionOperationFailed;
import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.Constants.ACTIVE;

import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;

import com.ericsson.oss.itpf.common.event.ComponentEvent;
import com.ericsson.oss.itpf.common.event.handler.EventHandlerContext;
import com.ericsson.oss.mediation.adapter.netconf.jca.api.operation.NetconfOperationConnection;
import com.ericsson.oss.mediation.adapter.netconf.jca.api.operation.NetconfOperationRequest;
import com.ericsson.oss.mediation.adapter.netconf.jca.api.operation.NetconfOperationResult;
import com.ericsson.oss.mediation.adapter.netconf.jca.xa.yang.provider.YangNetconfOperationResult;
import com.ericsson.oss.mediation.cm.handlers.datastructure.MoDataObject;
import com.ericsson.oss.mediation.fm.o1.dps.DpsRead;
import com.ericsson.oss.mediation.netconf.session.api.handler.AbstractNetconfSessionMediationHandler;
import com.ericsson.oss.mediation.netconf.session.api.operation.NetconfSessionOperation;
import com.ericsson.oss.mediation.netconf.session.api.operation.NetconfSessionOperationErrorCode;
import com.ericsson.oss.mediation.netconf.session.api.operation.NetconfSessionOperationStatus;
import com.ericsson.oss.mediation.netconf.session.api.operation.NetconfSessionOperationsStatus;
import com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.Constants;
import com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.HeaderInfo;
import com.ericsson.oss.mediation.o1.yang.handlers.netconf.exception.MOHandlerException;
import com.ericsson.oss.mediation.o1.yang.handlers.netconf.util.O1HandlerStatusHelper;
import com.ericsson.oss.mediation.o1.yang.handlers.netconf.util.O1NetconfSessionOperationStatus;
import com.ericsson.oss.mediation.o1.yang.handlers.netconf.util.O1NetconfSessionOperationStatusBuilder;
import com.ericsson.oss.mediation.o1.yang.handlers.netconf.util.TimedInstrumentedOperation;
import com.ericsson.oss.mediation.util.netconf.api.NetconfResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Abstract handler that must be extended by O1 CRUD handlers.
 */
@Slf4j
public abstract class AbstractYangHandler extends AbstractNetconfSessionMediationHandler {

    @Inject
    protected DpsRead dpsRead;

    protected HeaderInfo headerInfo;

    protected AbstractYangHandler() {
    }

    protected abstract void initialize(final Map<String, Object> allProperties);

    protected boolean checkHeadersIfHandlerToBeSkipped(final ComponentEvent inputEvent) {
        final NetconfSessionOperationsStatus operationsStatus = getOperationsStatus(inputEvent.getHeaders());

        return isToBeSkipped(operationsStatus.getFirstOperationInErrorStatus()) || isToBeSkipped(operationsStatus);
    }

    @Override
    protected boolean isToBeSkipped(final NetconfSessionOperationStatus operationStatus) {
        return isNetconfSessionOperationFailed(operationStatus);
    }

    protected void cleanup() {
        headerInfo = null;
    }

    protected HeaderInfo getHeaderInfo() {
        return headerInfo;
    }

    protected String getFdn() {
        return headerInfo.getFdn();
    }

    /**
     * Method to put Netconf operator status in Headers, required for Error Propagation.
     *
     * @param netconfSessionOperationErrorCode Error code for Netconf operation
     * @param netconfResponse                  Netconf Response of operation
     * @param additionalErrorMessage           Additional Error Message, if any
     * @param netconfSessionOperation          Netconf Operation
     * @param headers                          ComponentEvent Headers
     */
    protected void putHandlerStatusInHeaders(final NetconfSessionOperationErrorCode netconfSessionOperationErrorCode,
                                             final NetconfResponse netconfResponse, final String additionalErrorMessage,
                                             final NetconfSessionOperation netconfSessionOperation, final Map<String, Object> headers) {

        final NetconfSessionOperationErrorCode localNetconfSessionOperationErrorCode =
                netconfResponse != null && netconfResponse.isError() ? NetconfSessionOperationErrorCode.OPERATION_FAILED
                        : netconfSessionOperationErrorCode;

        localNetconfSessionOperationErrorCode.setAdditionalErrorMessage(additionalErrorMessage);
        final O1NetconfSessionOperationStatus operationStatus = new O1NetconfSessionOperationStatusBuilder()
                .operation(netconfSessionOperation)
                .handlerName(getHandlerName())
                .errorCode(localNetconfSessionOperationErrorCode)
                .response(netconfResponse)
                .build();
        O1HandlerStatusHelper.putHandlerStatusInHeaders(operationStatus, headers);
    }

    protected void printSkippedHandlerLogMessage() {
        getLogger().info(String.format(Constants.MSG_HANDLER_SKIPPED, getHandlerCanonicalName()));
    }

    @Override
    public void init(final EventHandlerContext ctx) {
        super.init(ctx);
        initialize(ctx.getEventHandlerConfiguration().getAllProperties());
    }

    @Override
    public void destroy() {
        log.trace("Destroy {}", this.getClass().getCanonicalName());
        cleanup();
    }

    @Override
    protected Logger getLogger() {
        return log;
    }

    @Override
    protected abstract String getHandlerName();

    protected static String toString(final MoDataObject moDataObject) {
        return "namespace= " + moDataObject.getNamespace() + "|type= " + moDataObject.getTypeName() + "|version= " + moDataObject.getVersion()
                + "|name= " + moDataObject.getName() + "|attrMap = " + moDataObject.getAttrMap();
    }

    protected boolean shouldExecuteOperation(final ComponentEvent inputEvent) throws MOHandlerException {
        return true;
    }

    protected YangNetconfOperationResult executeOperation(final ComponentEvent inputEvent) throws MOHandlerException {

        final NetconfOperationRequest operation = timedConstructOperation();

        final MOHandlerInputManager handlerInputManager = new MOHandlerInputManager();
        handlerInputManager.init(getHandlerCanonicalName(), inputEvent.getHeaders());
        handlerInputManager.validateHandlerAttributes();

        final NetconfOperationConnection netconfConnection = (NetconfOperationConnection) handlerInputManager.getNetconfManager();

        return (YangNetconfOperationResult) timedExecuteXAResourceOperation(netconfConnection, operation);

    }

    protected NetconfOperationRequest timedConstructOperation() throws MOHandlerException {

        final TimedInstrumentedOperation<NetconfOperationRequest> operation = new TimedInstrumentedOperation<NetconfOperationRequest>() {

            @Override
            public void performOperation() {
                setResult(constructOperation());
            }

            @Override
            public void postInstrumentation(final long operationProcessingTime) {

            }
        };

        operation.execute();

        if (operation.getSuppressedPerformOperationException() != null) {
            throw new MOHandlerException("CRUD config construction failed", operation.getSuppressedPerformOperationException());
        }

        return operation.getResult();
    }

    protected abstract NetconfOperationRequest constructOperation();

    protected NetconfOperationResult timedExecuteXAResourceOperation(final NetconfOperationConnection netconfConnection,
                                                                     final NetconfOperationRequest operation) throws MOHandlerException {

        final TimedInstrumentedOperation<NetconfOperationResult> timedOperation = new TimedInstrumentedOperation<NetconfOperationResult>() {
            @Override
            public void performOperation() {
                setResult(netconfConnection.executeXAResourceOperation(operation));
            }

            @Override
            public void postInstrumentation(final long operationProcessingTime) {

            }
        };

        timedOperation.execute();

        if (timedOperation.getSuppressedPerformOperationException() != null) {
            final Exception suppressedException = timedOperation.getSuppressedPerformOperationException();

            throw new MOHandlerException("CRUD config enqueue failed: " + suppressedException.getMessage(), suppressedException);
        }

        return timedOperation.getResult();

    }

    protected boolean isActive() {
        boolean active = false;
        if (headerInfo.getHeaders().containsKey("supervisionAttributes")) { // supervision based client
            final Map<String, Object> supervisionAttributes = (Map<String, Object>) headerInfo.getHeaders().get("supervisionAttributes");
            active = (boolean) supervisionAttributes.getOrDefault(ACTIVE, false);
        } else if (headerInfo.getHeaders().containsKey(ACTIVE)) { // event based client
            active = (boolean) headerInfo.getHeaders().getOrDefault(ACTIVE, false);
        }
        return active;
    }

    /**
     * Creates the FDN of the MO that the CRUD operation will be performed on.
     * <p>
     * The {@code managedElementId} must be provided in a header called "ManagedElementId".
     */
    protected abstract void createFdnForCrudOperation(final String managedElementId);
}
