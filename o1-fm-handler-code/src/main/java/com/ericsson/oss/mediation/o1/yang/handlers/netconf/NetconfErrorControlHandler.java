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

import static com.ericsson.oss.mediation.fm.o1.common.Constants.ACTIVE;
import static com.ericsson.oss.mediation.fm.o1.common.Constants.FLOW_URN;
import static com.ericsson.oss.mediation.fm.o1.common.Constants.NODE_ADDRESS;
import static com.ericsson.oss.mediation.fm.o1.common.util.AlarmSupervisionResponseHelper.createAlarmSupervisionResponseFailure;
import static com.ericsson.oss.mediation.netconf.session.api.handler.NetconfSessionMediationHandlerHelper.getOperationsStatus;

import javax.inject.Inject;

import org.slf4j.Logger;

import com.ericsson.oss.itpf.common.event.ComponentEvent;
import com.ericsson.oss.itpf.common.event.handler.annotation.EventHandler;
import com.ericsson.oss.itpf.common.event.handler.exception.EventHandlerException;
import com.ericsson.oss.itpf.sdk.eventbus.model.EventSender;
import com.ericsson.oss.itpf.sdk.eventbus.model.annotation.Modeled;
import com.ericsson.oss.mediation.flow.events.MediationComponentEvent;
import com.ericsson.oss.mediation.fm.o1.common.util.FdnUtil;
import com.ericsson.oss.mediation.fm.o1.handlers.util.Recorder;
import com.ericsson.oss.mediation.fm.supervision.response.AlarmSupervisionResponse;
import com.ericsson.oss.mediation.netconf.session.api.operation.NetconfSessionOperationErrorCode;
import com.ericsson.oss.mediation.netconf.session.api.operation.NetconfSessionOperationStatus;
import com.ericsson.oss.mediation.netconf.session.api.operation.NetconfSessionOperationsStatus;
import com.ericsson.oss.mediation.o1.yang.handlers.netconf.util.O1NetconfSessionOperationStatus;
import lombok.extern.slf4j.Slf4j;

/**
 * This handler checks for failed operations in the header property 'netconfSessionOperationsStatus'. If a failed
 * operation is found then the following is done:
 *
 * <ul>
 *   <li>The error is recorded using SystemRecorder</li>
 *   <li>An AlarmSupervisionResponse event is sent to FM indicating a supervision failure</li>
 *   <li>A EventHandlerException is thrown with the original exception message which will rollback any transactions
 *   and terminate the flow.</li>
 * </ul>
 * <p>
 * If no failed operations are found then the handler does nothing and the flow continues. An aborted operation is not
 * regarded as a failed operation.
 */
@Slf4j
@EventHandler
public class NetconfErrorControlHandler extends AbstractEventInputHandler {

    @Inject
    private Recorder recorder;

    @Inject
    @Modeled
    private EventSender<AlarmSupervisionResponse> alarmSupervisionResponseSender;

    @Override
    public ComponentEvent onEventWithResult(final Object inputEvent) {
        super.onEventWithResult(inputEvent);

        final O1NetconfSessionOperationStatus failedOperation = getFailedOperation(getOperationsStatus(headers));

        if (failedOperation != null) {
            recorder.recordError(failedOperation.getHandlerName(), getFlowUrn(), failedOperation.getNetconfSessionOperation().name(),
                    failedOperation.getNetconfSessionOperationErrorCode().getAdditionalErrorMessage());
            sendAlarmSupervisionResponse();
            throw new EventHandlerException(failedOperation.getNetconfSessionOperationErrorCode().getAdditionalErrorMessage());
        }

        log.info("No operations failed, no exception will be thrown.");
        return new MediationComponentEvent(headers, null);
    }

    private O1NetconfSessionOperationStatus getFailedOperation(final NetconfSessionOperationsStatus operationsStatus) {

        for (NetconfSessionOperationStatus operation : operationsStatus.getNetconfSessionOperationsStatus().values()) {
            if (operation.getNetconfSessionOperationErrorCode().equals(NetconfSessionOperationErrorCode.OPERATION_FAILED)) {
                return (O1NetconfSessionOperationStatus) operation;
            }
        }
        return null;
    }

    private void sendAlarmSupervisionResponse() {
        try {
            final String networkElementFdn = FdnUtil.getParentFdn((String) headers.get(NODE_ADDRESS));
            final AlarmSupervisionResponse response = createAlarmSupervisionResponseFailure(networkElementFdn, getActive());
            alarmSupervisionResponseSender.send(response);
            log.info("Sent alarm supervision response for failure: {}", response);
        } catch (final Exception exception) {
            log.error("Exception when sending alarm supervision response failure event {}", exception.getMessage());
        }
    }

    private boolean getActive() {
        return (boolean) headers.get(ACTIVE);
    }

    private String getFlowUrn() {
        return (String) headers.get(FLOW_URN);
    }

    @Override
    protected Logger getLogger() {
        return log;
    }
}
