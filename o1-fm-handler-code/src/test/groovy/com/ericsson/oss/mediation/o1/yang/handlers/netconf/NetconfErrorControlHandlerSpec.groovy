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
package com.ericsson.oss.mediation.o1.yang.handlers.netconf

import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.itpf.common.config.Configuration
import com.ericsson.oss.itpf.common.event.handler.EventHandlerContext
import com.ericsson.oss.itpf.common.event.handler.exception.EventHandlerException
import com.ericsson.oss.itpf.sdk.eventbus.model.EventSender
import com.ericsson.oss.itpf.sdk.recording.ErrorSeverity
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder
import com.ericsson.oss.mediation.flow.events.MediationComponentEvent
import com.ericsson.oss.mediation.fm.supervision.response.AlarmSupervisionResponse
import com.ericsson.oss.mediation.netconf.session.api.operation.NetconfSessionOperation
import com.ericsson.oss.mediation.netconf.session.api.operation.NetconfSessionOperationErrorCode
import com.ericsson.oss.mediation.netconf.session.api.operation.NetconfSessionOperationsStatus
import com.ericsson.oss.mediation.o1.yang.handlers.netconf.util.O1NetconfSessionOperationStatus

import javax.inject.Inject

import static com.ericsson.oss.mediation.fm.o1.common.util.FdnUtil.getParentFdn

class NetconfErrorControlHandlerSpec extends CdiSpecification {

    private static final String FM_ALARM_SUPERVISION_FDN = "NetworkElement=Node5G,FmAlarmSupervision=1"
    private static final String FM_SUPERVISION_FLOW = "//O1_MED/FmSupervisionFlow/1.0.0"

    private final Map<String, Object> headers = new HashMap<>()
    private EventHandlerContext mockEventHandlerContext = Mock()
    private Configuration mockConfiguration = Mock()

    @ObjectUnderTest
    private NetconfErrorControlHandler errorControlHandler

    @Inject
    private SystemRecorder systemRecorder

    @Inject
    private EventSender<AlarmSupervisionResponse> eventSender

    def setup() {
        mockEventHandlerContext.getEventHandlerConfiguration() >> mockConfiguration
        mockConfiguration.getAllProperties() >> headers
    }

    def "Test when no operation failures exist then no exception is thrown"() {

        given: "netconfSessionOperationsStatus property with no failures"
            headers.put('netconfSessionOperationsStatus', new NetconfSessionOperationsStatus())
        and: "a valid input event"
            def inputEvent = new MediationComponentEvent(headers, null)
            errorControlHandler.init(mockEventHandlerContext)

        when: "error control handler is invoked"
            errorControlHandler.onEventWithResult(inputEvent)

        then:
            noExceptionThrown()
    }

    def "Test when operation failure exists then exception is thrown and recorder is invoked and supervision response sent"() {

        given: "netconfSessionOperationsStatus property with failure"
            setOperationFailureInHeaders()
        and: "a valid input event"
            def inputEvent = new MediationComponentEvent(headers, null)
            errorControlHandler.init(mockEventHandlerContext)

        when: "error control handler is invoked"
            errorControlHandler.onEventWithResult(inputEvent)

        then: "exception is thrown"
            EventHandlerException exception = thrown()
            assert exception.message == "error"
        and: "system recorder error is logged"
            1 * systemRecorder.recordError("HandlerA", ErrorSeverity.ERROR, FM_SUPERVISION_FLOW, "GET", "error")
        and: "send alarm supervision response failed"
            1 * eventSender.send({ it.toString() == getAlarmSupervisionResponse().toString() })
    }

    def "Test when operation failure exists and supervision response fails to be sent then exception is still thrown"() {

        given: "netconfSessionOperationsStatus property with failure"
            setOperationFailureInHeaders()
        and: "alarm response event fails when sending"
            eventSender.send(_ as AlarmSupervisionResponse) >> { throw new RuntimeException("Error") }
        and: "a valid input event"
            def inputEvent = new MediationComponentEvent(headers, null)
            errorControlHandler.init(mockEventHandlerContext)

        when: "error control handler is invoked"
            errorControlHandler.onEventWithResult(inputEvent)

        then: "exception is thrown"
            EventHandlerException exception = thrown()
            assert exception.message == "error"
        and: "system recorder error is logged"
            1 * systemRecorder.recordError("HandlerA", ErrorSeverity.ERROR, FM_SUPERVISION_FLOW, "GET", "error")
    }

    def "Test when operation aborted exists then exception is not thrown"() {

        given: "netconfSessionOperationsStatus property with failure"
            setOperationAbortedInHeaders()
        and: "alarm response event fails when sending"
            eventSender.send(_ as AlarmSupervisionResponse) >> { throw new RuntimeException("Error") }
        and: "a valid input event"
            def inputEvent = new MediationComponentEvent(headers, null)
            errorControlHandler.init(mockEventHandlerContext)

        when: "error control handler is invoked"
            errorControlHandler.onEventWithResult(inputEvent)

        then:
            noExceptionThrown()
    }

    private void setOperationFailureInHeaders() {
        NetconfSessionOperationErrorCode errorCode = NetconfSessionOperationErrorCode.OPERATION_FAILED
        errorCode.setAdditionalErrorMessage("error")
        headers.put('netconfSessionOperationsStatus', createOperation("HandlerA", NetconfSessionOperation.GET, errorCode))
        setHeaders()
    }

    private void setOperationAbortedInHeaders() {
        NetconfSessionOperationErrorCode errorCode = NetconfSessionOperationErrorCode.OPERATION_ABORTED
        errorCode.setAdditionalErrorMessage("aborted")
        headers.put('netconfSessionOperationsStatus', createOperation("HandlerA", NetconfSessionOperation.GET, errorCode))
        setHeaders()
    }

    private void setHeaders() {
        headers.put("flowUrn", FM_SUPERVISION_FLOW)
        headers.put("nodeAddress", FM_ALARM_SUPERVISION_FDN)
        headers.put("active", true)
    }

    private AlarmSupervisionResponse getAlarmSupervisionResponse() {
        final AlarmSupervisionResponse alarmResponse = new AlarmSupervisionResponse()
        alarmResponse.setNodeFdn(getParentFdn(FM_ALARM_SUPERVISION_FDN))
        alarmResponse.setActive(true)
        alarmResponse.setResult("FAILURE")
        return alarmResponse
    }

    private NetconfSessionOperationsStatus createOperation(final String handler, final NetconfSessionOperation operation,
                                                           final NetconfSessionOperationErrorCode errorCode) {
        final O1NetconfSessionOperationStatus operationStatus = new O1NetconfSessionOperationStatus(handler, operation, null, errorCode, false)
        final NetconfSessionOperationsStatus netconfSessionOperationsStatus = new NetconfSessionOperationsStatus();
        netconfSessionOperationsStatus.addOperationStatus(operationStatus)
        return netconfSessionOperationsStatus
    }
}
