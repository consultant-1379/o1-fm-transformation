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
package com.ericsson.oss.mediation.fm.o1.handlers.test

import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.itpf.common.config.Configuration
import com.ericsson.oss.itpf.common.event.handler.EventHandlerContext
import com.ericsson.oss.itpf.common.event.handler.exception.EventHandlerException
import com.ericsson.oss.mediation.flow.events.MediationComponentEvent
import com.ericsson.oss.mediation.fm.o1.engine.api.O1AlarmService
import com.ericsson.oss.mediation.fm.o1.handlers.EventNotificationDispatchHandler
import com.ericsson.oss.mediation.translator.model.EventNotification

import javax.inject.Inject

class EventNotificationDispatchHandlerSpec extends CdiSpecification {

    private static final EMPTY_MAP = [:]

    @ObjectUnderTest
    EventNotificationDispatchHandler eventNotifDispatchHandler

    @Inject
    private O1AlarmService mockAlarmService

    def setup() {
        def mockEventHandlerContext = mock(EventHandlerContext)
        def mockConfiguration = mock(Configuration)
        mockEventHandlerContext.getEventHandlerConfiguration() >> mockConfiguration
        mockConfiguration.getAllProperties() >> EMPTY_MAP
        eventNotifDispatchHandler.init(mockEventHandlerContext)
    }

    def "Test handler with event that contains empty payload"() {

        given: "an input event that contains an empty body"
            def inputEvent = new MediationComponentEvent(EMPTY_MAP as Map<String, Object>, "")

        when: "the handler is invoked"
            final MediationComponentEvent result = eventNotifDispatchHandler.onEventWithResult(inputEvent) as MediationComponentEvent

        then: "the event is ignored and the payload is set to empty"
            final String payload = result.getPayload()
            assert payload.isEmpty()

        and: "there are no alarms sent to the alarm service"
            0 * mockAlarmService.sendAlarm(_)
    }

    def "Test handler with event that contains empty list"() {

        given: "an input event that contains an empty body"
            List<EventNotification> alarmList = new ArrayList<>()
            def inputEvent = new MediationComponentEvent(EMPTY_MAP, alarmList)

        when: "the handler is invoked"
            final MediationComponentEvent result = eventNotifDispatchHandler.onEventWithResult(inputEvent)

        then: "the event is ignored and the payload is set to empty"
            final String payload = result.getPayload()
            assert payload.isEmpty()

        and: "there are no alarms sent to the alarm service"
            0 * mockAlarmService.sendAlarms(alarmList)
    }

    def "Test handler with a valid input event"() {

        given: "a valid input event that contains a translated alarm"
            final EventNotification alarm = new EventNotification()
            def inputEvent = new MediationComponentEvent(EMPTY_MAP as Map<String, Object>, alarm)

        when: "the handler is invoked"
            final MediationComponentEvent result = eventNotifDispatchHandler.onEventWithResult(inputEvent) as MediationComponentEvent

        then: "the alarm service is successfully invoked for the translated alarm"
            1 * mockAlarmService.sendAlarm(alarm)
    }

    def "Test handler with a valid list input event"() {

        given: "a valid input event that contains a translated alarm"
            final EventNotification alarm = new EventNotification()
            List<EventNotification> testAlarmList = new ArrayList<>()
            testAlarmList.add(alarm)
            testAlarmList.add(alarm)
            testAlarmList.add(alarm)

            def inputEvent = new MediationComponentEvent(EMPTY_MAP, testAlarmList)


        when: "the handler is invoked"
            final MediationComponentEvent result = eventNotifDispatchHandler.onEventWithResult(inputEvent)

        then: "the alarm service is successfully invoked for the translated alarm"
            1 * mockAlarmService.sendAlarms(testAlarmList)
    }


    def "Test handler with an invalid event"() {

        given: "an invalid input event"

        when: "the handler is invoked"
            final MediationComponentEvent result = eventNotifDispatchHandler.onEventWithResult(null) as MediationComponentEvent

        then: "Exception was thrown from handler"
            EventHandlerException exception = thrown()
            exception.message == "Internal error - input event received is null"

    }

    def "Test handler with event that contains invalid list"() {

        given: "an input event that contains a String list"
            List<String> alarmList = new ArrayList<>()
            alarmList.add("TestValue")
            def inputEvent = new MediationComponentEvent(EMPTY_MAP, alarmList)

        when: "the handler is invoked"
            final MediationComponentEvent result = eventNotifDispatchHandler.onEventWithResult(inputEvent)

        then: "the event is ignored and the payload is set to empty"
            final String payload = result.getPayload()
            assert payload.isEmpty()

        and: "there are no alarms sent to the alarm service"
            0 * mockAlarmService.sendAlarms(alarmList)
    }
}

