package com.ericsson.oss.mediation.fm.o1.engine.nodesuspender

import com.ericsson.cds.cdi.support.providers.stubs.InMemoryCache
import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.rule.SpyImplementation
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject
import com.ericsson.oss.itpf.datalayer.dps.stub.RuntimeConfigurableDps
import com.ericsson.oss.itpf.sdk.cache.classic.CacheProviderBean
import com.ericsson.oss.itpf.sdk.eventbus.model.EventSender
import com.ericsson.oss.itpf.sdk.eventbus.model.annotation.Modeled
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder
import com.ericsson.oss.mediation.fm.o1.engine.api.O1AlarmService
import com.ericsson.oss.mediation.fm.o1.engine.nodesuspender.cache.O1NodeSuspenderCache
import com.ericsson.oss.mediation.fm.o1.engine.nodesuspender.config.NodeSuspenderConfigurationListener
import com.ericsson.oss.mediation.fm.o1.engine.service.FmEventBufferFacade
import com.ericsson.oss.mediation.o1.api.NodeSuspensionEntry
import com.ericsson.oss.mediation.sdk.event.MediationTaskRequest

import javax.ejb.Timer
import javax.ejb.TimerService
import javax.inject.Inject

import static com.ericsson.oss.itpf.sdk.recording.EventLevel.DETAILED
import static com.ericsson.oss.itpf.sdk.recording.ErrorSeverity.ERROR
import static com.ericsson.oss.mediation.fm.o1.common.Constants.COMMA_FM_ALARM_SUPERVISION_RDN

class NodeSuspenderTimerSpec extends CdiSpecification {

    static private final String NETWORK_ELEMENT_FDN_NODEA = "NetworkElement=NodeA"

    @ObjectUnderTest
    private NodeSuspenderTimer nodeSuspenderTimer

    @Inject
    private NodeSuspenderConfigurationListener listener

    @SpyImplementation

    private NodeSuspenderConfigurationListenerStub nodeSuspenderConfigurationListener = new NodeSuspenderConfigurationListenerStub();

    @MockedImplementation
    private TimerService mockTimerService

    @Inject
    private O1NodeSuspenderCache o1NodeSuspenderCache

    @MockedImplementation
    private O1AlarmService alarmService


    @MockedImplementation
    private FmEventBufferFacade fmEventBufferFacade

    @Inject
    private SystemRecorder systemRecorder

    @Inject
    private DataPersistenceService dataPersistenceService

    @Inject
    @Modeled
    private EventSender<MediationTaskRequest> fmMediationAlarmSyncRequestSender;

    @MockedImplementation
    private CacheProviderBean cacheProviderBean

    def nodeSuspensionCache = new InMemoryCache<String, NodeSuspensionEntry>('O1NodeSuspenderCache')

    def setup() {
        RuntimeConfigurableDps configurableDps = cdiInjectorRule.getService(RuntimeConfigurableDps)
        configurableDps.withTransactionBoundaries()

        configurableDps.addManagedObject().
                withFdn(NETWORK_ELEMENT_FDN_NODEA).build();

        configurableDps.addManagedObject().
                withFdn(NETWORK_ELEMENT_FDN_NODEA + COMMA_FM_ALARM_SUPERVISION_RDN).build();

        setUpCache()
    }


    def "Test timer is created when init method is called"() {

        when: "init method was called"
            nodeSuspenderTimer.init()

        then: "a timer was created"
            1 * mockTimerService.createIntervalTimer(_, _, _)
    }


    def "Test when timeout method is invoked, NE with count less than threshold has suspension revoked"() {

        given: "The cache contains a suspended node with count less than the threshold"
            cacheProviderBean.createOrGetModeledCache('O1NodeSuspenderCache') >> nodeSuspensionCache
            nodeSuspenderTimer.listener = nodeSuspenderConfigurationListener

        and: "NodeA is in wanted condition"
            nodeSuspensionCache.get("NodeA").isSuspended()
            nodeSuspensionCache.get("NodeA").getCount() == 49

        and: "automatic synchronization set to true"
            setAutomaticSynchronization(NETWORK_ELEMENT_FDN_NODEA, true)

        when: "timeout method was called"
            o1NodeSuspenderCache.initializeCache()
            nodeSuspenderTimer.timeout(_ as Timer)

        then: "Cache is reset"
            nodeSuspensionCache.get("NodeA").getCount() == 0

        and: "node suspension status is revoked"
            !nodeSuspensionCache.get("NodeA").isSuspended()

        and: "Node suspended alarm is cleared"
            1 * fmEventBufferFacade.sendEvent({ it.toString().contains("perceivedSeverity=CLEARED") })
            1 * systemRecorder.recordEvent('FM_O1_ALARM_SERVICE', DETAILED,
                    '', 'NetworkElement=NodeA', 'Clearing node suspended alarm for NetworkElement=NodeA');

        and:"Alarm sync request is sent"
            1 * fmMediationAlarmSyncRequestSender.send({ it.toString().contains("MediationTaskRequest [nodeAddress=NetworkElement=NodeA,FmAlarmSupervision=1") })

    }


    def "Test when timeout method is invoked, NE with count greater than threshold stays suspended"() {

        given: "The cache contains a suspended node with count greater than threshold"
            cacheProviderBean.createOrGetModeledCache('O1NodeSuspenderCache') >> nodeSuspensionCache
            nodeSuspenderTimer.listener = nodeSuspenderConfigurationListener
            setAutomaticSynchronization(NETWORK_ELEMENT_FDN_NODEA, true)

        and: "NodeB is in wanted condition"
            nodeSuspensionCache.get("NodeB").isSuspended()
            nodeSuspensionCache.get("NodeB").getCount() == 51

        when: "timeout method was called"
            o1NodeSuspenderCache.initializeCache()
            nodeSuspenderTimer.timeout(_ as Timer)

        then: "Cache is reset"
            nodeSuspensionCache.get("NodeB").getCount() == 0

        and: "node stays suspended"
            nodeSuspensionCache.get("NodeB").isSuspended()

        and: "Alarm is not cleared"
            0 * fmEventBufferFacade.sendEvent({ it.toString().contains("managedObjectInstance=NetworkElement=NodeB") })
            0 * systemRecorder.recordEvent('FM_O1_ALARM_SERVICE', DETAILED,
                    '', 'NetworkElement=NodeB', 'Node suspended alarm for NetworkElement=NodeB is not cleared');

        and: "Alarm sync request is not sent"
            0 * fmMediationAlarmSyncRequestSender.send({ it.toString().contains("nodeAddress=NetworkElement=NodeB") })

    }


    def "Test when timeout method is invoked, suspended NE's are processed with automatic sync set to false"() {

        given: "The cache contains a suspended nodes"
            cacheProviderBean.createOrGetModeledCache('O1NodeSuspenderCache') >> nodeSuspensionCache
            nodeSuspenderTimer.listener = nodeSuspenderConfigurationListener

        and: "NodeA is in wanted condition"
            nodeSuspensionCache.get("NodeA").isSuspended()
            nodeSuspensionCache.get("NodeA").getCount() == 49

        and: "automatic synchronization set to false"
            setAutomaticSynchronization(NETWORK_ELEMENT_FDN_NODEA, false)

        when: "timeout method was called"
            o1NodeSuspenderCache.initializeCache()
            nodeSuspenderTimer.timeout(_ as Timer)

        then: "Cache is reset"
            nodeSuspensionCache.get("NodeA").getCount() == 0

        and: "node suspension status is revoked"
            !nodeSuspensionCache.get("NodeA").isSuspended()

        and: "Alarm is cleared"
            1 * fmEventBufferFacade.sendEvent(_)

        and: "Alarm sync request is not sent"
            0 * fmMediationAlarmSyncRequestSender.send(_)

    }


    def "Test timeout invocation when NE has suspension revoked and alarm sync request fails"() {

        given: "The cache contains a suspended node with count less than the threshold"
            cacheProviderBean.createOrGetModeledCache('O1NodeSuspenderCache') >> nodeSuspensionCache
            nodeSuspenderTimer.listener = nodeSuspenderConfigurationListener

        and: "alarm sync request throws exception"
            1 * fmMediationAlarmSyncRequestSender.send(_) >> { throw new IllegalStateException("alarm sync request fails") }

        and: "automatic synchronization set to true"
            setAutomaticSynchronization(NETWORK_ELEMENT_FDN_NODEA, true)

        when: "timeout method was called"
            o1NodeSuspenderCache.initializeCache()
            nodeSuspenderTimer.timeout(_ as Timer)

        then: "Node suspended alarm is cleared"
            1 * fmEventBufferFacade.sendEvent(_)

        and: "an error is logged indicating that alarm sync request failed"
            1 * systemRecorder.recordError('O1AlarmService', ERROR,
                    'NetworkElement=NodeA', "", 'Exception when sending FmMediationAlarmSyncRequest event [alarm sync request fails]');
    }


    private setAutomaticSynchronization(String networkElementFdn, boolean value) {
        final ManagedObject mo = dataPersistenceService.getLiveBucket().findMoByFdn(networkElementFdn + COMMA_FM_ALARM_SUPERVISION_RDN)
        mo.setAttribute("automaticSynchronization", value)
    }

    private setUpCache() {
        nodeSuspensionCache.put("NodeA", new NodeSuspensionEntry(49, true))
        nodeSuspensionCache.put("NodeB", new NodeSuspensionEntry(51, true))
    }

}

class NodeSuspenderConfigurationListenerStub extends NodeSuspenderConfigurationListener {

    private int alarmRateNormalThreshold = 50;

    int getAlarmRateNormalThreshold() {
        return alarmRateNormalThreshold
    }

    private Boolean alarmRateFlowControl = true;

    Boolean getAlarmRateFlowControl() {
        return alarmRateFlowControl
    }

}