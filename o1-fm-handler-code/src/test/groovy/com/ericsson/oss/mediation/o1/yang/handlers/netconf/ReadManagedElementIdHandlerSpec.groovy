package com.ericsson.oss.mediation.o1.yang.handlers.netconf

import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.itpf.common.config.Configuration
import com.ericsson.oss.itpf.common.event.ComponentEvent
import com.ericsson.oss.itpf.common.event.handler.EventHandlerContext
import com.ericsson.oss.itpf.sdk.core.retry.RetriableCommand
import com.ericsson.oss.itpf.sdk.core.retry.RetryManager
import com.ericsson.oss.itpf.sdk.core.retry.RetryPolicy
import com.ericsson.oss.mediation.flow.events.MediationComponentEvent
import com.ericsson.oss.mediation.netconf.session.api.operation.NetconfSessionOperationErrorCode
import com.ericsson.oss.mediation.netconf.session.api.operation.NetconfSessionOperationsStatus
import com.ericsson.oss.mediation.o1.yang.handlers.netconf.util.O1NetconfSessionOperationStatus
import com.ericsson.oss.mediation.util.netconf.api.NetconfManager
import com.ericsson.oss.mediation.util.netconf.api.NetconfResponse
import com.ericsson.oss.mediation.util.netconf.api.exception.NetconfManagerException

import javax.inject.Inject

import static com.ericsson.oss.mediation.util.netconf.api.NetconfConnectionStatus.CONNECTED
import static com.ericsson.oss.mediation.util.netconf.api.NetconfConnectionStatus.NEVER_CONNECTED
import static com.ericsson.oss.mediation.util.netconf.api.NetconfConnectionStatus.NOT_CONNECTED

class ReadManagedElementIdHandlerSpec extends CdiSpecification {

    final Map<String, Object> headers = new HashMap<>()
    private EventHandlerContext mockEventHandlerContext = Mock()

    private Configuration mockConfiguration = Mock()

    @ObjectUnderTest
    private ReadManagedElementIdHandler readManagedElementIdHandler

    @Inject
    private NetconfManager netconfManager

    @Inject
    private NetconfResponse netconfResponse

    @Inject
    private RetryManager retryManager

    def setup() {
        mockEventHandlerContext.getEventHandlerConfiguration() >> mockConfiguration
        mockConfiguration.getAllProperties() >> headers
        netconfManager.getStatus() >>> [NEVER_CONNECTED, CONNECTED]
    }

    def "Test successful read of Managed element id"() {
        given: "A valid netconf manager"
            headers.put('netconfManager', netconfManager)

        and: "A valid input event"
            def inputEvent = new MediationComponentEvent(headers, null)
            readManagedElementIdHandler.init(mockEventHandlerContext)

        and: "A valid response from the node"
            netconfResponse.setData(getManagedElementResponse_ok())
            retryManager.executeCommand(_ as RetryPolicy, _ as RetriableCommand) >> netconfResponse

        when: "ReadManagedElementIdHandler is invoked"
            ComponentEvent responseEvent = readManagedElementIdHandler.onEventWithResult(inputEvent)

        then: "ManagedElementId is successfully read from the node"
            responseEvent.getHeaders().get("ManagedElementId") == "ocp83vcu03o1"
    }
    
    def "Test successful read of Managed element id when id is 1"() {
        given: "A valid netconf manager"
            headers.put('netconfManager', netconfManager)

        and: "A valid input event"
            def inputEvent = new MediationComponentEvent(headers, null)
            readManagedElementIdHandler.init(mockEventHandlerContext)

        and: "A valid response from the node"
            netconfResponse.setData(getManagedElementResponse_idIsOne())
            retryManager.executeCommand(_ as RetryPolicy, _ as RetriableCommand) >> netconfResponse

        when: "ReadManagedElementIdHandler is invoked"
            ComponentEvent responseEvent = readManagedElementIdHandler.onEventWithResult(inputEvent)

        then: "ManagedElementId is successfully read from the node"
            responseEvent.getHeaders().get("ManagedElementId") == "1"
    }

    def "Test successful read of Managed element id when already connected"() {
        given: "A valid netconf manager"
            headers.put('netconfManager', netconfManager)

        and: "A valid input event"
            def inputEvent = new MediationComponentEvent(headers, null)
            readManagedElementIdHandler.init(mockEventHandlerContext)

        and: "A valid response from the node"
            netconfResponse.setData(getManagedElementResponse_ok())
            retryManager.executeCommand(_ as RetryPolicy, _ as RetriableCommand) >> netconfResponse

        when: "ReadManagedElementIdHandler is invoked"
            ComponentEvent responseEvent = readManagedElementIdHandler.onEventWithResult(inputEvent)

        then: "ManagedElementId is successfully read from the node"
            netconfManager.getStatus() >>> [CONNECTED, CONNECTED]
            responseEvent.getHeaders().get("ManagedElementId") == "ocp83vcu03o1"
    }

    def "Test successful read of Managed element id when disconnect fails"() {
        given: "A valid netconf manager"
            headers.put('netconfManager', netconfManager)

        and: "A valid input event"
            def inputEvent = new MediationComponentEvent(headers, null)
            readManagedElementIdHandler.init(mockEventHandlerContext)

        and: "A valid response from the node"
            netconfResponse.setData(getManagedElementResponse_ok())
            retryManager.executeCommand(_ as RetryPolicy, _ as RetriableCommand) >>> [netconfResponse,
                                                                                      { throw new NetconfManagerException("disconnect exception") }]
        when: "ReadManagedElementIdHandler is invoked"
            ComponentEvent responseEvent = readManagedElementIdHandler.onEventWithResult(inputEvent)

        then: "ManagedElementId is successfully read from the node"
            netconfManager.getStatus() >>> [CONNECTED, NOT_CONNECTED]
            responseEvent.getHeaders().get("ManagedElementId") == "ocp83vcu03o1"
    }

    def "Netconf manager missing in the header throws manager not found exception"() {
        given: "A valid netconf manager is missing from headers"
            assert headers.isEmpty()

        and: "A valid input event"
            def inputEvent = new MediationComponentEvent(headers, null)
            readManagedElementIdHandler.init(mockEventHandlerContext)

        when: "ReadManagedElementIdHandler is invoked"
            def result = readManagedElementIdHandler.onEventWithResult(inputEvent)

        then: "headers contains netconfSessionOperationsStatus with a single operation error"
            NetconfSessionOperationsStatus operationsStatus = result.getHeaders().get("netconfSessionOperationsStatus")
            operationsStatus.netconfSessionOperationsFailedStatus.size() == 1

        and: "has failed operationStatus with the handler name and additional error message set"
            assertOperationStatusFailure(operationsStatus, "Netconf manager not found")
    }

    def "Managed element id missing in netconf response throws internal error"() {
        given: "A netconf connection that sends response with null managed element id"
            headers.put('netconfManager', netconfManager)

        and: "A valid input event"
            def inputEvent = new MediationComponentEvent(headers, null)
            readManagedElementIdHandler.init(mockEventHandlerContext)

        and: "A response with missing managed element id"
            netconfResponse.setData(getManagedElementResponse_missingId())
            retryManager.executeCommand(_ as RetryPolicy, _ as RetriableCommand) >> netconfResponse

        when: "ReadManagedElementIdHandler is invoked"
            def result = readManagedElementIdHandler.onEventWithResult(inputEvent)

        then: "headers contains netconfSessionOperationsStatus with a single operation error"
            NetconfSessionOperationsStatus operationsStatus = result.getHeaders().get("netconfSessionOperationsStatus")
            operationsStatus.netconfSessionOperationsFailedStatus.size() == 1

        and: "has failed operationStatus with the handler name and additional error message set"
            assertOperationStatusFailure(operationsStatus, "No managedElementId found on the node")
    }

    def "Invoke handler with a null event exception thrown"() {
        given: "A valid netconf manager"
            headers.put('netconfManager', netconfManager)

        when: "ReadManagedElementIdHandler is invoked with a null input event"
            def result = readManagedElementIdHandler.onEventWithResult(null)

        then: "headers contains netconfSessionOperationsStatus with a single operation error"
            NetconfSessionOperationsStatus operationsStatus = result.getHeaders().get("netconfSessionOperationsStatus")
            operationsStatus.netconfSessionOperationsFailedStatus.size() == 1

        and: "has failed operationStatus with the handler name and additional error message set"
            assertOperationStatusFailure(operationsStatus, "Internal error - input event received is null")
    }

    def "Netconf manager throws exception when get request is invoked"() {
        given: "A netconf connection that returns exception at get request"
            headers.put('netconfManager', netconfManager)

        and: "A valid input event"
            def inputEvent = new MediationComponentEvent(headers, null)
            readManagedElementIdHandler.init(mockEventHandlerContext)

        and: "Netconf GET call that will throw an exception"
            retryManager.executeCommand(_ as RetryPolicy, _ as RetriableCommand) >>> [netconfResponse, { throw new NetconfManagerException("Ooops") }, netconfResponse]

        when: "ReadManagedElementIdHandler is invoked"
            def result = readManagedElementIdHandler.onEventWithResult(inputEvent)

        then: "headers contains netconfSessionOperationsStatus with a single operation error"
            NetconfSessionOperationsStatus operationsStatus = result.getHeaders().get("netconfSessionOperationsStatus")
            operationsStatus.netconfSessionOperationsFailedStatus.size() == 1

        and: "has failed operationStatus with the handler name and additional error message set"
            assertOperationStatusFailure(operationsStatus, "Failed to read MO from node using filter XPathFilter{filter='/ManagedElement/attributes'}")
    }

    def "Test when data returned in response from node is empty"() {
        given: "A netconf connection that returs response with empty data"
            headers.put('netconfManager', netconfManager)

        and: "A valid input event"
            def inputEvent = new MediationComponentEvent(headers, null)
            readManagedElementIdHandler.init(mockEventHandlerContext)

        and: "Netconf GET response with empty data"
            netconfResponse.setData("")
            retryManager.executeCommand(_ as RetryPolicy, _ as RetriableCommand) >> netconfResponse

        when: "ReadManagedElementIdHandler is invoked"
            def result = readManagedElementIdHandler.onEventWithResult(inputEvent)

        then: "headers contains netconfSessionOperationsStatus with a single operation error"
            NetconfSessionOperationsStatus operationsStatus = result.getHeaders().get("netconfSessionOperationsStatus")
            operationsStatus.netconfSessionOperationsFailedStatus.size() == 1

        and: "has failed operationStatus with the handler name and additional error message set"
            assertOperationStatusFailure(operationsStatus, "Error - netconf GET response was: NetconfResponse [messageId=0, isError=false, errorMessage=null, errorCode=0, data=, errors=null]")
    }

    def "Test when netconf response from node is null"() {
        given: "A netconf connection that returs response with empty data"
            headers.put('netconfManager', netconfManager)

        and: "A valid input event"
            def inputEvent = new MediationComponentEvent(headers, null)
            readManagedElementIdHandler.init(mockEventHandlerContext)

        and: "Netconf GET response is null"
            retryManager.executeCommand(_ as RetryPolicy, _ as RetriableCommand) >> null

        when: "ReadManagedElementIdHandler is invoked"
            def result = readManagedElementIdHandler.onEventWithResult(inputEvent)

        then: "headers contains netconfSessionOperationsStatus with a single operation error"
            NetconfSessionOperationsStatus operationsStatus = result.getHeaders().get("netconfSessionOperationsStatus")
            operationsStatus.netconfSessionOperationsFailedStatus.size() == 1

        and: "has failed operationStatus with the handler name and additional error message set"
            assertOperationStatusFailure(operationsStatus, "Netconf GET response was null")
    }
    
    def "Test when response from node indicates an error"() {
        given: "A netconf connection"
            headers.put('netconfManager', netconfManager)

        and: "A valid input event"
            def inputEvent = new MediationComponentEvent(headers, null)
            readManagedElementIdHandler.init(mockEventHandlerContext)

        and: "Netconf GET response that indicates an error occurred"
            netconfResponse.setError(true)
            retryManager.executeCommand(_ as RetryPolicy, _ as RetriableCommand) >> netconfResponse

        when: "ReadManagedElementIdHandler is invoked"
            def result = readManagedElementIdHandler.onEventWithResult(inputEvent)

        then: "headers contains netconfSessionOperationsStatus with a single operation error"
            NetconfSessionOperationsStatus operationsStatus = result.getHeaders().get("netconfSessionOperationsStatus")
            operationsStatus.netconfSessionOperationsFailedStatus.size() == 1

        and: "has failed operationStatus with error set to true"
            assertOperationStatusFailure(operationsStatus, "Error - netconf GET response was: NetconfResponse [messageId=0, isError=true, errorMessage=null, errorCode=0, data=, errors=null]")
    }

    private void assertOperationStatusFailure(final NetconfSessionOperationsStatus operationsStatus, final String expectedErrorMessage) {
        final O1NetconfSessionOperationStatus operationStatus = operationsStatus.getFirstOperationInErrorStatus()
        final NetconfSessionOperationErrorCode errorCode = operationStatus.getNetconfSessionOperationErrorCode()
        assert operationStatus.getHandlerName() == "ReadManagedElementIdHandler"
        assert errorCode == NetconfSessionOperationErrorCode.OPERATION_FAILED
        assert errorCode.getAdditionalErrorMessage().equals(expectedErrorMessage)
    }

    private String getManagedElementResponse_missingId() {
        return "<ManagedElement xmlns=\"urn:3gpp:sa5:_3gpp-common-managed-element\">\n" +
                "</ManagedElement>\n";
    }

    private String getManagedElementResponse_ok() {
        return " <ManagedElement xmlns=\"urn:3gpp:sa5:_3gpp-common-managed-element\">\n" +
                "        <id>ocp83vcu03o1</id>\n" +
                "        <attributes>\n" +
                "            <priorityLabel>1</priorityLabel>\n" +
                "            <vendorName>Ericsson AB</vendorName>\n" +
                "            <managedElementTypeList>GNBCUCP</managedElementTypeList>\n" +
                "            <locationName>Linkoping,SWE</locationName>\n" +
                "        </attributes>\n" +
                "    </ManagedElement>";
    }
    
    private String getManagedElementResponse_idIsOne() {
        return " <ManagedElement xmlns=\"urn:3gpp:sa5:_3gpp-common-managed-element\">\n" +
                "        <id>1</id>\n" +
                "        <attributes>\n" +
                "            <priorityLabel>1</priorityLabel>\n" +
                "            <vendorName>Ericsson AB</vendorName>\n" +
                "            <managedElementTypeList>GNBCUCP</managedElementTypeList>\n" +
                "            <locationName>Linkoping,SWE</locationName>\n" +
                "        </attributes>\n" +
                "    </ManagedElement>";
    }
}