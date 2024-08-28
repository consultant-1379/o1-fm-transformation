package com.ericsson.oss.mediation.fm.o1.handlers.test

import com.ericsson.oss.mediation.fm.o1.instrumentation.O1HandlerStatistics

import static com.ericsson.oss.mediation.fm.o1.handlers.util.HandlerMessages.*

import java.time.Instant

import javax.inject.Inject
import javax.jms.JMSException
import javax.jms.ObjectMessage

import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.itpf.common.config.Configuration
import com.ericsson.oss.itpf.common.event.handler.EventHandlerContext
import com.ericsson.oss.itpf.common.event.handler.exception.EventHandlerException
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder
import com.ericsson.oss.mediation.fm.o1.handlers.HeartbeatNotificationHandler
import com.ericsson.oss.mediation.o1.heartbeat.api.HeartbeatDetails

class HeartbeatNotificationHandlerSpec extends CdiSpecification {

    private static final EMPTY_MAP = [:]
    static final String NETWORK_ELEMENT_FDN = "NetworkElement=NodeA";
    static final String FM_ALARM_SUPERVISION_FDN = NETWORK_ELEMENT_FDN + ",FmAlarmSupervision=1";

    @ObjectUnderTest
    private HeartbeatNotificationHandler heartbeatNotificationHandler

    @Inject
    private SystemRecorder systemRecorder

    @Inject
    O1HandlerStatistics o1HandlerStatistics

    private HeartbeatDetails heartbeatDetails;

    def setup() {
        def mockEventHandlerContext = mock(EventHandlerContext)
        def mockConfiguration = mock(Configuration)
        mockEventHandlerContext.getEventHandlerConfiguration() >> mockConfiguration
        mockConfiguration.getAllProperties() >> EMPTY_MAP
        heartbeatNotificationHandler.init(mockEventHandlerContext)
        createHeartbeatDetails()
    }

    def "Test when JMS message input and message cannot be retrieved from JMS object"() {

        given: "an input JMS object that throws an exception when getObject is called"
            def inputEvent = mock(ObjectMessage)
            inputEvent.getObject() >> { throw new JMSException("some internal issue") }

        when: "the handler is invoked"
            heartbeatNotificationHandler.onEventWithResult(inputEvent)

        then: "an exception with an appropriate message is thrown"
            1 * systemRecorder.recordError("HeartbeatNotificationHandler", _, _, _, FAILED_TO_GET_JMS_MESSAGE_FROM_INPUT)
            def exception = thrown(EventHandlerException)
            exception.message.contains(FAILED_TO_GET_JMS_MESSAGE_FROM_INPUT)

        and: "statistics updated to represent a new JMS event received from o1 heartbeat queue"
            o1HandlerStatistics.getTotalNoOfHeartbeatsReceived() == 1
    }

    def "Test when JMS message input and message does not contain map of heartbeat notification properties"() {

        given: "an input JMS message that has no notification properties map"
            def inputEvent = mock(ObjectMessage)

        when: "the handler is invoked"
            heartbeatNotificationHandler.onEventWithResult(inputEvent)

        then: "an exception with an appropriate message is thrown"
            def exception = thrown(EventHandlerException)
            exception.message.contains("Invalid inputEvent: JMS message does not contain the expected Map type")

        and: "statistics updated to represent a new JMS event received from o1 heartbeat queue"
            o1HandlerStatistics.getTotalNoOfHeartbeatsReceived() == 1
    }

    def "Test when JMS message input and message with no href value in properties map"() {

        given: "an invalid input event with null href value"
            def inputEvent = mock(ObjectMessage)
            inputEvent.getObject() >> ["href": null]

        when: "the handler is invoked"
            heartbeatNotificationHandler.onEventWithResult(inputEvent)

        then: "error is logged and an exception with appropriate message is thrown"
            def exception = thrown(EventHandlerException)
            exception.message.contains("dnPrefix was null or empty.")

        and: "statistics updated to represent a new JMS event received from o1 heartbeat queue"
            o1HandlerStatistics.getTotalNoOfHeartbeatsReceived() == 1
    }

    def "Test when JMS message input and message with a href value with no dnPrefix in properties map"() {

        given: "an invalid input event with non-empty invalid href value but no dnPrefix"
            def inputEvent = mock(ObjectMessage)
            inputEvent.getObject() >> ["href": "https://ManagedElement=ocp83vcu03o1/GNBCUCPFunction=1"]

        when: "the handler is invoked"
            heartbeatNotificationHandler.onEventWithResult(inputEvent)

        then: "error is logged and an exception with appropriate message is thrown"
            def exception = thrown(EventHandlerException)
            exception.message.contains("dnPrefix was null or empty.")

        and: "statistics updated to represent a new JMS event received from o1 heartbeat queue"
            o1HandlerStatistics.getTotalNoOfHeartbeatsReceived() == 1
    }

    def "Test when JMS message input and message with invalid href value in properties map"() {

        given: "an invalid input event with invalid href value"
            def inputEvent = mock(ObjectMessage)
            inputEvent.getObject() >> ["href": "MeContext=NodeB,Test=Blah"]

        when: "the handler is invoked"
            heartbeatNotificationHandler.onEventWithResult(inputEvent)

        then: "error is logged and an exception with appropriate message is thrown"
            def exception = thrown(EventHandlerException)
            exception.message.contains("Could not remove or add heartbeat timer for node")

        and: "statistics updated to represent a new JMS event received from o1 heartbeat queue"
            o1HandlerStatistics.getTotalNoOfHeartbeatsReceived() == 1
    }

    def "Test when notifyHeartbeat received when no active timer then alarm cleared and new timer added"() {

        given: "a valid input event with a JMS message that contains a map of notification properties"
            def inputEvent = mock(ObjectMessage)
            inputEvent.getObject() >> ["notificationType": "notifyHeartbeat", "href": "https://NodeA.MeContext/ManagedElement=NodeA/GNBCUCPFunction=1"]

        and: "there is heartbeat timer entry for the networkElementFdn is available in cache"
            heartbeatNotificationHandler.o1HeartbeatService.getEntryFromHeartbeatCache(NETWORK_ELEMENT_FDN) >> heartbeatDetails
            heartbeatDetails.setHeartbeatAlarmRaised(true)

        when: "the handler is invoked"
            heartbeatNotificationHandler.onEventWithResult(inputEvent)

        then: "the heartbeat alarm is cleared and o1heartbeatcache is updated"
            1 * heartbeatNotificationHandler.o1AlarmService.clearHeartbeatAlarm(NETWORK_ELEMENT_FDN)
            1 * heartbeatNotificationHandler.o1HeartbeatService.putEntryInHeartbeatCache(NETWORK_ELEMENT_FDN, _)

        and: "statistics updated to represent a new JMS event received from o1 heartbeat queue"
            o1HandlerStatistics.getTotalNoOfHeartbeatsReceived() == 1
    }

    def "Test when notifyHeartbeat received and dnPrefix conatin mutiple SubNetworks when no active timer then alarm cleared and new timer added"() {

        given: "a valid input event with a JMS message that contains a map of notification properties"
            def inputEvent = mock(ObjectMessage)
            inputEvent.getObject() >> ["notificationType": "notifyHeartbeat", "href": "https://NodeA.MeContext.athlone.SubNetwork.ireland.SubNetwork/ManagedElement=NodeA/GNBCUCPFunction=1"]

        and: "there is heartbeat timer entry for the networkElementFdn is available in cache"
            heartbeatNotificationHandler.o1HeartbeatService.getEntryFromHeartbeatCache(NETWORK_ELEMENT_FDN) >> heartbeatDetails
            heartbeatDetails.setHeartbeatAlarmRaised(true)

        when: "the handler is invoked"
            heartbeatNotificationHandler.onEventWithResult(inputEvent)

        then: "the heartbeat alarm is cleared and o1heartbeatcache is updated"
            1 * heartbeatNotificationHandler.o1AlarmService.clearHeartbeatAlarm(NETWORK_ELEMENT_FDN)
            1 * heartbeatNotificationHandler.o1HeartbeatService.putEntryInHeartbeatCache(NETWORK_ELEMENT_FDN, _)

        and: "statistics updated to represent a new JMS event received from o1 heartbeat queue"
            o1HandlerStatistics.getTotalNoOfHeartbeatsReceived() == 1
    }

    def "Test when notifyHeartbeat received with active timer then timer is reset"() {

        given: "a valid input event with a JMS message that contains a map of notification properties"
            def inputEvent = mock(ObjectMessage)
            inputEvent.getObject() >> ["notificationType": "notifyHeartbeat", "href": "https://NodeA.MeContext/ManagedElement=NodeA/GNBCUCPFunction=1"]

        and: "there is an active heartbeat timer for the node"
            heartbeatNotificationHandler.o1AlarmService.hasActiveHeartbeatTimer(NETWORK_ELEMENT_FDN) >> true
            heartbeatNotificationHandler.o1HeartbeatService.getEntryFromHeartbeatCache(NETWORK_ELEMENT_FDN) >> heartbeatDetails

        when: "the handler is invoked"
            heartbeatNotificationHandler.onEventWithResult(inputEvent)

        then: "o1heartbeatcache is updated with current timestamp"
            1 * heartbeatNotificationHandler.o1HeartbeatService.putEntryInHeartbeatCache(NETWORK_ELEMENT_FDN, _)

        and: "statistics updated to represent a new JMS event received from o1 heartbeat queue"
            o1HandlerStatistics.getTotalNoOfHeartbeatsReceived() == 1
    }

    private void createHeartbeatDetails() {
        heartbeatDetails = new HeartbeatDetails()
        heartbeatDetails.setHeartbeatTimeout(300)
        heartbeatDetails.setLastHeartbeatRecievedEpochTimestamp(Instant.now().getEpochSecond())
    }

}

