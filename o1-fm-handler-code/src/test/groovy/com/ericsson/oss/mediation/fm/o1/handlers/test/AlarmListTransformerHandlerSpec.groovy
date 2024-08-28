package com.ericsson.oss.mediation.fm.o1.handlers.test


import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.itpf.common.config.Configuration
import com.ericsson.oss.itpf.common.event.handler.EventHandlerContext
import com.ericsson.oss.mediation.flow.events.MediationComponentEvent
import com.ericsson.oss.mediation.fm.o1.engine.api.O1AlarmService
import com.ericsson.oss.mediation.fm.o1.handlers.AlarmListTransformerHandler
import com.ericsson.oss.mediation.translator.model.EventNotification

import javax.inject.Inject

class AlarmListTransformerHandlerSpec extends CdiSpecification {

    final Map<String, Object> headers = new HashMap<>()

    @ObjectUnderTest
    AlarmListTransformerHandler alarmListTransformerHandler

    @Inject
    private O1AlarmService mockAlarmService

    def setup() {
        def mockEventHandlerContext = mock(EventHandlerContext)
        def mockConfiguration = mock(Configuration)
        headers.put("nodeAddress", "node1")
        mockEventHandlerContext.getEventHandlerConfiguration() >> mockConfiguration
        mockConfiguration.getAllProperties() >> headers

        alarmListTransformerHandler.init(mockEventHandlerContext)

    }

    def "Test handler with event that contains empty payload"() {

        given: "an input event that contains an empty body"
            def inputEvent = new MediationComponentEvent(headers, "")

        when: "the handler is invoked"
            final MediationComponentEvent result = alarmListTransformerHandler.onEventWithResult(inputEvent)

        then: "the event is ignored and the payload is set to empty"
            final String payload = result.getPayload()
            assert payload.isEmpty()

        and: "the alarm is not sent to the alarm service"
            0 * mockAlarmService.translateAlarms(_)
    }

    def "Test handler translates alarm successfully"() {

        given: "a valid input event that contains a map of alarm properties"
            List<Map<String, Object>> list = new ArrayList<>()
            list.add(["prop1": "value1", "prop2": "value2"])
            list.add(["props1": "values1", "props2": "values2"])
            def inputEvent = new MediationComponentEvent(headers, list)
            inputEvent.getHeaders().put("fdn", "SubNetwork=O1,MeContext=5G141O1001,ManagedElement=1")

        and: "the alarm service transforms the map to an EventNotification"
            List<EventNotification> eventNotificationList = new ArrayList<>()
            eventNotificationList.add(new EventNotification())
            eventNotificationList.add(new EventNotification())
            mockAlarmService.translateAlarms(list, "SubNetwork=O1,MeContext=5G141O1001,") >> eventNotificationList

        when: "the handler is invoked"
            final MediationComponentEvent result = alarmListTransformerHandler.onEventWithResult(inputEvent)

        then: "the handler returns a result that contains a payload with the transformed properties as a list."
            assert result.getPayload() instanceof List

        then: "the handler returns a result that contains a payload with the transformed properties in the list as eventNotifications."
            List<Object> payload = result.getPayload()
            assert payload.get(0) instanceof EventNotification
            assert payload.get(1) instanceof EventNotification
            assert payload.get(2) instanceof EventNotification
            assert payload.get(3) instanceof EventNotification

        then: "the payload contains the synchronization alarms"
            List<EventNotification> EventNotificationpayload = result.getPayload()
            assert EventNotificationpayload.get(0).getRecordType().contains("SYNCHRONIZATION_STARTED")
            assert EventNotificationpayload.get(3).getRecordType().contains("SYNCHRONIZATION_ENDED")

    }
}

