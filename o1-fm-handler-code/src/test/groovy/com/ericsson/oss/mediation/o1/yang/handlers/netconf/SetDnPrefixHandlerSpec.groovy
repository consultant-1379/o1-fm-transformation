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
import spock.lang.Unroll

class SetDnPrefixHandlerSpec extends CdiSpecification {

    @ObjectUnderTest
    SetDnPrefixHandler setDnPrefixHandler

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
        createO1NodeInDps()
    }

    @Unroll
    def "Test successful update of ManagedElement MO when supervision is enabled"() {
        given: "Prepare headers for event with all required attributes"
            prepareHeaders(active, hasSupervisionAttributes)

        and: "provide a valid netconf manager"
            headers.put('netconfManager', netconfConnectionManagerForTestsOk)

        and: "setup input event and initialise SetDnPrefixHandler"
            def inputEvent = new MediationComponentEvent(headers, null)
            setDnPrefixHandler.init(mockEventHandlerContext)

        when: "invoke onEvent with input"
            setDnPrefixHandler.onEvent(inputEvent)

        then: "Underlying netconf library was called with the expected request details"
            1 * netconfConnectionManagerForTestsOk.executeXAResourceOperation({
                String operationString = it.toString()
                operationString.contains(getExpectedRequestDetails('MERGE')) &&
                        operationString.contains("Attribute name=dnPrefix Attribute value=MeContext=TestNode")
            })

        and: "header and operation set correctly"
            ((String) headers.get('dataStore')) == 'CANDIDATE'
            setDnPrefixHandler.getHeaderInfo().getOperation() == 'MERGE'

        and: "header info attributes for modify usecase are set correctly"
            setDnPrefixHandler.getHeaderInfo().getModifyAttributes().get('dnPrefix') == 'MeContext=TestNode'
            setDnPrefixHandler.getHeaderInfo().getFdn() == 'MeContext=TestNode,ManagedElement=TestNode'
            setDnPrefixHandler.getHeaderInfo().getNameSpace() == 'urn:3gpp:sa5:_3gpp-common-managed-element'
            setDnPrefixHandler.getHeaderInfo().getType() == 'ManagedElement'
            setDnPrefixHandler.getHeaderInfo().getVersion() == '2023.2.14'
            setDnPrefixHandler.getHeaderInfo().getIncludeNsPrefix() == true

        where:
            active | hasSupervisionAttributes
            true   | true
            true   | false

    }

    @Unroll
    def "Test no update of ManagedElement MO when supervision is disabled"() {
        given: "Prepare headers for event with all required attributes"
            prepareHeaders(active, hasSupervisionAttributes)

        and: "provide a valid netconf manager"
            headers.put('netconfManager', netconfConnectionManagerForTestsOk)

        and: "setup input event and initialise SetDnPrefixHandler"
            def inputEvent = new MediationComponentEvent(headers, null)
            setDnPrefixHandler.init(mockEventHandlerContext)

        when: "invoke onEvent with input"
            setDnPrefixHandler.onEvent(inputEvent)

        then: "Underlying netconf library was not called"
            0 * netconfConnectionManagerForTestsOk.executeXAResourceOperation(_)

        and: "there is no exception thrown"
            notThrown(Exception)

        and: "header and operation set correctly"
            ((String) headers.get('dataStore')) == 'CANDIDATE'

        where:
            active | hasSupervisionAttributes
            false   | true
            false   | false
    }

    @Unroll
    def "Test ManagedElement MO update fails when NetConf connection manager throws exception"() {
        given: "Prepare headers for event with all required attributes"
            prepareHeaders(active, hasSupervisionAttributes)

        and: "the netconf manager provided is setup to fail on execution"
            headers.put('netconfManager', netconfConnectionManagerForTestsThrowsException)

        and: "setup input event and initialise SetDnPrefixHandler"
            def inputEvent = new MediationComponentEvent(headers, null)
            setDnPrefixHandler.init(mockEventHandlerContext)

        when: "invoke onEvent with input"
            setDnPrefixHandler.onEvent(inputEvent)

        then: "Underlying netconf library was called with valid operation request"
            1 * netconfConnectionManagerForTestsThrowsException.executeXAResourceOperation({ it.toString().contains(getExpectedRequestDetails('MERGE')) })

        and: "there is no exception thrown"
            notThrown(Exception)

        and: "header is updated to indicate that the operation failed"
            ((NetconfSessionOperationsStatus) headers.get('netconfSessionOperationsStatus'))
                    .getOperationStatus(NetconfSessionOperation.EDIT_CONFIG).getNetconfSessionOperationErrorCode() == NetconfSessionOperationErrorCode.OPERATION_FAILED

        where:
            active | hasSupervisionAttributes
            true   | true
            true   | false
    }

    def createO1NodeInDps() {
        RuntimeConfigurableDps configurableDps = cdiInjectorRule.getService(RuntimeConfigurableDps)
        configurableDps.withTransactionBoundaries()
        def nodeAttribute = [
                fdn   : "NetworkElement=TestNode",
                neType: "O1Node"]

        ManagedObject meContext = configurableDps.addManagedObject()
                .withFdn("MeContext=TestNode").build();

        ManagedObject networkElement = configurableDps.addManagedObject()
                .withFdn("NetworkElement=TestNode")
                .addAttributes(nodeAttribute)
                .withAssociation("nodeRootRef", meContext)
                .build()

        meContext.addAssociation('networkElementRef', networkElement);
    }

    def prepareHeaders(boolean active, boolean hasSupervisionAttributes) {
        def map = [active: active]
        if (hasSupervisionAttributes) {
            headers.put('supervisionAttributes', map)
        }
        headers.put('active', active)
        headers.put('fdn', 'NetworkElement=TestNode,FmAlarmSupervision=1')
        NetconfSessionOperationsStatus operationsStatus = new NetconfSessionOperationsStatus()
        headers.put('netconfSessionOperationsStatus', operationsStatus)
        headers.put('ManagedElementId', 'TestNode')
    }

    String getExpectedRequestDetails(String operation) {
        return "operation=" + operation + ", " +
                "fdn=MeContext=TestNode,ManagedElement=TestNode, " +
                "namespace=urn:3gpp:sa5:_3gpp-common-managed-element, " +
                "name=ManagedElement, nodeType=O1Node"
    }
}