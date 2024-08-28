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
package com.ericsson.oss.mediation.o1.yang.handlers.netconf;

import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.util.O1HandlerStatusHelper.putHandlerStatusInHeaders;

import javax.inject.Inject;

import org.slf4j.Logger;

import com.ericsson.oss.itpf.common.event.ComponentEvent;
import com.ericsson.oss.itpf.common.event.handler.annotation.EventHandler;
import com.ericsson.oss.mediation.flow.events.MediationComponentEvent;
import com.ericsson.oss.mediation.netconf.session.api.operation.NetconfSessionOperation;
import com.ericsson.oss.mediation.netconf.session.api.operation.NetconfSessionOperationErrorCode;
import com.ericsson.oss.mediation.o1.yang.handlers.netconf.util.NetconfHelper;
import com.ericsson.oss.mediation.o1.yang.handlers.netconf.util.O1NetconfSessionOperationStatus;
import com.ericsson.oss.mediation.o1.yang.handlers.netconf.util.O1NetconfSessionOperationStatusBuilder;
import com.ericsson.oss.mediation.o1.yang.handlers.netconf.util.XPathFilter;
import com.ericsson.oss.mediation.util.netconf.api.NetconfManager;
import com.ericsson.oss.mediation.util.netconf.api.NetconfResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * This handler is used to support the 'idempotent' FmSupervisionFlow use case. For example, if the alarm supervision
 * controller sends an FmSupervisionRecoveryTaskRequest, this will trigger the FmSupervisionFlow again.
 * </p><p>
 * The handler checks if the HeartbeatControl MO is already created on the node. If it is found to exist, then the header
 * property 'O1NetconfSessionOperationStatus' is updated with an O1NetconfSessionOperationStatus with a boolean to indicate
 * that the HeartbeatControl is already created. Later, the handlers for creating the NtfSubscriptionControl and
 * HeartbeatControl MOs use this boolean property to decide whether to create those MOs or not. This prevents the flow
 * from failing.
 * </p>
 */
@Slf4j
@EventHandler
public class CheckSupervisionMosHandler extends AbstractEventInputHandler {

    private static final String HANDLER_NAME = CheckSupervisionMosHandler.class.getSimpleName();

    @Inject
    private NetconfHelper netconfHelper;


    @Override
    public ComponentEvent onEventWithResult(final Object inputEvent) {

        super.onEventWithResult(inputEvent);

        if (skipHandler()) {
            return new MediationComponentEvent(headers, null);
        }

        NetconfSessionOperationErrorCode netconfSessionOperationErrorCode = NetconfSessionOperationErrorCode.NONE;
        boolean heartbeatControlExists = false;
        NetconfManager netconfManager = null;
        NetconfResponse netconfResponse = null;

        try {
            netconfManager = netconfHelper.getNetconfManager(headers);
            netconfHelper.connect(netconfManager);
            netconfResponse = netconfHelper.readMo(netconfManager, getHeartbeatControlXpath());
            heartbeatControlExists = checkHeartbeatControlMoExists(netconfResponse);

        } catch (final Exception e) {
            netconfSessionOperationErrorCode = NetconfSessionOperationErrorCode.OPERATION_FAILED;
            netconfSessionOperationErrorCode.setAdditionalErrorMessage(e.getMessage());

        } finally {
            netconfHelper.disconnect(netconfManager);
            updateHeaderStatus(netconfSessionOperationErrorCode, heartbeatControlExists, netconfResponse);
        }
        return new MediationComponentEvent(headers, null);
    }

    private boolean skipHandler() {
        if (!isEventBasedClient()) {
            log.info("Skipping handler, input event is not from EVENT_BASED client.");
            return true;
        }
        if (isSupervisionDisabled()) {
            log.info("Skipping handler, supervision active is true.");
            return true;
        }
        return false;
    }

    private void updateHeaderStatus(final NetconfSessionOperationErrorCode netconfSessionOperationErrorCode, final boolean alreadyExists,
                                    final NetconfResponse netconfResponse) {
        final O1NetconfSessionOperationStatus operationStatus = new O1NetconfSessionOperationStatusBuilder()
                .operation(NetconfSessionOperation.GET)
                .handlerName(HANDLER_NAME)
                .errorCode(netconfSessionOperationErrorCode)
                .response(netconfResponse)
                .setHeartbeatControlMoAlreadyExists(alreadyExists)
                .build();
        putHandlerStatusInHeaders(operationStatus, headers);
    }

    /*
    If the HeartbeatControl MO exists on the node then the NtfSubscriptionControl MO does as well as it's the parent MO.
    If no HeartbeatControl MO exists on the node then an <rpc-error> response is returned.
     */
    private boolean checkHeartbeatControlMoExists(final NetconfResponse netconfResponse) {
        if (netconfResponse.isError()) {
            log.info("NtfSubscriptionControl and HeartbeatControl MO's do not exist, need to create them...");
            return false;
        }
        return true;
    }

    private static XPathFilter getHeartbeatControlXpath() {
        return new XPathFilter("/ManagedElement/NtfSubscriptionControl/HeartbeatControl");
    }

    @Override
    protected Logger getLogger() {
        return log;
    }
}
