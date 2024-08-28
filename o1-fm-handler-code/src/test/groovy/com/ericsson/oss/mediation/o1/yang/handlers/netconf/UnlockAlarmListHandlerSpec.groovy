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
import com.ericsson.oss.mediation.o1.yang.handlers.netconf.stub.NetconfConnectionManagerForTestsThrowsException
import com.ericsson.oss.mediation.netconf.session.api.operation.NetconfSessionOperation
import com.ericsson.oss.mediation.netconf.session.api.operation.NetconfSessionOperationErrorCode
import com.ericsson.oss.mediation.netconf.session.api.operation.NetconfSessionOperationsStatus
import com.ericsson.oss.mediation.o1.yang.handlers.netconf.stub.NetconfConnectionManagerForTestsOk
import com.ericsson.oss.mediation.util.netconf.api.NetconfResponse

import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.Constants.ADMINISTRATIVE_STATE
import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.Constants.ALARM_LIST
import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.Constants.FDN
import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.Constants.INCLUDE_NS_PREFIX
import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.Constants.MERGE
import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.Constants.SUPERVISION_ATTRIBUTES
import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.Constants.UNLOCKED

class UnlockAlarmListHandlerSpec extends CdiSpecification {

    static final String NETWORK_ELEMENT_FDN = "NetworkElement=5G141O1001"
    static final String MECONTEXT_FDN = "MeContext=5G141O1001"
    static final String FM_ALARM_SUPERVISION_FDN = "NetworkElement=5G141O1001,FmAlarmSupervision=1"
    static final String ALARM_LIST_FDN = "MeContext=5G141O1001,ManagedElement=5G141O1001,AlarmList=1"

    @ObjectUnderTest
    private UnlockAlarmListHandler unlockAlarmListHandler

    @MockedImplementation
    protected EventHandlerContext mockEventHandlerContext

    @MockedImplementation
    protected Configuration mockConfiguration

    @SpyImplementation
    protected NetconfConnectionManagerForTestsOk netconfConnectionManagerForTestsOk

    @SpyImplementation
    protected NetconfConnectionManagerForTestsThrowsException netconfConnectionManagerForTestsThrowsException

    private final Map<String, Object> headers = new HashMap<>()

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
        addO1NodeToDps()
    }

    def "Test AlarmList MO update fails when NetConf connection manager throws exception"() {
        given: "headers for event with all required attributes"
            prepareHeaders(true)

        and: "the netconf manager provided is setup to fail on execution"
            headers.put('netconfManager', netconfConnectionManagerForTestsThrowsException)

        and: "the UnlockAlarmListHandler is initialized"
            unlockAlarmListHandler.init(mockEventHandlerContext)

        when: "the handler is triggered"
            def inputEvent = new MediationComponentEvent(headers, null)
            unlockAlarmListHandler.onEvent(inputEvent)

        then: "there is one call to the connection manager with the expected request details"
            1 * netconfConnectionManagerForTestsThrowsException.executeXAResourceOperation({ it.toString().contains(getExpectedRequestDetails(MERGE)) })

        and: "there is no exception thrown"
            notThrown(Exception)

        and: "the header is updated to indicate that the operation failed"
            ((NetconfSessionOperationsStatus) headers.get('netconfSessionOperationsStatus'))
                    .getOperationStatus(NetconfSessionOperation.EDIT_CONFIG).getNetconfSessionOperationErrorCode() == NetconfSessionOperationErrorCode.OPERATION_FAILED
    }

    def "Test no update to AlarmList.administrativeState when supervision is disabled"() {
        given: "headers for event with all required attributes"
            prepareHeaders(false)

        and: "a valid netconf manager is provided"
            headers.put('netconfManager', netconfConnectionManagerForTestsOk)

        and: "the UnlockAlarmListHandler is initialized"
            unlockAlarmListHandler.init(mockEventHandlerContext)

        when: "the handler is triggered"
            def inputEvent = new MediationComponentEvent(headers, null)
            unlockAlarmListHandler.onEvent(inputEvent)

        then: "there is no call to the connection manager"
            0 * netconfConnectionManagerForTestsOk.executeXAResourceOperation(_)

        and: "there is no exception thrown"
            notThrown(Exception)

        and: "the header and operation are set correctly"
            ((String) headers.get('dataStore')) == 'CANDIDATE'
    }

    def "Test AlarmList.administrativeState set to UNLOCKED when supervision is enabled"() {
        given: "headers for event with all required attributes"
            prepareHeaders(true)

        and: "a valid netconf manager is provided"
            headers.put('netconfManager', netconfConnectionManagerForTestsOk)

        and: "the UnlockAlarmListHandler is initialized"
            unlockAlarmListHandler.init(mockEventHandlerContext)

        when: "the handler is triggered"
            def inputEvent = new MediationComponentEvent(headers, null)
            unlockAlarmListHandler.onEvent(inputEvent)

        then: "there is one call to the connection manager with the expected request details"
            1 * netconfConnectionManagerForTestsOk.executeXAResourceOperation({
                String operationString = it.toString()
                operationString.contains(getExpectedRequestDetails(MERGE)) &&
                        operationString.contains("Attribute name=administrativeState Attribute value=UNLOCKED")
            })

        and: "the header and operation are set correctly"
            ((String) headers.get('dataStore')) == 'CANDIDATE'
            unlockAlarmListHandler.getHeaderInfo().getOperation() == MERGE

        and: "the header info attributes for modify usecase are set correctly"
            unlockAlarmListHandler.getHeaderInfo().getModifyAttributes().get(ADMINISTRATIVE_STATE) == UNLOCKED
            unlockAlarmListHandler.getHeaderInfo().getFdn() == ALARM_LIST_FDN
            unlockAlarmListHandler.getHeaderInfo().getNameSpace() == 'urn:3gpp:sa5:_3gpp-common-managed-element'
            unlockAlarmListHandler.getHeaderInfo().getType() == ALARM_LIST
            unlockAlarmListHandler.getHeaderInfo().getVersion() == '2023.2.14'
            unlockAlarmListHandler.getHeaderInfo().getIncludeNsPrefix() == true
    }

    def addO1NodeToDps() {
        RuntimeConfigurableDps configurableDps = cdiInjectorRule.getService(RuntimeConfigurableDps)
        configurableDps.withTransactionBoundaries()

        ManagedObject meContext = configurableDps.addManagedObject()
                .withFdn(MECONTEXT_FDN)
                .build();

        ManagedObject networkElement = configurableDps.addManagedObject()
                .withFdn(NETWORK_ELEMENT_FDN)
                .addAttributes([neType: "O1Node"])
                .withAssociation("nodeRootRef", meContext)
                .build()

        meContext.addAssociation('networkElementRef', networkElement)
    }

    def prepareHeaders(boolean active) {
        def map = [active: active]
        headers.put(FDN, FM_ALARM_SUPERVISION_FDN)
        headers.put(SUPERVISION_ATTRIBUTES, map)
        headers.put(INCLUDE_NS_PREFIX, true)
        NetconfSessionOperationsStatus operationsStatus = new NetconfSessionOperationsStatus()
        headers.put('netconfSessionOperationsStatus', operationsStatus)
        headers.put('ManagedElementId', '5G141O1001')
    }

    def configureNetconfConnectionManagerStubWithResponse(response) {
        def netconfResponse = new NetconfResponse();
        netconfResponse.setData(response);
        netconfConnectionManagerForTestsOk.setNetconfOperationResult(netconfResponse, OK)
        headers.put('netconfManager', netconfConnectionManagerForTestsOk)
    }

    private String getExpectedRequestDetails(String operation) {
        return "operation=" + operation + ", " +
                "fdn=MeContext=5G141O1001,ManagedElement=5G141O1001,AlarmList=1, " +
                "namespace=urn:3gpp:sa5:_3gpp-common-managed-element, " +
                "name=AlarmList, nodeType=O1Node"
    }
}
