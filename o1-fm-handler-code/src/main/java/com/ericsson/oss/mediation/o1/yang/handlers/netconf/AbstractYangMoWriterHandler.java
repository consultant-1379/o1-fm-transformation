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

import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.Constants.MANAGED_ELEMENT_ID;
import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.Constants.NE_TYPE;

import javax.inject.Inject;

import com.ericsson.oss.itpf.common.event.ComponentEvent;
import com.ericsson.oss.mediation.adapter.netconf.jca.xa.yang.provider.MO2EditConfigOperationConverter;
import com.ericsson.oss.mediation.adapter.netconf.jca.xa.yang.provider.YangEditConfigOperation;
import com.ericsson.oss.mediation.adapter.netconf.jca.xa.yang.provider.YangNetconfOperationResult;
import com.ericsson.oss.mediation.netconf.session.api.operation.NetconfSessionOperation;
import com.ericsson.oss.mediation.netconf.session.api.operation.NetconfSessionOperationErrorCode;
import com.ericsson.oss.mediation.o1.yang.handlers.netconf.util.ModelServiceUtils;
import com.ericsson.oss.mediation.o1.yang.handlers.netconf.util.TimedInstrumentedOperation;
import com.ericsson.oss.mediation.o1.yang.handlers.netconf.util.WriteSystemRecorder;
import com.ericsson.oss.mediation.util.netconf.api.Datastore;
import com.ericsson.oss.mediation.util.netconf.api.editconfig.Operation;

/**
 * Abstract handler that must be extended by O1 MO WRITE handlers.
 * <p>
 * The id of the ManagedElement MO that will be used to construct the FDN of the MO that the write operation will be performed on must be provided in
 * the header {@code managedlementId}'
 * <p>
 * The concrete class that extends this class is responsible for providing the FDN to be read by implementing the abstract method
 * {@code createFdnForCrudOperation}.
 */
public abstract class AbstractYangMoWriterHandler extends AbstractYangHandler {

    @Inject
    protected WriteSystemRecorder writeSystemRecorder;

    protected Operation operationType;

    protected AbstractYangMoWriterHandler() {
    }

    @Override
    public ComponentEvent onEvent(final ComponentEvent inputEvent) {

        final TimedInstrumentedOperation<Void> operation = new TimedInstrumentedOperation<Void>() {

            @Override
            public void preInstrumentation() {
                writeSystemRecorder.recordInvocationDetails(getHandlerCanonicalName(), getFdn(), inputEvent);
            }

            @Override
            public void performOperation() {
                NetconfSessionOperationErrorCode netconfSessionOperationErrorCode = NetconfSessionOperationErrorCode.NONE;

                String additionalErrorMessage = null;
                try {
                    AbstractYangMoWriterHandler.super.onEvent(inputEvent);
                    if (checkHeadersIfHandlerToBeSkipped(inputEvent)) {
                        netconfSessionOperationErrorCode = NetconfSessionOperationErrorCode.OPERATION_ABORTED;
                        printSkippedHandlerLogMessage();
                    } else {
                        final String managedElementId = (String) inputEvent.getHeaders().get(MANAGED_ELEMENT_ID);
                        createFdnForCrudOperation(managedElementId);

                        if (shouldExecuteOperation(inputEvent) && executeOperation(inputEvent) != YangNetconfOperationResult.OK) {
                            netconfSessionOperationErrorCode = NetconfSessionOperationErrorCode.OPERATION_FAILED;
                        }
                    }
                    inputEvent.getHeaders().put("dataStore", Datastore.valueOf("CANDIDATE"));
                } catch (final Exception e) {
                    netconfSessionOperationErrorCode = NetconfSessionOperationErrorCode.OPERATION_FAILED;
                    additionalErrorMessage = e.getMessage();

                    try {
                        getLogger().error(getHandlerCanonicalName() + " failed.", e);
                    } catch (final Exception e1) {
                        getLogger().warn("instrumentation failure", e1);
                    }
                } finally {
                    putHandlerStatusInHeaders(netconfSessionOperationErrorCode, null, additionalErrorMessage, NetconfSessionOperation.EDIT_CONFIG,
                            inputEvent.getHeaders());
                }
            }

            @Override
            public void postInstrumentation(final long operationProcessingTime) {
                getLogger().debug("inputEvent headers before returning = {}", inputEvent.getHeaders());

                writeSystemRecorder.recordTimeTaken(getHandlerCanonicalName(), getFdn(), operationProcessingTime, operationType.toString());
            }
        };

        operation.execute();

        return inputEvent;
    }

    @Override
    protected YangEditConfigOperation constructOperation() {
        getLogger().debug("header info is  : {}", getHeaderInfo());
        final String fdn = getHeaderInfo().getFdn();

        final String networkElementFdn = dpsRead.getNetworkElementFdn(fdn);
        final String neType = dpsRead.getAttributeValue(networkElementFdn, NE_TYPE);

        final boolean includeNameSpacePrefix = getHeaderInfo().getIncludeNsPrefix();
        final String nodeName = getHeaderInfo().getNodeName();
        final String nodeNamespace = getHeaderInfo().getNodeNamespace();

        final MO2EditConfigOperationConverter converter = new MO2EditConfigOperationConverter();
        converter.setFdn(fdn);
        converter.setNamespace(getHeaderInfo().getNameSpace());
        converter.setType(getHeaderInfo().getType());
        converter.setVersion(getHeaderInfo().getVersion());
        converter.setNodeType(neType);
        converter.setNamespacePrefix(includeNameSpacePrefix);
        converter.setnodeName(nodeName);
        converter.setnodeNameSpace(nodeNamespace);
        converter.setTarget(ModelServiceUtils.getTarget(fdn, neType));
        fillMoAttributes(converter);
        converter.setOperation(operationType);
        return converter.convert();
    }

    protected void fillMoAttributes(final MO2EditConfigOperationConverter converter) {
    }

}
