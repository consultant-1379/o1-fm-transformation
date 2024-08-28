
package com.ericsson.oss.mediation.fm.o1.flows;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.Test;

import com.ericsson.cds.cdi.support.rule.ImplementationInstance;
import com.ericsson.oss.itpf.common.event.ComponentEvent;
import com.ericsson.oss.mediation.adapter.netconf.jca.xa.yang_read.provider.YangGetConfigOperation;
import com.ericsson.oss.mediation.engine.test.flow.FlowRef;
import com.ericsson.oss.mediation.fm.o1.handlers.util.MediationTaskRequestUtil;
import com.ericsson.oss.mediation.netconf.handlers.NetconfSessionBuilderHandler;
import com.ericsson.oss.mediation.netconf.session.api.operation.NetconfSessionOperationsStatus;
import com.ericsson.oss.mediation.sdk.event.MediationTaskRequest;

@FlowRef(flowName = "FmAlarmSyncFlow", version = "1.0.0", namespace = "O1_MED")
public class FmAlarmSyncFlowSuccessTest extends FmAlarmSyncFlowBaseTest {

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
        // SETUP: the mocks have been set up.
        setupGenericMocks();
        // GIVEN: the O1Node exists and supervision is on.
        createDpsMos("O1Node_Test");
        // WHEN: the flow is invoked.
        MediationTaskRequest request = MediationTaskRequestUtil.createFmMediationAlarmSyncRequest(NETWORK_ELEMENT_FDN);
        invokeFlow(request);

        // THEN: each handler performs it's expected operations.
        verifyOamInterfaceTypeResolverHandler();
        verifyTlsCredentialsManagerHandler();

        // SshCredentialsManagerHandler not tested.
        // Both TlsCredentialsManagerHandler and SshCredentialsManagerHandler have a DpsHelper class with the same
        // package name but different methods. This causes a MissingMethodException. We can test either tls or ssh
        // handlers but not both.

        // NetconfSessionBuilderHandler not tested.
        // The test framework does not recognize NetconfSessionBuilderHandler as a mock/spy when annotated with
        // @SpyImplementation. Error is thrown in code: org.mockito.exceptions.misusing.NotAMockException

        verifyReadManagedElementIdHandler();
        verifyReadAlarmListHandler();
        verifyNetconfSessionReleaserHandler();
        verifyAlarmListTransformerHandler();
        verifyEventNotificationDispatchHandler();
    }

    private void verifyOamInterfaceTypeResolverHandler() {
        verify(spy_oamInterfaceTypeResolverHandler, times(1)).onEventWithResult(any());
    }

    private void verifyReadManagedElementIdHandler() {
        verify(spy_readManagedElementIdHandler, times(1)).onEventWithResult(any());
    }

    private void verifyReadAlarmListHandler() {
        verify(spy_readAlarmListHandler, times(1)).onEvent(any());
        verify(spy_netconfConnectionManagerForTestsOk, times(1)).executeXAResourceOperation(netconfOperationRequestCaptor.capture());

        YangGetConfigOperation operation = (YangGetConfigOperation) netconfOperationRequestCaptor.getAllValues().get(0);
        String handlerName = "ReadAlarmListHandler";

        verifyNetconfOperationAttribute(handlerName, "fdn", ALARM_LIST_FDN, operation.getFdn());
        verifyNetconfOperationAttribute(handlerName, "namespace", NAMESPACE, operation.getNamespace());
        verifyNetconfOperationAttribute(handlerName, "name", "AlarmList", operation.getName());
        verifyNetconfOperationAttribute(handlerName, "nodeType", "O1Node", operation.getNodeType());
        verifyMoAttributeExistsInGetOperation(handlerName, operation, "attributes", NAMESPACE);
        verifyMoAttributeExistsInGetOperation(handlerName, operation, "alarmRecords", NAMESPACE);
    }

    private void verifyAlarmListTransformerHandler() {
        verify(spy_alarmListTransformerHandler, times(1)).onEventWithResult(any());
    }

    private void verifyEventNotificationDispatchHandler() {
        verify(spy_eventNotificationDispatchHandler, times(1)).onEventWithResult(any());
    }

    private void setupMocks() throws ReflectiveOperationException, IOException {
        // TlsCredentialsHandlerManager
        when(mocked_tlsCredentialsManagerHandler_dpsHelper.isProtocolTypeSetToTls(any())).thenReturn(true);
    }
}
