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
import com.ericsson.oss.mediation.o1.yang.handlers.netconf.exception.MOHandlerException
import com.ericsson.oss.mediation.o1.yang.handlers.netconf.stub.NetconfConnectionManagerForTestsOk
import com.ericsson.oss.mediation.o1.yang.handlers.netconf.stub.NetconfConnectionManagerForTestsThrowsException
import com.ericsson.oss.mediation.o1.yang.handlers.netconf.util.GlobalPropUtils
import com.ericsson.oss.mediation.o1.yang.handlers.netconf.util.O1NetconfSessionOperationStatus
import com.ericsson.oss.mediation.o1.yang.handlers.netconf.util.O1NetconfSessionOperationStatusBuilder

import java.lang.reflect.Field

class NtfSubscriptionControlHandlerSpec extends CdiSpecification {

    public static final boolean HEARTBEAT_CONTROL_MO_NOT_EXIST = false
    public static final boolean HEARTBEAT_CONTROL_MO_EXISTS = true
    public static final boolean NOT_ACTIVE = false
    public static final boolean ACTIVE = true

    @ObjectUnderTest
    NtfSubscriptionControlHandler ntfSubscriptionControlHandler

    @MockedImplementation
    private EventHandlerContext mockEventHandlerContext

    @MockedImplementation
    private Configuration mockConfiguration

    @SpyImplementation
    private NetconfConnectionManagerForTestsOk netconfConnectionManagerForTestsOk

    @SpyImplementation
    private NetconfConnectionManagerForTestsThrowsException netconfConnectionManagerForTestsThrowsException

    final Map<String, Object> headers = new HashMap<>()

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
        setStaticField(GlobalPropUtils, "globalPropertiesFile", this.getClass().getClassLoader().getResource("global.properties").getPath())
        GlobalPropUtils.initGlobalProperties()
    }

    private void setStaticField(Class clazz, String fieldName, Object newValue) {
        Field field = clazz.getDeclaredField(fieldName)
        field.setAccessible(true)
        field.set(null, newValue)
    }

    def "Test successful creation of the NtfSubscriptionControl MO when supervision is enabled"() {
        given: "Prepare headers for event with all required attributes"
            prepareHeaders(ACTIVE, HEARTBEAT_CONTROL_MO_NOT_EXIST)

        and: "provide a valid netconf manager"
            headers.put('netconfManager', netconfConnectionManagerForTestsOk)

        and: "setup input event and initialise NtfSubscriptionControl"
            def inputEvent = new MediationComponentEvent(headers, null)
            ntfSubscriptionControlHandler.init(mockEventHandlerContext)

        when: "invoke onEvent with input"
            ntfSubscriptionControlHandler.onEvent(inputEvent)

        then: "Underlying netconf library was called with the expected request details"
            1 * netconfConnectionManagerForTestsOk.executeXAResourceOperation({
                String operationString = it.toString()
                operationString.contains(getExpectedRequestDetails('CREATE')) &&
                        operationString.contains("Attribute name=notificationRecipientAddress Attribute value=http://1.2.3.4:8099/eventListener/v1") &&
                        operationString.contains("Attribute name=notificationTypes Attribute value=notifyChangedAlarm") &&
                        operationString.contains("Attribute name=notificationTypes Attribute value=notifyNewAlarm") &&
                        operationString.contains("Attribute name=notificationTypes Attribute value=notifyChangedAlarmGeneral") &&
                        operationString.contains("Attribute name=notificationTypes Attribute value=notifyClearedAlarm") &&
                        operationString.contains("Attribute name=notificationTypes Attribute value=notifyAlarmListRebuilt") &&
                        !operationString.contains("Attribute name=scope Attribute value=BASE_ALL")
            })

        and: "header and operation set correctly"
            ((String) headers.get('dataStore')) == 'CANDIDATE'
            ntfSubscriptionControlHandler.getHeaderInfo().getOperation() == 'CREATE'

        and: "header info attributes for create usecase are set correctly"
            ntfSubscriptionControlHandler.getHeaderInfo().getCreateAttributes().get('notificationRecipientAddress') == 'http://1.2.3.4:8099/eventListener/v1'
            ntfSubscriptionControlHandler.getHeaderInfo().getCreateAttributes().get('id') == 'ENMFM'
            ntfSubscriptionControlHandler.getHeaderInfo().getCreateAttributes().get('notificationTypes') == Arrays.asList('notifyChangedAlarm', 'notifyNewAlarm',
                    'notifyChangedAlarmGeneral', 'notifyClearedAlarm', 'notifyAlarmListRebuilt')

            ntfSubscriptionControlHandler.getHeaderInfo().getFdn() == 'MeContext=TestNode,ManagedElement=TestNode,NtfSubscriptionControl=ENMFM'
            ntfSubscriptionControlHandler.getHeaderInfo().getNameSpace() == 'urn:3gpp:sa5:_3gpp-common-managed-element'
            ntfSubscriptionControlHandler.getHeaderInfo().getType() == 'NtfSubscriptionControl'
            ntfSubscriptionControlHandler.getHeaderInfo().getVersion() == '2023.2.14'
            ntfSubscriptionControlHandler.getHeaderInfo().getIncludeNsPrefix() == true
    }


    def "Test successful deletion of the NtfSubscriptionControl MO when supervision is disabled"() {
        given: "Prepare headers for event with all required attributes"
            prepareHeaders(NOT_ACTIVE, HEARTBEAT_CONTROL_MO_NOT_EXIST)

        and: "provide a valid netconf manager"
            headers.put('netconfManager', netconfConnectionManagerForTestsOk)

        and: "setup input event and initialise NtfSubscriptionControl"
            def inputEvent = new MediationComponentEvent(headers, null)
            ntfSubscriptionControlHandler.init(mockEventHandlerContext)

        when: "invoke onEvent with input"
            ntfSubscriptionControlHandler.onEvent(inputEvent)

        then: "Underlying netconf library was called with the expected request details"
            1 * netconfConnectionManagerForTestsOk.executeXAResourceOperation({ it.toString().contains(getExpectedRequestDetails('DELETE')) })

        and: "header and operation set correctly"
            ((String) headers.get('dataStore')) == 'CANDIDATE'
            ntfSubscriptionControlHandler.getHeaderInfo().getOperation() == 'DELETE'

        then: "header info attributes for delete usecase are set correctly"
            ntfSubscriptionControlHandler.getHeaderInfo().getCreateAttributes() == null
            ntfSubscriptionControlHandler.getHeaderInfo().getFdn() == 'MeContext=TestNode,ManagedElement=TestNode,NtfSubscriptionControl=ENMFM'
            ntfSubscriptionControlHandler.getHeaderInfo().getNameSpace() == 'urn:3gpp:sa5:_3gpp-common-managed-element'
            ntfSubscriptionControlHandler.getHeaderInfo().getType() == 'NtfSubscriptionControl'
            ntfSubscriptionControlHandler.getHeaderInfo().getVersion() == '2023.2.14'
            ntfSubscriptionControlHandler.getHeaderInfo().getIncludeNsPrefix() == true
    }

    def "Test NtfSubscriptionControl MO creation fails when VIP property not found"() {
        given: "Prepare headers for event with all required attributes"
            prepareHeaders(ACTIVE, HEARTBEAT_CONTROL_MO_NOT_EXIST)

        and: "provide a valid netconf manager"
            headers.put('netconfManager', netconfConnectionManagerForTestsOk)

        and: "setup Global Properties for testing"
            setStaticField(GlobalPropUtils, "globalPropertiesFile", this.getClass().getClassLoader().getResource("noVIP_global.properties").getPath())
            GlobalPropUtils.initGlobalProperties()
            GlobalPropUtils.globalPropertiesFile = "src/test/resources/noVIP_global.properties"

        when: "handler is initialized"
            def inputEvent = new MediationComponentEvent(headers, null)
            ntfSubscriptionControlHandler.init(mockEventHandlerContext)

        then: "Exception was thrown from handler"
            MOHandlerException exception = thrown()
            exception.message == "Global Properties file value svc_FM_VIP_ipaddress was not found"
    }

    def "Test NtfSubscriptionControl MO creation fails when NetConf connection manager throws exception"() {
        given: "Prepare headers for event with all required attributes"
            prepareHeaders(ACTIVE, HEARTBEAT_CONTROL_MO_NOT_EXIST)

        and: "the netconf manager provided is setup to fail on execution"
            headers.put('netconfManager', netconfConnectionManagerForTestsThrowsException)

        and: "setup Global Properties for testing"
            GlobalPropUtils.globalPropertiesFile = "src/test/resources/global.properties"

        and: "setup input event and initialise NtfSubscriptionControl"
            def inputEvent = new MediationComponentEvent(headers, null)
            ntfSubscriptionControlHandler.init(mockEventHandlerContext)

        when: "invoke onEvent with input"
            ntfSubscriptionControlHandler.onEvent(inputEvent)

        then: "Underlying netconf library was called with valid operation request"
            1 * netconfConnectionManagerForTestsThrowsException.executeXAResourceOperation({ it.toString().contains(getExpectedRequestDetails('CREATE')) })

        and: "there is no exception thrown"
            notThrown(Exception)

        and: "header is updated to indicate that the operation failed"
            ((NetconfSessionOperationsStatus) headers.get('netconfSessionOperationsStatus'))
                    .getOperationStatus(NetconfSessionOperation.EDIT_CONFIG).getNetconfSessionOperationErrorCode() == NetconfSessionOperationErrorCode.OPERATION_FAILED
    }

    def "Test NtfSubscriptionControl MO creation fails when VIP header not set"() {
        given: "Prepare headers for event with all required attributes except 'VIP'"
            prepareHeaders(ACTIVE, HEARTBEAT_CONTROL_MO_NOT_EXIST)
            headers.remove("VIP")

        when: "handler is initialized"
            def inputEvent = new MediationComponentEvent(headers, null)
            ntfSubscriptionControlHandler.init(mockEventHandlerContext)

        then: "Exception was thrown from handler"
            MOHandlerException exception = thrown()
            exception.message == "Cannot determine VES collector ipaddress as 'VIP' header has not been set"
    }

    def "Test NtfSubscriptionControl creation is skipped when heartbeat control is already created"() {
        given: "Prepare headers for event with all required attributes except 'VIP'"
            prepareHeaders(ACTIVE, HEARTBEAT_CONTROL_MO_EXISTS)

        and: "handler is initialized"
            def inputEvent = new MediationComponentEvent(headers, null)
            ntfSubscriptionControlHandler.init(mockEventHandlerContext)

        when: "invoke onEvent with input"
            ntfSubscriptionControlHandler.onEvent(inputEvent)

        then: "operation is aborted"
            ((NetconfSessionOperationsStatus) headers.get('netconfSessionOperationsStatus'))
                    .getOperationStatus(NetconfSessionOperation.EDIT_CONFIG).getNetconfSessionOperationErrorCode() == NetconfSessionOperationErrorCode.OPERATION_ABORTED
    }

    def createObjects(String nodeName) {
        RuntimeConfigurableDps configurableDps = cdiInjectorRule.getService(RuntimeConfigurableDps)
        configurableDps.withTransactionBoundaries()
        def nodeAttribute = [
                fdn         : "NetworkElement=" + nodeName,
                platformType: "O1",
                neType      : "O1Node",
                version     : "1.1.0"]

        ManagedObject meContext = configurableDps.addManagedObject()
                .withFdn("MeContext=" + nodeName).addAttribute("id", 1).build();

        ManagedObject networkElement = configurableDps.addManagedObject().withFdn("NetworkElement=" + nodeName).addAttributes(nodeAttribute)
                .withAssociation("nodeRootRef", meContext)
                .build()

        meContext.addAssociation('networkElementRef', networkElement);
    }

    def prepareHeaders(boolean active, boolean heartbeatControlMoExists) {
        def map = [active: active]
        headers.put('supervisionAttributes', map)
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

    String getExpectedRequestDetails(String operation) {
        return "operation=" + operation + ", " +
                "fdn=MeContext=TestNode,ManagedElement=TestNode,NtfSubscriptionControl=ENMFM, " +
                "namespace=urn:3gpp:sa5:_3gpp-common-managed-element, " +
                "name=NtfSubscriptionControl, nodeType=O1Node"
    }
}
