
package com.ericsson.oss.mediation.fm.o1.flows;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;

import org.junit.Test;

import com.ericsson.cds.cdi.support.rule.ImplementationInstance;
import com.ericsson.oss.itpf.common.event.ComponentEvent;
import com.ericsson.oss.mediation.engine.test.flow.FlowRef;
import com.ericsson.oss.mediation.fm.o1.handlers.util.MediationTaskRequestUtil;
import com.ericsson.oss.mediation.netconf.handlers.NetconfSessionBuilderHandler;
import com.ericsson.oss.mediation.netconf.session.api.operation.NetconfSessionOperationsStatus;
import com.ericsson.oss.mediation.sdk.event.MediationTaskRequest;
import com.ericsson.oss.mediation.translator.model.EventNotification;

@FlowRef(flowName = "FmAlarmSyncFlow", version = "1.0.0", namespace = "O1_MED")
public class FmAlarmSyncFlowFailTest extends FmAlarmSyncFlowBaseTest {

    @ImplementationInstance
    NetconfSessionBuilderHandler stubbed_netconfSessionBuilderHandler = new NetconfSessionBuilderHandler() {
        @Override
        public ComponentEvent onEvent(ComponentEvent inputEvent) {
            NetconfSessionOperationsStatus operationsStatus = new NetconfSessionOperationsStatus();
            inputEvent.getHeaders().put("netconfManager", spy_netconfConnectionManagerForTestsFail);
            inputEvent.getHeaders().put("transportManager", spy_netconfConnectionManagerForTestsFail);
            inputEvent.getHeaders().put("netconfSessionOperationsStatus", operationsStatus);
            return inputEvent;
        }
    };

    @Test
    public void test_when_netconf_operation_fails_in_ReadAlarmListHandler() throws ReflectiveOperationException, IOException {
        // SETUP: the mocks have been set up.
        setupGenericMocks();
        // GIVEN: the O1Node exists and supervision is on.
        createDpsMos(NODE_NAME);
        // WHEN: the flow is invoked.
        // Note: Uses stub NetconfConnectionManagerForTestsFail. This causes a netconf GET to fail because the
        // NetconfConnectionManagerForTestsFail.get method returns null.
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
        verifyReadAlarmListHandlerForFailedNetconfOperation();
        verifyNetconfSessionReleaserHandler();
        verifyAlarmListTransformerHandler();
        verifyEventNotificationDispatchHandler();
    }

    private void verifyOamInterfaceTypeResolverHandler() {
        verify(spy_oamInterfaceTypeResolverHandler, times(1)).onEventWithResult(any());
        verify(spy_oamInterfaceTypeResolverHandler).onEventWithResult(componentEventCaptor.capture());

    }

    private void verifyReadManagedElementIdHandler() {
        verify(spy_readManagedElementIdHandler, times(1)).onEventWithResult(any());
    }

    private void verifyReadAlarmListHandlerForFailedNetconfOperation() {
        verify(spy_readAlarmListHandler, times(1)).onEvent(any());

        verify(mocked_o1AlarmService, times(1)).sendAlarm(eventNotificationCaptor.capture());
        EventNotification actualEvent = eventNotificationCaptor.getValue();
        assertEquals("EventNotification sent has wrong managed object instance.",
                ALARM_LIST_FDN, actualEvent.getManagedObjectInstance());
        assertEquals("EventNotification sent has wrong record type.", "SYNCHRONIZATION_ABORTED",
                actualEvent.getRecordType());
        assertEquals("EventNotification sent has wrong record type.", NETWORK_ELEMENT_FDN,
                actualEvent.getAdditionalAttribute("fdn"));
    }

    private void verifyAlarmListTransformerHandler() {
        verify(spy_alarmListTransformerHandler, times(1)).onEventWithResult(any());
    }

    private void verifyEventNotificationDispatchHandler() {
        verify(spy_eventNotificationDispatchHandler, times(1)).onEventWithResult(any());
    }
}
