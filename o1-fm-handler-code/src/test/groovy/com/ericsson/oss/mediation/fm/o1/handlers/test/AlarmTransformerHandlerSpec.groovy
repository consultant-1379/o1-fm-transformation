package com.ericsson.oss.mediation.fm.o1.handlers.test

import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.itpf.common.config.Configuration
import com.ericsson.oss.itpf.common.event.handler.EventHandlerContext
import com.ericsson.oss.mediation.flow.events.MediationComponentEvent
import com.ericsson.oss.mediation.fm.o1.engine.api.O1AlarmService
import com.ericsson.oss.mediation.fm.o1.handlers.AlarmTransformerHandler
import com.ericsson.oss.mediation.translator.model.EventNotification

import javax.inject.Inject

class AlarmTransformerHandlerSpec extends CdiSpecification {

    private static final EMPTY_MAP = [:]

    @ObjectUnderTest
    AlarmTransformerHandler alarmTransformerHandler

    @Inject
    private O1AlarmService mockAlarmService

    def setup() {
        def mockEventHandlerContext = mock(EventHandlerContext)
        def mockConfiguration = mock(Configuration)
        mockEventHandlerContext.getEventHandlerConfiguration() >> mockConfiguration
        mockConfiguration.getAllProperties() >> EMPTY_MAP
        alarmTransformerHandler.init(mockEventHandlerContext)
    }

    def "Test handler with event that contains empty payload"() {
        given: "an input event that contains an empty payload"
            def inputEvent = new MediationComponentEvent(EMPTY_MAP as Map<String, Object>, "")
        when: "the handler is invoked"
            final MediationComponentEvent result = alarmTransformerHandler.onEventWithResult(inputEvent) as MediationComponentEvent
        then: "the event is ignored and the payload is set to empty"
            final String payload = result.getPayload()
            assert payload.isEmpty()
        and: "the alarm is not sent to the alarm service"
            0 * mockAlarmService.translateAlarm(_)
    }

    def "Test handler translates alarm successfully"() {
        given: "a valid input event that contains a map of alarm properties"
            def inputEvent = new MediationComponentEvent(EMPTY_MAP as Map<String, Object>, ["prop1": "value1", "prop2": "value2"])
        and: "the alarm service transforms the map to an EventNotification"
            mockAlarmService.translateAlarm(["prop1": "value1", "prop2": "value2"]) >> new EventNotification()
        when: "the handler is invoked"
            final MediationComponentEvent result = alarmTransformerHandler.onEventWithResult(inputEvent) as MediationComponentEvent
        then: "the handler returns a result that contains a payload with the transformed properties."
            assert result.getPayload() instanceof EventNotification
    }
}

