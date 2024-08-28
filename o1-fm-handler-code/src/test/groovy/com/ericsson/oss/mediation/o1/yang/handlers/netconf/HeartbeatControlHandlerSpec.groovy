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
package com.ericsson.oss.mediation.o1.yang.handlers.netconf

import com.ericsson.cds.cdi.support.configuration.InjectionProperties
import com.ericsson.cds.cdi.support.providers.custom.model.ModelPattern
import com.ericsson.cds.cdi.support.providers.custom.model.local.LocalModelServiceProvider
import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.rule.SpyImplementation
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.itpf.common.config.Configuration
import com.ericsson.oss.itpf.common.event.handler.EventHandlerContext
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject
import com.ericsson.oss.itpf.datalayer.dps.stub.RuntimeConfigurableDps
import com.ericsson.oss.mediation.flow.events.MediationComponentEvent
import com.ericsson.oss.mediation.netconf.session.api.operation.NetconfSessionOperation
import com.ericsson.oss.mediation.netconf.session.api.operation.NetconfSessionOperationErrorCode
import com.ericsson.oss.mediation.netconf.session.api.operation.NetconfSessionOperationsStatus
import com.ericsson.oss.mediation.o1.yang.handlers.netconf.stub.NetconfConnectionManagerForTestsOk
import com.ericsson.oss.mediation.o1.yang.handlers.netconf.stub.NetconfConnectionManagerForTestsThrowsException
import com.ericsson.oss.mediation.o1.yang.handlers.netconf.util.O1NetconfSessionOperationStatus
import com.ericsson.oss.mediation.o1.yang.handlers.netconf.util.O1NetconfSessionOperationStatusBuilder

class HeartbeatControlHandlerSpec extends CdiSpecification {

    public static final boolean HEARTBEAT_CONTROL_MO_NOT_EXIST = false
    public static final boolean HEARTBEAT_CONTROL_MO_EXISTS = true

    @ObjectUnderTest
    private HeartbeatControlHandler heartbeatControlHandler

    private final Map<String, Object> headers = new HashMap<>()

    @MockedImplementation
    protected EventHandlerContext mockEventHandlerContext

    @MockedImplementation
    protected Configuration mockConfiguration

    @SpyImplementation
    protected NetconfConnectionManagerForTestsOk netconfConnectionManagerForTestsOk = new NetconfConnectionManagerForTestsOk()

    @SpyImplementation
    protected NetconfConnectionManagerForTestsThrowsException netconfConnectionManagerForTestsThrowsException = new NetconfConnectionManagerForTestsThrowsException()

    static filteredModels = [
            new ModelPattern('.*', '.*', '.*', '.*')
    ]

    static LocalModelServiceProvider localModelServiceProvider = new LocalModelServiceProvider(filteredModels)

    @Override
    def addAdditionalInjectionProperties(InjectionProperties injectionProperties) {
        super.addAdditionalInjectionProperties(injectionProperties)
        injectionProperties.addInjectionProvider(localModelServiceProvider)
    }

    def setup() {
        mockEventHandlerContext.getEventHandlerConfiguration() >> mockConfiguration
        mockConfiguration.getAllProperties() >> headers
        createObjects("TestNode")
    }

    def "Test successful creation of the HeartbeatControl MO when supervision is enabled"() {
        given: "Prepare headers for event with all required attributes"
            prepareHeaders(HEARTBEAT_CONTROL_MO_NOT_EXIST)

        and: "a valid netconf manager provided"
            headers.put('netconfManager', netconfConnectionManagerForTestsOk)

        and: "setup input event and initialise NtfSubscriptionControl"
            def inputEvent = new MediationComponentEvent(headers, null)
            heartbeatControlHandler.init(mockEventHandlerContext)

        when: "invoke onEvent with input"
            heartbeatControlHandler.onEvent(inputEvent)

        then: "Underlying netconf library was called with the expected request details"
            1 * netconfConnectionManagerForTestsOk.executeXAResourceOperation({ it.toString().contains(getExpectedRequestDetails()) })

        and: "header set correctly"
            ((String) headers.get('dataStore')) == 'CANDIDATE'
    }

    def "Test HeartbeatControl MO creation fails when NetConf connection manager throws exception"() {
        given: "Prepare headers for event with all required attributes"
            prepareHeaders(HEARTBEAT_CONTROL_MO_NOT_EXIST)

        and: "the netconf manager provided is setup to fail on execution"
            headers.put('netconfManager', netconfConnectionManagerForTestsThrowsException)

        and: "setup input event and initialise NtfSubscriptionControl"
            def inputEvent = new MediationComponentEvent(headers, null)
            heartbeatControlHandler.init(mockEventHandlerContext)

        when: "invoke onEvent with input"
            heartbeatControlHandler.onEvent(inputEvent)

        then: "Underlying netconf library was called"
            1 * netconfConnectionManagerForTestsThrowsException.executeXAResourceOperation(_)

        and: "there is no exception thrown"
            notThrown(Exception)

        and: "header is updated to indicate that the operation failed"
            ((NetconfSessionOperationsStatus) headers.get('netconfSessionOperationsStatus'))
                    .getOperationStatus(NetconfSessionOperation.EDIT_CONFIG).getNetconfSessionOperationErrorCode() == NetconfSessionOperationErrorCode.OPERATION_FAILED
    }

    def "Test HeartbeatControl creation is skipped when heartbeat control is already created"() {
        given: "Prepare headers for event with all required attributes except 'VIP'"
            prepareHeaders(HEARTBEAT_CONTROL_MO_EXISTS)

        and: "handler is initialized"
            def inputEvent = new MediationComponentEvent(headers, null)
            heartbeatControlHandler.init(mockEventHandlerContext)

        when: "invoke onEvent with input"
            heartbeatControlHandler.onEvent(inputEvent)

        then: "operation is aborted"
            ((NetconfSessionOperationsStatus) headers.get('netconfSessionOperationsStatus'))
                    .getOperationStatus(NetconfSessionOperation.EDIT_CONFIG).getNetconfSessionOperationErrorCode() == NetconfSessionOperationErrorCode.OPERATION_ABORTED
    }

    def prepareHeaders(boolean heartbeatControlMoExists) {
        def map = [active: true]
        headers.put('supervisionAttributes', map)
        headers.put('heartbeatinterval', 300)
        headers.put('oamInterfaceForFM', 'O1')
        headers.put('fdn', 'NetworkElement=TestNode,FmAlarmSupervision=1')
        final NetconfSessionOperationsStatus operationsStatus = new NetconfSessionOperationsStatus()
        operationsStatus.addOperationStatus(createOperation(heartbeatControlMoExists));
        headers.put('netconfSessionOperationsStatus', operationsStatus)
        headers.put('VIP', 'svc_FM_VIP_ipaddress')
        headers.put('ManagedElementId', 'TestNode')
    }

    private O1NetconfSessionOperationStatus createOperation(final boolean heartbeatControlMoExists) {
        return new O1NetconfSessionOperationStatusBuilder()
                .operation(NetconfSessionOperation.GET)
                .handlerName("HandlerA")
                .errorCode(NetconfSessionOperationErrorCode.NONE)
                .response(null)
                .setHeartbeatControlMoAlreadyExists(heartbeatControlMoExists)
                .build()
    }

    def createObjects(String nodeName) {
        RuntimeConfigurableDps configurableDps = cdiInjectorRule.getService(RuntimeConfigurableDps)
        configurableDps.withTransactionBoundaries()
        def nodeAttribute = [
                fdn              : "NetworkElement=" + nodeName,
                ossPrefix        : "test_ossPrefix",
                platformType     : "test_platform",
                neType           : "O1Node",
                ossModelIdentity : "555-777-999",
                nodeModelIdentity: "444-333-222",
                version          : "1.1.0"]

        ManagedObject meContext = configurableDps.addManagedObject()
                .withFdn("MeContext=" + nodeName).addAttribute("id", 1).build();

        ManagedObject networkElement = configurableDps.addManagedObject().withFdn("NetworkElement=" + nodeName).addAttributes(nodeAttribute)
                .withAssociation("nodeRootRef", meContext)
                .build()

        meContext.addAssociation('networkElementRef', networkElement);
    }

    def String getExpectedRequestDetails() {
        return "operation=CREATE, " +
                "fdn=MeContext=TestNode,ManagedElement=TestNode,NtfSubscriptionControl=ENMFM,HeartbeatControl=1, " +
                "namespace=urn:3gpp:sa5:_3gpp-common-managed-element, " +
                "name=HeartbeatControl, nodeType=O1Node"
    }
}
