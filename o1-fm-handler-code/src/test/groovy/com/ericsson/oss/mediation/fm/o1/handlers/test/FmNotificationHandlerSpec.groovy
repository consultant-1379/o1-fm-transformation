package com.ericsson.oss.mediation.fm.o1.handlers.test

import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.itpf.common.config.Configuration
import com.ericsson.oss.itpf.common.event.handler.EventHandlerContext
import com.ericsson.oss.itpf.common.event.handler.exception.EventHandlerException
import com.ericsson.oss.itpf.sdk.recording.EventLevel
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder
import com.ericsson.oss.mediation.flow.events.MediationComponentEvent
import com.ericsson.oss.mediation.fm.o1.engine.api.O1AlarmService
import com.ericsson.oss.mediation.fm.o1.handlers.FmNotificationHandler
import com.ericsson.oss.mediation.fm.o1.instrumentation.O1HandlerStatistics

import javax.inject.Inject
import javax.jms.JMSException
import javax.jms.ObjectMessage

import static com.ericsson.oss.mediation.fm.o1.handlers.util.HandlerMessages.DN_PREFIX_MISSING
import static com.ericsson.oss.mediation.fm.o1.handlers.util.HandlerMessages.FAILED_TO_GET_JMS_MESSAGE_FROM_INPUT
import static com.ericsson.oss.mediation.fm.o1.handlers.util.HandlerMessages.UNKNOWN_NOTIFICATION
import static com.ericsson.oss.mediation.fm.o1.handlers.util.HandlerMessages.UNSUPPORTED_NOTIFICATION

class FmNotificationHandlerSpec extends CdiSpecification {


    @ObjectUnderTest
    private FmNotificationHandler fmNotificationHandler

    @Inject
    private SystemRecorder systemRecorder

    @Inject
    private O1AlarmService mockAlarmService

    @Inject
    O1HandlerStatistics o1HandlerStatistics

    private final Map<String, Object> headers = new HashMap<>()
    private EventHandlerContext mockEventHandlerContext = Mock()
    private Configuration mockConfiguration = Mock()


    def setup() {
        mockEventHandlerContext.getEventHandlerConfiguration() >> mockConfiguration
        mockConfiguration.getAllProperties() >> headers
        fmNotificationHandler.init(mockEventHandlerContext)
    }

    def "Test when JMS message input and message cannot be retrieved from JMS object"() {

        given: "a input JMS object that throws an exception when getObject is called"
            def inputEvent = mock(ObjectMessage)
            inputEvent.getObject() >> { throw new JMSException("some internal issue") }

        when: "the handler is invoked"
            fmNotificationHandler.onEventWithResult(inputEvent)

        then: "an exception with an appropriate message is thrown"
            def exception = thrown(EventHandlerException)
            exception.message.contains(FAILED_TO_GET_JMS_MESSAGE_FROM_INPUT)

        and: "statistics updated to represent a new JMS event received from o1 notifications queue"
            o1HandlerStatistics.getTotalNoOfAlarmsReceived() == 1
    }

    def "Test when JMS message input and message does not contain map of alarm properties"() {

        given: "an input JMS message that has no alarm properties map"
            def inputEvent = mock(ObjectMessage)

        when: "the handler is invoked"
            fmNotificationHandler.onEventWithResult(inputEvent)

        then: "an exception with an appropriate message is thrown"
            def exception = thrown(EventHandlerException)
            exception.message.contains("Invalid inputEvent: JMS message does not contain the expected Map type")

        and: "statistics updated to represent a new JMS event received from o1 notifications queue"
            o1HandlerStatistics.getTotalNoOfAlarmsReceived() == 1
    }


    def "Test when JMS message input and message with unknown notification type"() {
        given: "a valid input event with unknown notificationType"
            def inputEvent = mock(ObjectMessage)
            inputEvent.getObject() >> ["prop1": "value", "notificationType": "unknownNotifyEvent"]

        when: "the handler is invoked"
            final MediationComponentEvent result = fmNotificationHandler.onEventWithResult(inputEvent)

        then: "error is logged and an exception with appropriate message is thrown"
            1 * systemRecorder.recordError("FmNotificationHandler", _, _, _, UNKNOWN_NOTIFICATION)
            def exception = thrown(EventHandlerException)
            exception.message.contains(UNKNOWN_NOTIFICATION)

        and: "statistics updated to represent a new JMS event received from o1 notifications queue"
            o1HandlerStatistics.getTotalNoOfAlarmsReceived() == 1
    }


    def "Test when JMS message input and message with unsupported notification type"() {
        given: "a valid input event with unsupported notificationType attribute"
            def inputEvent = mock(ObjectMessage)
            inputEvent.getObject() >> ["notificationType": "notifyEvent"]

        when: "the handler is invoked"
            final MediationComponentEvent result = fmNotificationHandler.onEventWithResult(inputEvent)

        then: "warning is logged and an exception with appropriate message is thrown"
            1 * systemRecorder.recordError("FmNotificationHandler", _, _, _, UNSUPPORTED_NOTIFICATION)
            def exception = thrown(EventHandlerException)
            exception.message.contains(UNSUPPORTED_NOTIFICATION)

        and: "statistics updated to represent a new JMS event received from o1 notifications queue"
            o1HandlerStatistics.getTotalNoOfAlarmsReceived() == 1
    }

    def "Test when JMS message input with no href in payload"() {

        given: "an valid input event with null href value"
            def inputEvent = mock(ObjectMessage)
            inputEvent.getObject() >> ["notificationType": "notifyNewAlarm", "href": null]

        and: "alarm flow rate is enabled"
            mockAlarmService.isAlarmFlowRateEnabled() >> true

        when: "the handler is invoked"
            fmNotificationHandler.onEventWithResult(inputEvent)

        then: "error is logged and an exception with appropriate message is thrown"
            1 * systemRecorder.recordError("FmNotificationHandler", _, _, _, DN_PREFIX_MISSING)
            def exception = thrown(EventHandlerException)
            exception.message.contains(DN_PREFIX_MISSING)

        and: "statistics updated to represent a new JMS event received from o1 notifications queue"
            o1HandlerStatistics.getTotalNoOfAlarmsReceived() == 1
    }


    def "Test when JMS message input with no dnPrefix in href"() {

        given: "an valid input event with no dnPrefix"
            def inputEvent = mock(ObjectMessage)
            inputEvent.getObject() >> ["notificationType": "notifyNewAlarm", "href": "https://ManagedElement=ocp83vcu03o1/GNBCUCPFunction=1"]

        and: "alarm flow rate is enabled"
            mockAlarmService.isAlarmFlowRateEnabled() >> true

        when: "the handler is invoked"
            fmNotificationHandler.onEventWithResult(inputEvent)

        then: "error is logged and an exception with appropriate message is thrown"
            1 * systemRecorder.recordError("FmNotificationHandler", _, _, _, DN_PREFIX_MISSING)
            def exception = thrown(EventHandlerException)
            exception.message.contains(DN_PREFIX_MISSING)

        and: "statistics updated to represent a new JMS event received from o1 notifications queue"
            o1HandlerStatistics.getTotalNoOfAlarmsReceived() == 1
    }


    def "Test when JMS message input and message contains valid map of alarm properties"() {

        given: "a valid input event with a JMS message that contains a map of alarm properties"

            def inputEvent = mock(ObjectMessage)

            inputEvent.getObject() >> ["prop1": "value", "prop2": "value", "notificationType": "notifyNewAlarm", "href": "https://NodeA.MeContext/ManagedElement=NodeA/GNBCUCPFunction=1"]

        when: "the handler is invoked"
            final MediationComponentEvent result = fmNotificationHandler.onEventWithResult(inputEvent)

        then: "the handler returns a result that contains a payload with the alarm properties."
            result.getPayload() == ["prop1": "value", "prop2": "value", "notificationType": "notifyNewAlarm", "href": "https://NodeA.MeContext/ManagedElement=NodeA/GNBCUCPFunction=1"]

        and: "statistics updated to represent a new JMS event received from o1 notifications queue"
            o1HandlerStatistics.getTotalNoOfAlarmsReceived() == 1
    }


    def "Test when JMS message input when alarm flow rate is disabled"() {

        given: "a valid input event with a JMS message that contains a map of alarm properties"
            def inputEvent = mock(ObjectMessage)
            inputEvent.getObject() >> ["notificationType": "notifyNewAlarm", "href": "https://NodeA.MeContext/ManagedElement=NodeA/GNBCUCPFunction=1"]

        and: "alarm flow rate is disabled"
            mockAlarmService.isAlarmFlowRateEnabled() >> false

        when: "the handler is invoked"
            final MediationComponentEvent result = fmNotificationHandler.onEventWithResult(inputEvent)

        then: "the handler returns a result that contains a payload with the alarm properties."
            result.getPayload() == ["notificationType": "notifyNewAlarm", "href": "https://NodeA.MeContext/ManagedElement=NodeA/GNBCUCPFunction=1"]

        and: "no call made to update node suspender cache and node suspended alarm is not raised"
            0 * fmNotificationHandler.alarmService.updateNodeSuspenderCache(_)
            0 * mockAlarmService.sendAlarm(_)

        and: "header is added with choice operator attribute set to false"
            headers.get("isNodeSuspended") == false

        and: "statistics updated to represent a new JMS event received from o1 notifications queue"
            o1HandlerStatistics.getTotalNoOfAlarmsReceived() == 1
    }


    def "Test when JMS message input with alarm flow rate is enabled and node is suspended before incrementing the notification count"() {

        given: "a valid input event with a JMS message that contains a map of alarm properties"
            def inputEvent = mock(ObjectMessage)
            inputEvent.getObject() >> ["notificationType": "notifyNewAlarm", "href": "https://NodeA.MeContext/ManagedElement=NodeA/GNBCUCPFunction=1"]

        and: "alarm flow rate is enabled"
            mockAlarmService.isAlarmFlowRateEnabled() >> true

        and: "node is suspended"
            1 * mockAlarmService.isNodeSuspended("NodeA") >> true

        when: "the handler is invoked"
            fmNotificationHandler.onEventWithResult(inputEvent)

        then: "notification count is incremented"
            1 * fmNotificationHandler.alarmService.updateNodeSuspenderCache(_)

        and: "header is added with choice operator attribute set to true"
            headers.get("isNodeSuspended") == true

        and: "no call made to update node suspender cache and node suspended alarm is not raised"
            0 * mockAlarmService.sendAlarm(_)

        and: "statistics updated to represent a new JMS event received from o1 notifications queue"
            o1HandlerStatistics.getTotalNoOfAlarmsReceived() == 1
    }

    def "Test when JMS message input with alarm flow rate is enabled and node is not suspended"() {

        given: "a valid input event with a JMS message that contains a map of alarm properties"
            def inputEvent = mock(ObjectMessage)
            inputEvent.getObject() >> ["notificationType": "notifyNewAlarm", "href": "https://NodeA.MeContext/ManagedElement=NodeA/GNBCUCPFunction=1"]

        and: "alarm flow rate is enabled"
            mockAlarmService.isAlarmFlowRateEnabled() >> true

        and: "node is not suspended"
            mockAlarmService.isNodeSuspended("NodeA") >> false

        when: "the handler is invoked"
            final MediationComponentEvent result = fmNotificationHandler.onEventWithResult(inputEvent)

        then: "the handler returns a result that contains a payload with the alarm properties."
            result.getPayload() == ["notificationType": "notifyNewAlarm", "href": "https://NodeA.MeContext/ManagedElement=NodeA/GNBCUCPFunction=1"]

        and: "notification count is incremented"
            1 * fmNotificationHandler.alarmService.updateNodeSuspenderCache(_)

        and: "no call made to update node suspender cache and node suspended alarm is not raised"
            0 * fmNotificationHandler.alarmService.updateNodeSuspenderCache(_)
            0 * mockAlarmService.sendAlarm(_)

        and: "header is added with choice operator attribute set to false"
            headers.get("isNodeSuspended") == false

        and: "statistics updated to represent a new JMS event received from o1 notifications queue"
            o1HandlerStatistics.getTotalNoOfAlarmsReceived() == 1
    }

    def "Test when JMS message input with node is suspended after incrementing the notification count"() {

        given: "a valid input event with a JMS message that contains a map of alarm properties"
            def inputEvent = mock(ObjectMessage)
            inputEvent.getObject() >> ["notificationType": "notifyNewAlarm", "href": "https://NodeA.MeContext/ManagedElement=NodeA/GNBCUCPFunction=1"]

        and: "alarm flow rate is enabled"
            mockAlarmService.isAlarmFlowRateEnabled() >> true

        and: "node is not initially suspended"
            1 * mockAlarmService.isNodeSuspended("NodeA") >> false

        and: "node is suspended after its incremented"
            1 * mockAlarmService.isNodeSuspended("NodeA") >> true

        when: "the handler is invoked"
            fmNotificationHandler.onEventWithResult(inputEvent)

        then: "node is suspended"
            1 * fmNotificationHandler.alarmService.updateNodeSuspenderCache(_)

        and: "node suspended alarm is raised"
            1 * mockAlarmService.sendAlarm({
                it.getProbableCause().contains("Threshold crossed") &&
                        it.getSpecificProblem().contains("Alarm Rate Threshold Crossed") &&
                        it.getEventType().contains("Quality of service") &&
                        it.getPerceivedSeverity().contains("CRITICAL") &&
                        it.getRecordType().contains("NODE_SUSPENDED")
            })
            1 * systemRecorder.recordEvent('FM_O1_NODE_SUSPENDER', EventLevel.DETAILED, '', '', 'Node Suspended : Alarm raised for NetworkElement=NodeA')

        and: "header is added with choice operator attribute set to true"
            headers.get("isNodeSuspended") == true

        and: "statistics updated to represent a new JMS event received from o1 notifications queue"
            o1HandlerStatistics.getTotalNoOfAlarmsReceived() == 1
    }


}
