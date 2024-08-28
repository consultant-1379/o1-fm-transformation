package com.ericsson.oss.mediation.fm.o1.handlers.test

import static com.ericsson.oss.mediation.fm.o1.handlers.util.Attributes.HEARTBEAT_TIMEOUT

import java.time.Instant

import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.itpf.common.config.Configuration
import com.ericsson.oss.itpf.common.event.handler.EventHandlerContext
import com.ericsson.oss.mediation.flow.events.MediationComponentEvent
import com.ericsson.oss.mediation.fm.o1.handlers.HeartbeatTimerHandler
import com.ericsson.oss.mediation.o1.heartbeat.api.HeartbeatDetails

class HeartbeatTimerHandlerSpec extends CdiSpecification {

    static private final String NETWORK_ELEMENT_FDN = "NetworkElement=NodeA"
    static private final String FM_ALARM_SUPERVISION_FDN = NETWORK_ELEMENT_FDN + ",FmAlarmSupervision=1"
    static private final long heartbeatTimeout = 300

    @ObjectUnderTest
    private HeartbeatTimerHandler heartbeatTimerHandler

    final Map<String, Object> headers = new HashMap<>()
    protected EventHandlerContext mockEventHandlerContext = Mock()
    HeartbeatDetails heartbeatDetails = new HeartbeatDetails();

    def setup() {
        Configuration mockConfiguration = Mock()
        mockEventHandlerContext.getEventHandlerConfiguration() >> mockConfiguration
        mockConfiguration.getAllProperties() >> headers
        heartbeatDetails.setLastHeartbeatRecievedEpochTimestamp(Instant.now().getEpochSecond())
        heartbeatDetails.setHeartbeatTimeout(heartbeatTimeout)
    }
    def "Test when supervision active sends a request to add heartbeat management for network element"() {
        given: "Prepare headers for event with all required attributes"
            headers.put("supervisionAttributes", ["active": true])
            headers.put("fdn", FM_ALARM_SUPERVISION_FDN)
            headers.put(HEARTBEAT_TIMEOUT, 300)

        and: "setup input event and initialise AbstractFmMediationHandler"
            def inputEvent = new MediationComponentEvent(headers, null)
            heartbeatTimerHandler.init(mockEventHandlerContext)

        when: "invoke onEvent with input"
            heartbeatTimerHandler.onEventWithResult(inputEvent)

        then: "validate that the cache is updated correctly"
            heartbeatTimerHandler.o1HeartbeatService.putEntryInHeartbeatCache(NETWORK_ELEMENT_FDN, heartbeatDetails)
    }

    def "Test when supervision not active sends a request to remove heartbeat management for network element "() {
        given: "Prepare headers for event with all required attributes"
            headers.put("supervisionAttributes", ["active": false])
            headers.put("fdn", FM_ALARM_SUPERVISION_FDN)
            headers.put(HEARTBEAT_TIMEOUT, 300)

        and: "setup input event and initialise AbstractFmMediationHandler"
            def inputEvent = new MediationComponentEvent(headers, null)
            heartbeatTimerHandler.init(mockEventHandlerContext)

        when: "invoke onEvent with input"
            heartbeatTimerHandler.onEventWithResult(inputEvent)

        then: "validate that the entry is removed from the cache"
           heartbeatTimerHandler.o1HeartbeatService.removeEntryFromHeartbeatCache(NETWORK_ELEMENT_FDN)
    }
}
