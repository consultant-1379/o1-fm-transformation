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

import static com.ericsson.oss.itpf.sdk.recording.EventLevel.DETAILED;
import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.Constants.ID;
import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.Constants.MANAGED_ELEMENT;
import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.Constants.MANAGED_ELEMENT_ID;
import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.util.O1HandlerStatusHelper.putHandlerStatusInHeaders;

import java.util.Map;

import javax.inject.Inject;

import org.json.XML;
import org.slf4j.Logger;

import com.ericsson.oss.itpf.common.event.ComponentEvent;
import com.ericsson.oss.itpf.common.event.handler.annotation.EventHandler;
import com.ericsson.oss.itpf.common.event.handler.exception.EventHandlerException;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.mediation.flow.events.MediationComponentEvent;
import com.ericsson.oss.mediation.netconf.session.api.operation.NetconfSessionOperation;
import com.ericsson.oss.mediation.netconf.session.api.operation.NetconfSessionOperationErrorCode;
import com.ericsson.oss.mediation.o1.yang.handlers.netconf.util.LoggingUtil;
import com.ericsson.oss.mediation.o1.yang.handlers.netconf.util.NetconfHelper;
import com.ericsson.oss.mediation.o1.yang.handlers.netconf.util.O1NetconfSessionOperationStatus;
import com.ericsson.oss.mediation.o1.yang.handlers.netconf.util.O1NetconfSessionOperationStatusBuilder;
import com.ericsson.oss.mediation.o1.yang.handlers.netconf.util.XPathFilter;
import com.ericsson.oss.mediation.util.netconf.api.NetconfManager;
import com.ericsson.oss.mediation.util.netconf.api.NetconfResponse;

import lombok.extern.slf4j.Slf4j;

/**
 * Handler to read the ManagedElement id from the node using a netconf GET call. The handling of the netconf connection and disconnection is done
 * within this handler as it does not extend the AbstractYangHandler class.
 * <br><br>
 * This handler is made to behave like the other handlers that extend the AbstractYangHandler so if an exception occurs it is caught and the header
 * property 'netconfSessionOperationsStatus' is updated to indicate to the other handlers that a netconf handler has failed, so they will not execute
 * and eventually the NetconfSessionReleaseHandler releases the session. Then the NetconfErrorControlHandler determines whether to allow the flow
 * continue or not.
 * <br><br>
 * Note if an exception does occur, it must be set the NetconfSessionOperationErrorCode.setAdditionalErrorMessage() so that the error message can be
 * rethrown by the NetconfErrorControlHandler later on.
 */
@Slf4j
@EventHandler
public class ReadManagedElementIdHandler extends AbstractEventInputHandler {

    private static final String HANDLER_NAME = ReadManagedElementIdHandler.class.getSimpleName();

    @Inject
    private SystemRecorder systemRecorder;

    @Inject
    private NetconfHelper netconfHelper;

    @Override
    public ComponentEvent onEventWithResult(final Object inputEvent) {

        NetconfSessionOperationErrorCode netconfSessionOperationErrorCode = NetconfSessionOperationErrorCode.NONE;
        NetconfManager netconfManager = null;
        NetconfResponse netconfResponse = null;

        try {
            super.onEventWithResult(inputEvent);
            netconfManager = netconfHelper.getNetconfManager(headers);
            netconfHelper.connect(netconfManager);

            netconfResponse = netconfHelper.readMo(netconfManager, getManagedElementXpathFilter());
            final String managedElementId = extractId(netconfResponse);

            headers.put(MANAGED_ELEMENT_ID, managedElementId);
            log.info("Read managedElementId successful, [{}]", managedElementId);

        } catch (final Exception e) {
            log.error("Read managedElementId from node failed, [{}]", e);
            netconfSessionOperationErrorCode = NetconfSessionOperationErrorCode.OPERATION_FAILED;
            netconfSessionOperationErrorCode.setAdditionalErrorMessage(e.getMessage());

        } finally {
            netconfHelper.disconnect(netconfManager);
            updateHeaderStatus(netconfSessionOperationErrorCode, netconfResponse);
        }

        return new MediationComponentEvent(headers, null);
    }

    private void updateHeaderStatus(final NetconfSessionOperationErrorCode netconfSessionOperationErrorCode, final NetconfResponse netconfResponse) {
        final O1NetconfSessionOperationStatus operationStatus = new O1NetconfSessionOperationStatusBuilder()
                .operation(NetconfSessionOperation.GET)
                .handlerName(HANDLER_NAME)
                .errorCode(netconfSessionOperationErrorCode)
                .response(netconfResponse)
                .build();
        putHandlerStatusInHeaders(operationStatus, headers);
    }

    private String extractId(final NetconfResponse netconfResponse) {
        if (netconfResponse == null) {
            throw new EventHandlerException("Netconf GET response was null");
        }

        if (netconfResponse.getData().isEmpty() || netconfResponse.isError()) {
            throw new EventHandlerException("Error - netconf GET response was: " + netconfResponse);
        }

        systemRecorder.recordEvent(LoggingUtil.convertToRecordingFormat(getHandlerCanonicalName()), DETAILED,
                "Read managed element id", "", "Response received from the node: " + netconfResponse.getData());

        final Map<String, Object> responseAsMap = XML.toJSONObject(netconfResponse.getData()).toMap();
        final Object managedElementId = ((Map<String, Object>) responseAsMap.get(MANAGED_ELEMENT)).get(ID);

        if (managedElementId == null) {
            throw new EventHandlerException("No managedElementId found on the node");
        }

        return managedElementId instanceof String
                ? (String) managedElementId
                : String.valueOf(managedElementId);
    }

    @Override
    protected Logger getLogger() {
        return log;
    }

    private static XPathFilter getManagedElementXpathFilter() {
        return new XPathFilter(new StringBuilder().append("/ManagedElement/attributes").toString());
    }
}
