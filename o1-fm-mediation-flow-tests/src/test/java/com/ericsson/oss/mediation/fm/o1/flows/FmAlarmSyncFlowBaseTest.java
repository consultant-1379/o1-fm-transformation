
package com.ericsson.oss.mediation.fm.o1.flows;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;

import org.mockito.ArgumentCaptor;
import org.mockito.Captor;

import com.ericsson.cds.cdi.support.rule.ImplementationClasses;
import com.ericsson.cds.cdi.support.rule.ImplementationInstance;
import com.ericsson.cds.cdi.support.rule.MockedImplementation;
import com.ericsson.cds.cdi.support.rule.SpyImplementation;
import com.ericsson.oss.itpf.common.event.ComponentEvent;
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.itpf.datalayer.dps.stub.RuntimeConfigurableDps;
import com.ericsson.oss.mediation.adapter.netconf.jca.api.operation.NetconfOperationRequest;
import com.ericsson.oss.mediation.fm.o1.engine.api.O1AlarmService;
import com.ericsson.oss.mediation.fm.o1.flows.stubs.NetconfConnectionManagerForTestsFail;
import com.ericsson.oss.mediation.fm.o1.flows.stubs.NetconfConnectionManagerForTestsOk;
import com.ericsson.oss.mediation.fm.o1.handlers.AlarmListTransformerHandler;
import com.ericsson.oss.mediation.fm.o1.handlers.EventNotificationDispatchHandler;
import com.ericsson.oss.mediation.fm.o1.handlers.OamInterfaceTypeResolverHandler;
import com.ericsson.oss.mediation.handlers.SshCredentialsManagerHandler;
import com.ericsson.oss.mediation.handlers.TlsCredentialsManagerHandler;
import com.ericsson.oss.mediation.netconf.handlers.NetconfSessionBuilderHandler;
import com.ericsson.oss.mediation.netconf.handlers.NetconfSessionReleaserHandler;
import com.ericsson.oss.mediation.o1.yang.handlers.netconf.ReadAlarmListHandler;
import com.ericsson.oss.mediation.o1.yang.handlers.netconf.ReadManagedElementIdHandler;
import com.ericsson.oss.mediation.o1.yang.handlers.netconf.util.ReadSystemRecorder;
import com.ericsson.oss.mediation.translator.model.EventNotification;
import com.ericsson.oss.mediation.util.DpsHelper;

public abstract class FmAlarmSyncFlowBaseTest extends O1BaseFlowTest {

    final static String NODE_NAME = "O1Node_Test";
    final static String NETWORK_ELEMENT_FDN = "NetworkElement=O1Node_Test";
    final static String MECONTEXT_FDN = "MeContext=O1Node_Test";
    final static String MANAGED_ELEMENT_FDN = MECONTEXT_FDN + ",ManagedElement=ocp83vcu03o1";
    final static String ALARM_LIST_FDN = MANAGED_ELEMENT_FDN + ",AlarmList=1";
    final static String NAMESPACE = "urn:3gpp:sa5:_3gpp-common-managed-element";

    @MockedImplementation
    O1AlarmService mocked_o1AlarmService;

    @SpyImplementation
    ReadSystemRecorder spy_readSystemRecorder;

    @SpyImplementation
    OamInterfaceTypeResolverHandler spy_oamInterfaceTypeResolverHandler;
    @SpyImplementation
    ReadManagedElementIdHandler spy_readManagedElementIdHandler;

    @SpyImplementation
    ReadAlarmListHandler spy_readAlarmListHandler;

    @SpyImplementation
    AlarmListTransformerHandler spy_alarmListTransformerHandler;

    @SpyImplementation
    EventNotificationDispatchHandler spy_eventNotificationDispatchHandler;

    // This captor can be used to capture the input to each handler.
    // The input to this handler should contain the output from the previous handler.
    @Captor
    ArgumentCaptor<ComponentEvent> componentEventCaptor;
    @Captor
    ArgumentCaptor<NetconfOperationRequest> netconfOperationRequestCaptor;
    @Captor
    ArgumentCaptor<EventNotification> eventNotificationCaptor;

    @ImplementationClasses
    protected final Class<?>[] definedImplementations = {
        // RetryManagerNonCDIImpl required for ReadManagedElementIdHandler
        com.ericsson.oss.itpf.sdk.core.retry.classic.RetryManagerNonCDIImpl.class,
        com.ericsson.oss.itpf.modeling.modelservice.event.handling.ModelsDeploymentEventServiceImpl.class
    };

    // TlsCredentialsHandlerManager
    @SpyImplementation
    TlsCredentialsManagerHandler spy_tlsCredentialsManagerHandler;

    @MockedImplementation
    DpsHelper mocked_tlsCredentialsManagerHandler_dpsHelper;

    // SshCredentialsHandlerManager
    @ImplementationInstance
    SshCredentialsManagerHandler stub_sshCredentialsManagerHandler = new SshCredentialsManagerHandler() {
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
    protected NetconfSessionBuilderHandler spy_netconfSessionBuilderHandler;

    // Note that NetconfConnectionManagerForTestsOk.get method returns null. This will cause a
    // netconf fail if used for a GET operation. Update method return value to required NetconfResponse
    // before using for GET operation success scenarios.
    @SpyImplementation
    NetconfConnectionManagerForTestsOk spy_netconfConnectionManagerForTestsOk;

    // NetconfSessionBuilderHandler
    @SpyImplementation
    NetconfConnectionManagerForTestsFail spy_netconfConnectionManagerForTestsFail;

    @SpyImplementation
    NetconfSessionReleaserHandler spy_netconfSessionReleaserHandler;

    protected void setupGenericMocks() throws ReflectiveOperationException, IOException {
        // TlsCredentialsHandlerManager
        when(mocked_tlsCredentialsManagerHandler_dpsHelper.isProtocolTypeSetToTls(any())).thenReturn(true);
    }

    protected void createDpsMos(String nodeName) {
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

    public void verifyTlsCredentialsManagerHandler() {
        verify(spy_tlsCredentialsManagerHandler, times(1)).onEvent(any());
    }

    public void verifyNetconfSessionReleaserHandler() {
        verify(spy_netconfSessionReleaserHandler, times(1)).onEvent(any());
    }
}
