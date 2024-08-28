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
import com.ericsson.oss.itpf.sdk.core.retry.RetriableCommand
import com.ericsson.oss.itpf.sdk.core.retry.RetryManager
import com.ericsson.oss.itpf.sdk.core.retry.RetryPolicy
import com.ericsson.oss.mediation.flow.events.MediationComponentEvent
import com.ericsson.oss.mediation.netconf.session.api.operation.NetconfSessionOperation
import com.ericsson.oss.mediation.netconf.session.api.operation.NetconfSessionOperationErrorCode
import com.ericsson.oss.mediation.netconf.session.api.operation.NetconfSessionOperationsStatus
import com.ericsson.oss.mediation.o1.yang.handlers.netconf.util.O1NetconfSessionOperationStatus
import com.ericsson.oss.mediation.util.netconf.api.NetconfManager
import com.ericsson.oss.mediation.util.netconf.api.NetconfResponse
import com.ericsson.oss.mediation.util.netconf.api.exception.NetconfManagerException

import javax.inject.Inject

import static com.ericsson.oss.mediation.util.netconf.api.NetconfConnectionStatus.CONNECTED

class CheckSupervisionMosHandlerSpec extends CdiSpecification {

    private final Map<String, Object> headers = new HashMap<>()
    private final NetconfResponse netconfResponse = new NetconfResponse();

    private EventHandlerContext mockEventHandlerContext = Mock()
    private Configuration mockConfiguration = Mock()

    @ObjectUnderTest
    private CheckSupervisionMosHandler checkSupervisionMosHandler

    @Inject
    private NetconfManager mockNetconfManager

    @Inject
    private RetryManager mockRetryManager

    def setup() {
        mockEventHandlerContext.getEventHandlerConfiguration() >> mockConfiguration
        mockConfiguration.getAllProperties() >> headers
        mockNetconfManager.getStatus() >>> [CONNECTED]
    }

    def "Test invocation from supervision client"() {
        given: "A supervision client request"
            headers.put("clientType", "SUPERVISION")
            def inputEvent = new MediationComponentEvent(headers, null)

        and: "handler is initialized"
            checkSupervisionMosHandler.init(mockEventHandlerContext)

        when: "handler is invoked"
            checkSupervisionMosHandler.onEventWithResult(inputEvent)

        then: "it does not execute the handler logic"
            0 * mockNetconfManager._
    }

    def "Test invocation from event based client and active is false"() {
        given: "An event based client request"
            headers.put("clientType", "EVENT_BASED")
            headers.put("active", false)
            def inputEvent = new MediationComponentEvent(headers, null)

        and: "handler is initialized"
            checkSupervisionMosHandler.init(mockEventHandlerContext)

        when: "handler is invoked"
            checkSupervisionMosHandler.onEventWithResult(inputEvent)

        then: "it does not execute the handler logic"
            0 * mockNetconfManager._
    }

    def "Test invocation from event based client, active is true and heartbeat control MO exists"() {
        given: "An event based client request"
            headers.put("clientType", "EVENT_BASED")
            headers.put("active", true)
            headers.put('netconfManager', mockNetconfManager)
            def inputEvent = new MediationComponentEvent(headers, null)

        and: "handler is initialized"
            checkSupervisionMosHandler.init(mockEventHandlerContext)

        and: "the response shows MO exists"
            netconfResponse.setData(getMosExistResponse())

        when: "handler is invoked"
            checkSupervisionMosHandler.onEventWithResult(inputEvent)

        then: "the handler logic executes"
            mockRetryManager.executeCommand(_ as RetryPolicy, _ as RetriableCommand) >> netconfResponse

        and: "the header is updated to show the HeartbeatControl MO is already created"
            assertSuccessfulOperation_and_heartbeatControlMo_exists()
    }

    def "Test invocation from event based client, active is true and heartbeat control MO does not exist"() {
        given: "An event based client request"
            headers.put("clientType", "EVENT_BASED")
            headers.put("active", true)
            headers.put('netconfManager', mockNetconfManager)
            def inputEvent = new MediationComponentEvent(headers, null)

        and: "handler is initialized"
            checkSupervisionMosHandler.init(mockEventHandlerContext)

        and: "the response shows MO doesn't exist"
            netconfResponse.setError(true)
            netconfResponse.setData(getMosDoNotExistResponse())
            mockRetryManager.executeCommand(_ as RetryPolicy, _ as RetriableCommand) >> netconfResponse

        when: "handler is invoked"
            checkSupervisionMosHandler.onEventWithResult(inputEvent)

        then: "the operation shows that the heartbeatControl does not exist"
            assertSuccessfulOperation_and_heartbeatControlMo_doesNotExist()
    }

    def "Test invocation from event based client, when netconf get operation throws an exception"() {
        given: "An event based client request"
            headers.put("clientType", "EVENT_BASED")
            headers.put("active", true)
            headers.put('netconfManager', mockNetconfManager)
            def inputEvent = new MediationComponentEvent(headers, null)

        and: "handler is initialized"
            checkSupervisionMosHandler.init(mockEventHandlerContext)

        and: "the response shows MO doesn't exist and get command throws exception"
            mockRetryManager.executeCommand(_ as RetryPolicy, _ as RetriableCommand) >> [{ throw new NetconfManagerException("crap") }, getOkResponse()]

        when: "handler is invoked"
            checkSupervisionMosHandler.onEventWithResult(inputEvent)

        then: "the netconf sessions operations status shows a failed operation"
            assertFailedOperation("Failed to read MO from node using filter " +
                    "XPathFilter{filter='/ManagedElement/NtfSubscriptionControl/HeartbeatControl'}")
    }

    void assertSuccessfulOperation_and_heartbeatControlMo_doesNotExist() {
        assertSuccessfulOperation(false)
    }

    void assertSuccessfulOperation_and_heartbeatControlMo_exists() {
        assertSuccessfulOperation(true)
    }

    void assertSuccessfulOperation(final boolean heartbeatControlMoExists) {
        NetconfSessionOperationsStatus operationsStatus = headers.get("netconfSessionOperationsStatus")
        O1NetconfSessionOperationStatus operationStatus = operationsStatus.getOperationStatus(NetconfSessionOperation.GET)
        assert operationStatus.getNetconfSessionOperationErrorCode() == NetconfSessionOperationErrorCode.NONE
        assert operationStatus.heartbeatControlMoAlreadyExists() == heartbeatControlMoExists
    }

    void assertFailedOperation(final String errorMessage) {
        NetconfSessionOperationsStatus operationsStatus = headers.get("netconfSessionOperationsStatus")
        O1NetconfSessionOperationStatus operationStatus = operationsStatus.getOperationStatus(NetconfSessionOperation.GET)
        assert operationStatus.getNetconfSessionOperationErrorCode() == NetconfSessionOperationErrorCode.OPERATION_FAILED
        assert operationStatus.getNetconfSessionOperationErrorCode().getAdditionalErrorMessage() == errorMessage
        assert operationsStatus.isAnyOperationInError()
    }

    String getMosExistResponse() {
        return "<rpc-reply xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\" message-id=\"1\">\n" +
                "<data>\n" +
                "    <ManagedElement xmlns=\"urn:3gpp:sa5:_3gpp-common-managed-element\">\n" +
                "        <id>ocp83vcu03o1</id>\n" +
                "        <NtfSubscriptionControl>\n" +
                "            <id>ENMFM</id>\n" +
                "            <HeartbeatControl>\n" +
                "                <id>1</id>\n" +
                "                <attributes>\n" +
                "                    <heartbeatNtfPeriod>100</heartbeatNtfPeriod>\n" +
                "                </attributes>\n" +
                "            </HeartbeatControl>\n" +
                "        </NtfSubscriptionControl>\n" +
                "    </ManagedElement>\n" +
                "</data>\n" +
                "</rpc-reply>"
    }

    String getMosDoNotExistResponse() {
        return "<rpc-reply xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\" message-id=\"1\">\n" +
                "<rpc-error>\n" +
                "    <error-type>rpc</error-type>\n" +
                "    <error-tag>bad-element</error-tag>\n" +
                "    <error-severity>error</error-severity>\n" +
                "    <error-info>\n" +
                "        <bad-element>filter</bad-element>\n" +
                "    </error-info>\n" +
                "</rpc-error>\n" +
                "</rpc-reply>"
    }

    String getOkResponse() {
        return "<rpc-reply xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\" message-id=\"1001\">  \n" +
                "  <ok/>  \n" +
                "</rpc-reply>"
    }
}
