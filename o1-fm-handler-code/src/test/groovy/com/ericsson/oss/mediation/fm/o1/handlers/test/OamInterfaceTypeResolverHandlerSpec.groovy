package com.ericsson.oss.mediation.fm.o1.handlers.test

import static com.ericsson.oss.mediation.fm.o1.handlers.util.HandlerMessages.SUPERVISION_STATUS_OR_OAM_INTERFACE_FOR_FM_NOT_FOUND

import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.itpf.common.config.Configuration
import com.ericsson.oss.itpf.common.event.ComponentEvent
import com.ericsson.oss.itpf.common.event.handler.EventHandlerContext
import com.ericsson.oss.itpf.common.event.handler.exception.EventHandlerException
import com.ericsson.oss.mediation.flow.events.MediationComponentEvent
import com.ericsson.oss.mediation.fm.o1.handlers.OamInterfaceTypeResolverHandler

class OAMInterfaceTypeResolverHandlerSpec extends CdiSpecification {

    @ObjectUnderTest
    OamInterfaceTypeResolverHandler objUnderTest = new OamInterfaceTypeResolverHandler()

    final Map<String, Object> headers = new HashMap<>()
    protected EventHandlerContext mockEventHandlerContext = Mock()

    protected Configuration mockConfiguration = Mock()

    def setup() {
        mockEventHandlerContext.getEventHandlerConfiguration() >> mockConfiguration
        mockConfiguration.getAllProperties() >> headers
    }

    def "onEvent supervisionOff"() {
        given: "headers"
            headers.put("active", false)
            headers.put("oamInterfaceForFM", "O1")
        and: "setup input event"
            def inputEvent = new MediationComponentEvent(headers, null)
            objUnderTest.init(mockEventHandlerContext)
        when: "invoke onEventWithResult"
            ComponentEvent responseEvent = objUnderTest.onEventWithResult(inputEvent) as ComponentEvent
        then: "Success response mediation component event generated"
            responseEvent.getHeaders() != null
    }

    def "onEventWithResult supervisionOn"() {
        def map = [active: true]

        given: "headers"
            headers.put("oamInterfaceForFM", "O1")
            headers.put("supervisionAttributes", map)
        and: "setup input event"
            def inputEvent = new MediationComponentEvent(headers, null)
            objUnderTest.init(mockEventHandlerContext)
        when: "invoke onEventWithResult"
            ComponentEvent responseEvent = objUnderTest.onEventWithResult(inputEvent) as ComponentEvent
        then: "Success response mediation component event generated"
            responseEvent.getHeaders() != null
    }

    def "onEventWithResult supervisionOn supervision attributes empty"() {
        given: "headers with fdn"
            headers.put("fdn", "NetworkElement=O1Node,FmAlarmSupervision=1")

        and: "setup input event"
            def inputEvent = new MediationComponentEvent(headers, null)
            objUnderTest.init(mockEventHandlerContext)
        when: "invoke onEventWithResult"
            objUnderTest.onEventWithResult(inputEvent)
        then: "an exception with an appropriate message is thrown"
            def exception = thrown(EventHandlerException)
            exception.message.contains(SUPERVISION_STATUS_OR_OAM_INTERFACE_FOR_FM_NOT_FOUND)
    }
}
