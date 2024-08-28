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

import static com.ericsson.oss.mediation.netconf.session.api.operation.NetconfSessionOperation.GET;
import static com.ericsson.oss.mediation.netconf.session.api.operation.NetconfSessionOperationErrorCode.NONE;
import static com.ericsson.oss.mediation.netconf.session.api.operation.NetconfSessionOperationErrorCode.OPERATION_ABORTED;
import static com.ericsson.oss.mediation.netconf.session.api.operation.NetconfSessionOperationErrorCode.OPERATION_FAILED;
import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.Constants.EVENT_ID_FM_SUPERVISION_CONNECTION_ERROR;
import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.Constants.EVENT_ID_FM_SUPERVISION_NULL_RESPONSE;
import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.Constants.EVENT_ID_FM_SUPERVISION_PARSE_ERROR;
import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.Constants.MANAGED_ELEMENT_ID;
import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.Constants.MSG_NETCONF_NULL_RESULT;
import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.Constants.MSG_NETCONF_SETUP_FAILED;
import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.Constants.NE_TYPE;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import com.ericsson.oss.itpf.common.event.ComponentEvent;
import com.ericsson.oss.mediation.adapter.netconf.jca.xa.yang.provider.YangNetconfOperationResult;
import com.ericsson.oss.mediation.adapter.netconf.jca.xa.yang_read.provider.MO2GetConfigOperationConverter;
import com.ericsson.oss.mediation.adapter.netconf.jca.xa.yang_read.provider.YangGetConfigOperation;
import com.ericsson.oss.mediation.cm.handlers.datastructure.MoDataObject;
import com.ericsson.oss.mediation.netconf.session.api.operation.NetconfSessionOperationErrorCode;
import com.ericsson.oss.mediation.netconf.session.api.operation.NetconfSessionOperationsStatus;
import com.ericsson.oss.mediation.o1.yang.handlers.netconf.exception.MOHandlerException;
import com.ericsson.oss.mediation.o1.yang.handlers.netconf.parser.ParserData;
import com.ericsson.oss.mediation.o1.yang.handlers.netconf.parser.YangNetconfGetXmlParser;
import com.ericsson.oss.mediation.o1.yang.handlers.netconf.util.FdnUtil;
import com.ericsson.oss.mediation.o1.yang.handlers.netconf.util.ReadSystemRecorder;
import com.ericsson.oss.mediation.o1.yang.handlers.netconf.util.TimedInstrumentedOperation;
import com.ericsson.oss.mediation.util.netconf.api.NetconfResponse;

/**
 * Abstract handler that must be extended by O1 MO READ handlers.
 * <p>
 * The id of the ManagedElement MO that will be used to construct the FDN of the MO that the read operation will be performed on must be provided in
 * the header {@code managedlementId}'
 * <p>
 * The concrete class that extends this class is responsible for providing the FDN to be read by implementing the abstract method
 * {@code createFdnForCrudOperation}.
 */
public abstract class AbstractYangMoReadHandler extends AbstractYangHandler {

    protected static final String RESULT = "result";

    @Inject
    private YangNetconfGetXmlParser yangNetconfGetXmlParser;

    @Inject
    protected ReadSystemRecorder readSystemRecorder;

    protected AbstractYangMoReadHandler() {}

    protected void fillMoAttributes(final MO2GetConfigOperationConverter converter) {
        converter.setAttributes(getHeaderInfo().getReadAttributes());
    }

    @Override
    public ComponentEvent onEvent(final ComponentEvent inputEvent) {

        String managedElementId = (String) inputEvent.getHeaders().get(MANAGED_ELEMENT_ID);
        createFdnForCrudOperation(managedElementId);

        final TimedInstrumentedOperation<Void> operation = new TimedInstrumentedOperation<Void>() {

            @Override
            public void preInstrumentation() {
                readSystemRecorder.recordInvocationDetails(getHandlerCanonicalName(), getFdn(), inputEvent);
            }

            @Override
            public void performOperation() {
                NetconfSessionOperationErrorCode netconfSessionOperationErrorCode = NONE;

                String additionalErrorMessage = null;
                try {
                    AbstractYangMoReadHandler.super.onEvent(inputEvent);

                    if (checkHeadersIfHandlerToBeSkipped(inputEvent)) {
                        netconfSessionOperationErrorCode = OPERATION_ABORTED;
                        readSystemRecorder.recordError(EVENT_ID_FM_SUPERVISION_CONNECTION_ERROR, getHandlerName(),
                                headerInfo.getFdn(), MSG_NETCONF_SETUP_FAILED);
                    } else if (shouldExecuteOperation(inputEvent)) {
                        final YangNetconfOperationResult result = executeOperation(inputEvent);

                        if (isValid(result)) {
                            processNetconfResponse(inputEvent, result);
                        } else {
                            netconfSessionOperationErrorCode = OPERATION_FAILED;
                        }
                    }

                } catch (final Exception exception) {
                    netconfSessionOperationErrorCode = OPERATION_FAILED;
                    additionalErrorMessage = exception.getMessage();
                    readSystemRecorder.recordError(EVENT_ID_FM_SUPERVISION_CONNECTION_ERROR, getHandlerName(),
                            headerInfo.getFdn(), exception.getMessage());
                } finally {
                    putHandlerStatusInHeaders(netconfSessionOperationErrorCode, null,
                            additionalErrorMessage, GET, inputEvent.getHeaders());
                }
            }

            @Override
            public void postInstrumentation(final long operationProcessingTime) {
                getLogger().debug("inputEvent headers before returning = {}", inputEvent.getHeaders());
                readSystemRecorder.recordTimeTaken(getHandlerCanonicalName(), getFdn(), operationProcessingTime);
            }

            private boolean isValid(final YangNetconfOperationResult result) {
                if (result == null) {
                    readSystemRecorder.recordError(EVENT_ID_FM_SUPERVISION_NULL_RESPONSE, getHandlerName(),
                            headerInfo.getFdn(), MSG_NETCONF_NULL_RESULT);
                    return false;
                } else if (result.getResultCode() == YangNetconfOperationResult.YangNetconfOperationResultCode.ERROR) {
                    return false;
                }
                return true;
            }
        };

        operation.execute();

        return postOperationExecute(inputEvent);
    }

    protected abstract ComponentEvent postOperationExecute(final ComponentEvent inputEvent);

    @Override
    protected YangGetConfigOperation constructOperation() {
        getLogger().debug("headerinfo is  : {}", headerInfo);
        final MO2GetConfigOperationConverter converter = new MO2GetConfigOperationConverter();
        final String fdn = getHeaderInfo().getFdn();

        final String networkElementFdn = dpsRead.getNetworkElementFdn(fdn);
        final String neType = dpsRead.getAttributeValue(networkElementFdn, NE_TYPE);
        converter.setFdn(fdn);
        converter.setNamespace(getHeaderInfo().getNameSpace());
        converter.setType(getHeaderInfo().getType());
        converter.setVersion(getHeaderInfo().getVersion());
        converter.setNodeType(neType);

        fillMoAttributes(converter);
        return converter.convert();
    }

    protected boolean isFailedOperation(final ComponentEvent inputEvent) {
        final NetconfSessionOperationsStatus netconfSessionOperationsStatus =
                (NetconfSessionOperationsStatus) inputEvent.getHeaders().get("netconfSessionOperationsStatus");

        return netconfSessionOperationsStatus != null
                && netconfSessionOperationsStatus.getOperationStatus(GET).getNetconfSessionOperationErrorCode() != NONE;
    }

    protected Object getResult(final ComponentEvent inputEvent) {
        final Object response = inputEvent.getHeaders().get(RESULT);
        getLogger().debug("Response received from the node {}", response);
        return response;
    }

    @SuppressWarnings("unchecked")
    private void processNetconfResponse(final ComponentEvent inputEvent, final YangNetconfOperationResult yangNetconfOperationResult)
            throws MOHandlerException {
        final String managedElementFdn = FdnUtil.getManagedElementFdn(getHeaderInfo().getFdn());
        final String networkElementFdn = dpsRead.getNetworkElementFdn(getHeaderInfo().getFdn());
        final String neType = dpsRead.getAttributeValue(networkElementFdn, NE_TYPE);
        getLogger().debug("Initializing parser for managedElement FDN {} and neType {}", managedElementFdn, neType);

        yangNetconfGetXmlParser.init(managedElementFdn, neType);

        final String netconfResponse = ((NetconfResponse) yangNetconfOperationResult.getOperationResult()).getData();

        try {
            final ParserData parserData = yangNetconfGetXmlParser.parseXmlString(netconfResponse);
            final Map<String, MoDataObject> moDataObjects = parserData.getYangMOs();
            final Map<String, Object> attributes = readMoAttributes(moDataObjects);

            inputEvent.getHeaders().put(RESULT, attributes);

        } catch (final Exception ex) {
            String message = "Unable to parse response <" + netconfResponse + ">";
            readSystemRecorder.recordError(EVENT_ID_FM_SUPERVISION_PARSE_ERROR, getHandlerName(), headerInfo.getFdn(),
                    message);
            throw new MOHandlerException(message, ex);
        }
    }

    private Map<String, Object> readMoAttributes(final Map<String, MoDataObject> moDataObjects) {

        for (final MoDataObject moDataObject : moDataObjects.values()) {
            if (isMoRequested(moDataObject)) {
                return readAttrsForMoDataObject(moDataObject);
            }
        }

        return Collections.emptyMap(); // MO requested was not found in the response
    }

    private Map<String, Object> readAttrsForMoDataObject(final MoDataObject moDataObject) {
        final Map<String, Object> result = new HashMap<>();

        getLogger().debug("Processing MO {}", toString(moDataObject));

        final Map<String, Object> attributes = moDataObject.getAttrMap();
        final Collection<String> readAttributes = getHeaderInfo().getReadAttributes();

        for (final String readAttribute : readAttributes) {
            result.put(readAttribute, attributes.get(readAttribute));
        }

        return result;
    }

    /**
     * Returns true if this is the MO that the read request was performed for.
     */
    private boolean isMoRequested(final MoDataObject moDataObject) {
        return moDataObject.getTypeName().equals(getHeaderInfo().getType());
    }
}
