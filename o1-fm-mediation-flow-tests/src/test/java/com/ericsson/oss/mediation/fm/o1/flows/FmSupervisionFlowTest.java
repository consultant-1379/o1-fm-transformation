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

package com.ericsson.oss.mediation.fm.o1.flows;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.ericsson.cds.cdi.support.rule.ImplementationClasses;
import com.ericsson.cds.cdi.support.rule.ImplementationInstance;
import com.ericsson.cds.cdi.support.rule.MockedImplementation;
import com.ericsson.cds.cdi.support.rule.SpyImplementation;
import com.ericsson.oss.itpf.common.event.ComponentEvent;
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.itpf.datalayer.dps.stub.RuntimeConfigurableDps;
import com.ericsson.oss.itpf.sdk.recording.EventLevel;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.mediation.adapter.netconf.jca.api.operation.NetconfOperationRequest;
import com.ericsson.oss.mediation.adapter.netconf.jca.xa.yang.provider.YangEditConfigOperation;
import com.ericsson.oss.mediation.engine.test.flow.FlowRef;
import com.ericsson.oss.mediation.fm.o1.flows.stubs.NetconfConnectionManagerForTestsOk;
import com.ericsson.oss.mediation.fm.o1.handlers.FmSupervisionServiceStateHandler;
import com.ericsson.oss.mediation.fm.o1.handlers.HeartbeatTimerHandler;
import com.ericsson.oss.mediation.fm.o1.handlers.OamInterfaceTypeResolverHandler;
import com.ericsson.oss.mediation.handlers.SshCredentialsManagerHandler;
import com.ericsson.oss.mediation.netconf.handlers.NetconfSessionBuilderHandler;
import com.ericsson.oss.mediation.netconf.session.api.operation.NetconfSessionOperationsStatus;
import com.ericsson.oss.mediation.o1.yang.handlers.netconf.HeartbeatControlHandler;
import com.ericsson.oss.mediation.o1.yang.handlers.netconf.NetconfErrorControlHandler;
import com.ericsson.oss.mediation.o1.yang.handlers.netconf.NtfSubscriptionControlHandler;
import com.ericsson.oss.mediation.o1.yang.handlers.netconf.ReadManagedElementIdHandler;
import com.ericsson.oss.mediation.o1.yang.handlers.netconf.SetDnPrefixHandler;
import com.ericsson.oss.mediation.o1.yang.handlers.netconf.UnlockAlarmListHandler;
import com.ericsson.oss.mediation.o1.yang.handlers.netconf.util.GlobalPropUtils;
import com.ericsson.oss.mediation.sdk.event.SupervisionMediationTaskRequest;

/**
 * This class tests the O1 FmSupervisionFlow success scenario. To prevent this class size increasing please create new classes for other
 * test scenarios. It may be preferable to create a base class for each flow to keep common code per flow. Then extend that class for
 * each test.
 */
@FlowRef(flowName = "FmSupervisionFlow", version = "1.0.0", namespace = "O1_MED")
public class FmSupervisionFlowTest extends O1BaseFlowTest {

    final static String MECONTEXT_FDN = "MeContext=O1Node_Test";
    final static String MANAGED_ELEMENT_FDN = MECONTEXT_FDN + ",ManagedElement=ocp83vcu03o1";
    final static String NAMESPACE = "urn:3gpp:sa5:_3gpp-common-managed-element";
    final static String VES_COLLECTOR_IP = "1.2.3.4";
    final static String NOTIFICATION_RECIPIENT_ADDRESS = "http://1.2.3.4:8099/eventListener/v1";

    @MockedImplementation
    SystemRecorder mocked_systemRecorder;

    @SpyImplementation
    OamInterfaceTypeResolverHandler spy_oamInterfaceTypeResolverHandler;
    @SpyImplementation
    ReadManagedElementIdHandler spy_readManagedElementIdHandler;

    @SpyImplementation
    SetDnPrefixHandler spy_setDnPrefixHandler;

    @SpyImplementation
    UnlockAlarmListHandler spy_unlockAlarmListHandler;

    @SpyImplementation
    NtfSubscriptionControlHandler spy_ntfSubscriptionControlHandler;

    @SpyImplementation
    HeartbeatControlHandler spy_heartbeatControlHandler;

    @SpyImplementation
    HeartbeatTimerHandler spy_heartbeatTimerHandler;

    @SpyImplementation
    NetconfErrorControlHandler spy_netconfErrorControlHandler;

    @SpyImplementation
    FmSupervisionServiceStateHandler spy_fmSupervisionStateHandler;

    // This captor can be used to capture the input to each handler.
    // The input to this handler should contain the output from the previous handler.
    @Captor
    ArgumentCaptor<ComponentEvent> componentEventCaptor;
    @Captor
    ArgumentCaptor<NetconfOperationRequest> netconfOperationRequestCaptor;

    @ImplementationClasses
    private final Class<?>[] definedImplementations = {
        // RetryManagerNonCDIImpl required for ReadManagedElementIdHandler
        com.ericsson.oss.itpf.sdk.core.retry.classic.RetryManagerNonCDIImpl.class,
        com.ericsson.oss.itpf.modeling.modelservice.event.handling.ModelsDeploymentEventServiceImpl.class,
    };

    // TlsCredentialsHandlerManager
    @MockedImplementation
    com.ericsson.oss.mediation.util.DpsHelper mocked_tlsCredentialsManagerHandler_dpsHelper;

    // SshCredentialsHandlerManager
    @ImplementationInstance
    SshCredentialsManagerHandler stubbed_sshCredentialsManagerHandler = new SshCredentialsManagerHandler() {
        @Override
        public ComponentEvent onEvent(ComponentEvent inputEvent) {
            inputEvent.getHeaders().put("weakKeyExchangeAlgorithms", "dummyValue");
            inputEvent.getHeaders().put("weakEncryptionAlgorithms", "dummyValue");
            inputEvent.getHeaders().put("weakHashingAlgorithms", "dummyValue");
            inputEvent.getHeaders().put("username", "dummyValue");
            inputEvent.getHeaders().put("password", null);
            inputEvent.getHeaders().put("passwordBytes", "dummyValue");
            inputEvent.getHeaders().put("privateKey", "dummyValue");
            inputEvent.getHeaders().put("authenticationFailureEventNodeAddress", "dummyValue");
            return inputEvent;
        }
    };

    // NetconfSessionBuilderHandler
    @SpyImplementation
    NetconfConnectionManagerForTestsOk spy_netconfConnectionManagerForTestsOk;

    @ImplementationInstance
    NetconfSessionBuilderHandler stubbed_netconfSessionBuilderHandler = new NetconfSessionBuilderHandler() {
        @Override
        public ComponentEvent onEvent(ComponentEvent inputEvent) {
            NetconfSessionOperationsStatus operationsStatus = new NetconfSessionOperationsStatus();
            inputEvent.getHeaders().put("netconfManager", spy_netconfConnectionManagerForTestsOk);
            inputEvent.getHeaders().put("transportManager", spy_netconfConnectionManagerForTestsOk);
            inputEvent.getHeaders().put("netconfSessionOperationsStatus", operationsStatus);
            return inputEvent;
        }
    };

    @Test
    public void test_execute_flow_success() throws ReflectiveOperationException, IOException {
        createDpsMos("O1Node_Test");
        SupervisionMediationTaskRequest request = createSupervisionMediationTaskRequest("O1Node_Test");
        setupMocks();
        invokeFlow(request);

        verifyOamInterfaceTypeResolverHandler();
        // TlsCredentialsManagerHandler not tested.
        // SshCredentialsManagerHandler not tested.
        // NetconfSessionBuilderHandler not tested.
        verifyReadManagedElementIdHandler();

        // NetconfOperationConnection.executeXAResoureOperation is called by 4 handlers in order: SetDnPrefixHandler, UnlockAlarmListHandler,
        // NtfSubscriptionControlHandler, HeartbeatControlHandler.
        verify(spy_netconfConnectionManagerForTestsOk, times(4)).executeXAResourceOperation(netconfOperationRequestCaptor.capture());

        verifySetDnPrefixHandler();
        verifyUnlockAlarmListHandler();
        verifyNtfSubscriptionControlHandler();
        verifyHeartbeatControlHandler();
        // NetconfSessionReleaserHandler not tested.
        verifyHeartbeatTimerHandler();
        verifyFmSupervisionServiceStateHandler();
    }

    private void verifyOamInterfaceTypeResolverHandler() {
        verify(spy_oamInterfaceTypeResolverHandler, times(1)).onEventWithResult(any());
        verify(spy_oamInterfaceTypeResolverHandler).onEventWithResult(componentEventCaptor.capture());

        Map<String, Object> headers = componentEventCaptor.getValue().getHeaders();
        verifySupervisionAttributeExists(headers, "active", true);
        verifySupervisionAttributeExists(headers, "name", "FmAlarmSupervision");
        verifySupervisionAttributeExists(headers, "version", "1.1.0");
        verifySupervisionAttributeExists(headers, "name", "FmAlarmSupervision");

        verifyHeaderEntryDoesNotExist(headers, "oamInterfaceForFM");
        verifyHeaderEntryDoesNotExist(headers, "active");
    }

    private void verifyReadManagedElementIdHandler() {
        verify(spy_readManagedElementIdHandler, times(1)).onEventWithResult(any());
        verify(spy_readManagedElementIdHandler).onEventWithResult(componentEventCaptor.capture());

        Map<String, Object> headers = componentEventCaptor.getValue().getHeaders();
        verifyHeaderWasCreatedByHandler(headers, "OamInterfaceTypeResolverHandler", "oamInterfaceForFM", "O1");
        verifyHeaderWasCreatedByHandler(headers, "OamInterfaceTypeResolverHandler", "active", true);
        // This header will be set in ReadManagedElementIdHandler, so it should not exist at entry to handler.
        verifyHeaderEntryDoesNotExist(headers, "ManagedElementId");

        verify(mocked_systemRecorder).recordEvent("YANG.READMANAGEDELEMENTIDHANDLER", EventLevel.DETAILED, "Read managed element id", "",
                "Response received from the node: <ManagedElement " +
                        "xmlns=\"urn:3gpp:sa5:_3gpp-common-managed-element\">" +
                        "<id>ocp83vcu03o1</id>" +
                        "<ExternalDomain " +
                        "xmlns=\"urn:rdns:com:ericsson:oammodel:ericsson-external-domain-cr\">" +
                        "<id>1</id>" +
                        "</ExternalDomain>" +
                        "</ManagedElement>");
    }

    private void verifySetDnPrefixHandler() {
        verify(spy_setDnPrefixHandler, times(1)).onEvent(any());
        YangEditConfigOperation operation = (YangEditConfigOperation) netconfOperationRequestCaptor.getAllValues().get(0);
        String handlerName = "SetDnPrefixHandler";

        verifyNetconfOperationAttribute(handlerName, "operation", "merge", operation.getOperation().asParameter());
        verifyNetconfOperationAttribute(handlerName, "fdn", MANAGED_ELEMENT_FDN, operation.getFdn());
        verifyNetconfOperationAttribute(handlerName, "namespace", NAMESPACE, operation.getNamespace());
        verifyNetconfOperationAttribute(handlerName, "name", "ManagedElement", operation.getName());
        verifyNetconfOperationAttribute(handlerName, "nodeType", "O1Node", operation.getNodeType());

        verifyMoAttributeExistsInNetconfOperation(handlerName, getEditNetconfAttributes(operation), "dnPrefix", MECONTEXT_FDN);
    }

    private void verifyUnlockAlarmListHandler() {
        verify(spy_unlockAlarmListHandler, times(1)).onEvent(any());
        YangEditConfigOperation operation = (YangEditConfigOperation) netconfOperationRequestCaptor.getAllValues().get(1);
        String handlerName = "UnlockAlarmListHandler";

        verifyNetconfOperationAttribute(handlerName, "operation", "merge", operation.getOperation().asParameter());
        verifyNetconfOperationAttribute(handlerName, "fdn", MANAGED_ELEMENT_FDN + ",AlarmList=1", operation.getFdn());
        verifyNetconfOperationAttribute(handlerName, "namespace", NAMESPACE, operation.getNamespace());
        verifyNetconfOperationAttribute(handlerName, "name", "AlarmList", operation.getName());
        verifyNetconfOperationAttribute(handlerName, "nodeType", "O1Node", operation.getNodeType());

        verifyMoAttributeExistsInNetconfOperation(handlerName, getEditNetconfAttributes(operation), "administrativeState",
                "UNLOCKED");
    }

    private void verifyNtfSubscriptionControlHandler() {
        verify(spy_ntfSubscriptionControlHandler, times(1)).onEvent(any());
        YangEditConfigOperation operation = (YangEditConfigOperation) netconfOperationRequestCaptor.getAllValues().get(2);

        String handlerName = "NtfSubscriptionControlHandler";
        verifyNetconfOperationAttribute(handlerName, "operation", "create", operation.getOperation().asParameter());
        verifyNetconfOperationAttribute(handlerName, "fdn", MANAGED_ELEMENT_FDN + ",NtfSubscriptionControl=ENMFM", operation.getFdn());
        verifyNetconfOperationAttribute(handlerName, "namespace", NAMESPACE, operation.getNamespace());
        verifyNetconfOperationAttribute(handlerName, "name", "NtfSubscriptionControl", operation.getName());
        verifyNetconfOperationAttribute(handlerName, "nodeType", "O1Node", operation.getNodeType());

        verifyMoAttributeExistsInNetconfOperation(handlerName, getEditNetconfAttributes(operation), "notificationRecipientAddress",
                NOTIFICATION_RECIPIENT_ADDRESS);
        verifyMoAttributeExistsInNetconfOperation(handlerName, getEditNetconfAttributes(operation), "notificationTypes",
                "notifyChangedAlarm");
        verifyMoAttributeExistsInNetconfOperation(handlerName, getEditNetconfAttributes(operation), "notificationTypes",
                "notifyNewAlarm");
        verifyMoAttributeExistsInNetconfOperation(handlerName, getEditNetconfAttributes(operation), "notificationTypes",
                "notifyChangedAlarmGeneral");
        verifyMoAttributeExistsInNetconfOperation(handlerName, getEditNetconfAttributes(operation), "notificationTypes",
                "notifyClearedAlarm");
        verifyMoAttributeExistsInNetconfOperation(handlerName, getEditNetconfAttributes(operation), "notificationTypes",
                "notifyAlarmListRebuilt");
    }

    private void verifyHeartbeatControlHandler() {
        verify(spy_heartbeatControlHandler, times(1)).onEvent(any());
        YangEditConfigOperation operation = (YangEditConfigOperation) netconfOperationRequestCaptor.getAllValues().get(3);

        String handlerName = "HeartbeatControlHandler";
        verifyNetconfOperationAttribute(handlerName, "operation", "create", operation.getOperation().asParameter());
        verifyNetconfOperationAttribute(handlerName, "fdn", MANAGED_ELEMENT_FDN + ",NtfSubscriptionControl=ENMFM,HeartbeatControl=1",
                operation.getFdn());
        verifyNetconfOperationAttribute(handlerName, "namespace", NAMESPACE, operation.getNamespace());
        verifyNetconfOperationAttribute(handlerName, "name", "HeartbeatControl", operation.getName());
        verifyNetconfOperationAttribute(handlerName, "nodeType", "O1Node", operation.getNodeType());

        verifyMoAttributeExistsInNetconfOperation(handlerName, getEditNetconfAttributes(operation), "heartbeatNtfPeriod", "100");
    }

    private void verifyHeartbeatTimerHandler() {
        verify(spy_heartbeatTimerHandler, times(1)).onEventWithResult(any());
    }

    private void verifyNetconfErrorControlHandler() {
        verify(spy_netconfErrorControlHandler, times(1)).onEventWithResult(any());
    }

    private void verifyFmSupervisionServiceStateHandler() {
        verify(spy_fmSupervisionStateHandler, times(1)).onEventWithResult(any());
    }

    private void setupMocks() throws ReflectiveOperationException, IOException {
        // TlsCredentialsHandlerManager
        when(mocked_tlsCredentialsManagerHandler_dpsHelper.isProtocolTypeSetToTls(any())).thenReturn(true);

        //NtfSubscriptionControlHandler
        //Ref: https://www.baeldung.com/mockito-mock-static-methods
        MockedStatic<GlobalPropUtils> mockedGlobalPropUtil = Mockito.mockStatic(GlobalPropUtils.class);
        mockedGlobalPropUtil.when(() -> GlobalPropUtils.getGlobalValue("ves_collector_ip", String.class)).thenReturn(VES_COLLECTOR_IP);
    }

    private void createDpsMos(String nodeName) {
        RuntimeConfigurableDps configurableDps = getCdiInjectorRule().getService(RuntimeConfigurableDps.class);
        configurableDps.withTransactionBoundaries();

        // MeContext
        ManagedObject meContext = configurableDps.addManagedObject()
                .withFdn("MeContext=" + nodeName).addAttribute("id", 1).build();

        // O1ConnectivityInformation
        HashMap<String, Object> connectivityInfoAttributes = new LinkedHashMap<>();
        connectivityInfoAttributes.put("oamInterfaceForFM", "O1");
        connectivityInfoAttributes.put("transportProtocol", "TLS");
        connectivityInfoAttributes.put("ipAddress", "10.0.0.1");

        ManagedObject o1ConnectivityInformation =
                configurableDps.addManagedObject().withFdn("NetworkElement=" + nodeName + ",O1ConnectivityInformation=1")
                        .addAttributes(connectivityInfoAttributes)
                        .build();

        // NetworkElement
        HashMap<String, Object> networkElementAttributes = new LinkedHashMap<>();
        networkElementAttributes.put("fdn", "NetworkElement=" + nodeName);
        networkElementAttributes.put("platformType", "O1");
        networkElementAttributes.put("neType", "O1Node");
        networkElementAttributes.put("version", "1.1.0");

        ManagedObject networkElement =
                configurableDps.addManagedObject().withFdn("NetworkElement=" + nodeName).addAttributes(networkElementAttributes)
                        // NetworkElement to MeContext association
                        .withAssociation("nodeRootRef", meContext)
                        // NetworkElement to O1ConnectivityInformation association
                        .withAssociation("ciRef", o1ConnectivityInformation)
                        .build();

        // MeContext to NetworkElement association
        meContext.addAssociation("networkElementRef", networkElement);

        // FmAlarmSupervision
        HashMap<String, Object> fmAlarmSupervisionAttributes = new LinkedHashMap<>();
        fmAlarmSupervisionAttributes.put("active", true);
        fmAlarmSupervisionAttributes.put("heartbeatinterval", 100);
        fmAlarmSupervisionAttributes.put("heartbeatTimeout", 300);
        fmAlarmSupervisionAttributes.put("automaticSynchronization", true);

        ManagedObject fmAlarmSupervision = configurableDps.addManagedObject().withFdn("NetworkElement=" + nodeName + ",FmAlarmSupervision=1")
                .addAttributes(fmAlarmSupervisionAttributes)
                .build();
        // target for FmAlarmSupervision is the NetworkElement. The NetworkElement must have association 'ciRef' to O1ConnectivityInformation to allow
        // the mediation service to extract the ipAddress of the node by linking the FmAlarmSupervision -> NetworkElement (target) ->
        // O1ConnectivityInformation (ciRef).
        fmAlarmSupervision.setTarget(networkElement);

        // FmFunction
        HashMap<String, Object> fmFunctionAttributes = new LinkedHashMap<>();
        fmFunctionAttributes.put("FmFunctionId", 1);
        fmFunctionAttributes.put("alarmSuppressedState", false);
        // currentServiceState value Range: IN_SERVICE, HEART_BEAT_FAILURE, NODE_SUSPENDED, SYNCHRONIZATION, SYNC_ONGOING, IDLE, ALARM_SUPPRESSED,
        // TECHNICIAN_PRESENT, OUT_OF_SYNC
        fmFunctionAttributes.put("currentServiceState", "IDLE");
        // subscriptionState value range: DISABLED, ENABLED, ENABLING, DISABLING
        fmFunctionAttributes.put("subscriptionState", "DISABLED");
        configurableDps.addManagedObject().withFdn("NetworkElement=" + nodeName + ",FmFunction=1")
                .addAttributes(fmAlarmSupervisionAttributes)
                .build();

    }

    private SupervisionMediationTaskRequest createSupervisionMediationTaskRequest(String nodeName) {
        Map<String, Object> supervisionAttributes = new HashMap<>();
        supervisionAttributes.put("name", "FmAlarmSupervision");
        supervisionAttributes.put("active", true);
        supervisionAttributes.put("version", "1.1.0");

        SupervisionMediationTaskRequest request = new SupervisionMediationTaskRequest();
        request.setJobId("JOB_ID_1");
        request.setClientType("SUPERVISION");
        request.setNodeAddress("NetworkElement=" + nodeName + ",FmAlarmSupervision=1");
        request.setProtocolInfo("FM");
        request.setSupervisionAttributes(supervisionAttributes);
        return request;
    }

}
