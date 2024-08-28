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

package com.ericsson.oss.mediation.o1.controller

import javax.inject.Inject
import javax.ws.rs.core.Response

import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.itpf.sdk.core.util.ServiceIdentity
import com.ericsson.oss.itpf.sdk.eventbus.Channel
import com.ericsson.oss.itpf.sdk.eventbus.ChannelLocator
import com.ericsson.oss.itpf.sdk.eventbus.EventConfigurationBuilder
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder
import com.ericsson.oss.mediation.o1.event.exception.O1ValidationException
import com.ericsson.oss.mediation.o1.event.resources.O1NotificationReceiver

import spock.lang.Unroll

class O1NotificationReceiverSpec extends CdiSpecification {


    @Inject
    private ServiceIdentity mockServiceIdentity

    @Inject
    private ChannelLocator mockChannelLocator

    @Inject
    private SystemRecorder systemRecorder;

    @MockedImplementation
    private Channel mockChannel

    @ObjectUnderTest
    private O1NotificationReceiver o1NotificationReceiver;

    def setup() {
        mockServiceIdentity.getNodeId() >> "svc-1-mssnmpfm"
        mockChannelLocator.lookupAndConfigureChannel(_, _) >> mockChannel
    }

    @Unroll
    def "Test valid VES Notification is processed successfully"() {

        given: "A single VES notification is received"
        final String notification = getVesNotification(namespace)

        when: "The VES notification is validated"
        final Response response = o1NotificationReceiver.processNotification(notification)

        then: "The response matches the expected response"
        response.getStatus() == 202

        and: "a normalized notification and expected ExpectedConfiguration is sent to JMS"
        1 * mockChannel.send({
            it.toString() == getExpectedNormalizedVesNotification(namespace).toString()
        }, {
            it.toString() == getExpectedEventConfiguration()
        })
        where: "list of valid stndDefinedNamespace types"
        namespace << [
            '3GPP-FaultSupervision',
            '3GPP-Heartbeat'
        ]
    }

    def "Test valid 3GPP-specified O1 notification is processed successfully"() {

        given: "A single 3GPP-specified O1 notification is received"
        final String notification = get3GPPNotification()

        when: "The 3GPP-specified O1 notification is validated"
        final Response response = o1NotificationReceiver.processNotification(notification)

        then: "The response matches the expected response"
        response.getStatus() == 202

        and: "a normalized notification and expected ExpectedConfiguration is sent"
        1 * mockChannel.send({
            it.toString() == getExpectedNormalized3GPPNotification().toString()
        }, {
            it.toString() == getExpectedEventConfiguration()
        })
    }

    def "Test valid heartbeat notification is processed successfully"() {

        given: "A single VES notification is received for stndDefinedNamespace = 3GPP-Heartbeat"
        final String notification = getHeartbeatNotification()

        when: "The Ves notification is validated"
        final Response response = o1NotificationReceiver.processNotification(notification)

        then: "The response matches the expected response"
        response.getStatus() == 202

        and: "a normalized notification and expected ExpectedConfiguration is sent"
        1 * mockChannel.send({
            it.toString() == getExpectedNormalizedHeartbeatNotification().toString()
        }, {
            it.toString() == getExpectedEventConfiguration()
        })
    }

    def "Test invalid VES Notification produces an exception"() {

        given: "A VES notification with an invalid stndDefinedNamespace type is received"
        final String notification = getVesNotification("3GPP-Unknown")

        when: "The VES notification is validated"
        o1NotificationReceiver.processNotification(notification)

        then: "An exception is thrown"
        O1ValidationException o1Exception = thrown()
        o1Exception.message.contains('Schema validation failed:')
    }

    private String getVesNotification(final String namespace) {
        return "{\n" +
                    "  \"event\": {\n" +
                    "    \"commonEventHeader\": {\n" +
                    "      \"domain\": \"stndDefined\",\n" +
                    "      \"eventId\": \"1678271515731561767\",\n" +
                    "      \"eventName\": \"stndDefined_Vscf:Acs-Ericcson_ProcessingErrorAlarm\",\n" +
                    "      \"startEpochMicrosec\": 1678271515731562201,\n" +
                    "      \"lastEpochMicrosec\": 1678271515731562201,\n" +
                    "      \"priority\": \"Normal\",\n" +
                    "      \"reportingEntityName\": \"ibcx0001vm002oam001\",\n" +
                    "      \"sequence\": 0,\n" +
                    "      \"sourceName\": \"scfx0001vm002cap001\",\n" +
                    "      \"version\": \"4.1\",\n" +
                    "      \"vesEventListenerVersion\": \"7.2\",\n" +
                    "      \"stndDefinedNamespace\": " + namespace + ",\n" +
                    "      \"timeZoneOffset\": \"UTC-05.00\"\n" +
                    "    },\n" +
                    "    \"stndDefinedFields\": {\n" +
                    "      \"schemaReference\": \"https://forge.3gpp.org/rep/sa5/MnS/-/blob/Rel-18/OpenAPI/TS28532_FaultMnS.yaml#/components/schemas/NotifyNewAlarm\",\n" +
                    "      \"data\": {\n" +
                    "        \"additionalInformation\": {\n" +
                    "              \"additionalKeyOne\": \"additionalValue1\",\n" +
                    "              \"additionalKeyTwo\": \"additionalValue2\"\n" +
                    "        },\n" +
                    "        \"additionalText\": \"Connection lost for Service Discovery Interfaces listed in Additional Information.\",\n" +
                    "        \"alarmId\": \"9cf9a4a0-5271-490d-87ce-3727d823f32c\",\n" +
                    "        \"alarmType\": \"PROCESSING_ERROR_ALARM\",\n" +
                    "        \"eventTime\": \"2023-03-08T10:31:55.731560233Z\",\n" +
                    "        \"href\": \"https://customerA.com/SubNetwork=customerA-sub-network/MeContext=customerA-me-context/ManagedElement=1/GNBCUCPFunction=1\",\n" +
                    "        \"notificationId\": 1336781711,\n" +
                    "        \"notificationType\": \"notifyNewAlarm\",\n" +
                    "        \"perceivedSeverity\": \"MAJOR\",\n" +
                    "        \"probableCause\": 307,\n" +
                    "        \"systemDN\": \"ManagedElement=1,MnsAgent=FM\"\n" +
                    "      },\n" +
                    "      \"stndDefinedFieldsVersion\": \"1.0\"\n" +
                    "    }\n" +
                    "  }\n" +
                "}";
    }

    HashMap<String, Object> getExpectedNormalizedVesNotification(String namespace) {
        return new HashMap<>(
                        startEpochMicrosec: 1678271515731562201,
                        additionalText: "Connection lost for Service Discovery Interfaces listed in Additional Information.",
                        timeZoneOffset: "UTC-05.00",
                        notificationType: "notifyNewAlarm",
                        schemaReference: "https://forge.3gpp.org/rep/sa5/MnS/-/blob/Rel-18/OpenAPI/TS28532_FaultMnS.yaml#/components/schemas/NotifyNewAlarm",
                        perceivedSeverity: "MAJOR",
                        probableCause: 307,
                        alarmId: "9cf9a4a0-5271-490d-87ce-3727d823f32c",
                        eventTime: "2023-03-08T10:31:55.731560233Z",
                        eventName: "stndDefined_Vscf:Acs-Ericcson_ProcessingErrorAlarm",
                        vesEventListenerVersion: "7.2",
                        notificationId: 1336781711,
                        href: "https://customerA.com/SubNetwork=customerA-sub-network/MeContext=customerA-me-context/ManagedElement=1/GNBCUCPFunction=1",
                        eventId: 1678271515731561767,
                        additionalInformation: "[additionalKeyOne:additionalValue1, additionalKeyTwo:additionalValue2]",
                        priority: "Normal",
                        version: "4.1",
                        reportingEntityName: "ibcx0001vm002oam001",
                        sequence: 0,
                        stndDefinedFieldsVersion: "1.0",
                        systemDN: "ManagedElement=1,MnsAgent=FM",
                        alarmType: "PROCESSING_ERROR_ALARM",
                        domain: "stndDefined",
                        lastEpochMicrosec: 1678271515731562201,
                        sourceName: "scfx0001vm002cap001",
                        stndDefinedNamespace: namespace
                        )
    }

    private String getExpectedEventConfiguration() {
        final EventConfigurationBuilder eventConfigurationBuilder = new EventConfigurationBuilder();
        eventConfigurationBuilder.addEventProperty("__target_ms_instance", "svc-1-mssnmpfm");
        return eventConfigurationBuilder.build().toString();
    }

    private String get3GPPNotification() {
        return "{\n" +
                    "  \"additionalInformation\": {\n" +
                    "    \"additionalKeyOne\": \"additionalValue1\",\n" +
                    "    \"additionalKeyTwo\": \"additionalValue2\"\n" +
                    "   },\n" +
                    "  \"additionalText\": \"Connection lost for Service Discovery Interfaces listed in Additional Information.\",\n" +
                    "  \"alarmId\": \"9cf9a4a0-5271-490d-87ce-3727d823f32c\",\n" +
                    "  \"alarmType\": \"PROCESSING_ERROR_ALARM\",\n" +
                    "  \"eventTime\": \"2023-03-08T10:31:55.731560233Z\",\n" +
                    "  \"href\": \"https://customerA.com/SubNetwork=customerA-sub-network/MeContext=customerA-me-context/ManagedElement=1/GNBCUCPFunction=1\",\n" +
                    "  \"notificationId\": 1336781711,\n" +
                    "  \"notificationType\": \"notifyNewAlarm\",\n" +
                    "  \"perceivedSeverity\": \"MAJOR\",\n" +
                    "  \"probableCause\": 307,\n" +
                    "  \"systemDN\": \"ManagedElement=1,MnsAgent=FM\"\n" +
                    "  }";
    }

    HashMap<String, Object> getExpectedNormalized3GPPNotification() {
        return new HashMap<>(
                        additionalInformation: "[additionalKeyOne:additionalValue1, additionalKeyTwo:additionalValue2]",
                        additionalText: "Connection lost for Service Discovery Interfaces listed in Additional Information.",
                        alarmId: "9cf9a4a0-5271-490d-87ce-3727d823f32c",
                        alarmType: "PROCESSING_ERROR_ALARM",
                        eventTime: "2023-03-08T10:31:55.731560233Z",
                        href: "https://customerA.com/SubNetwork=customerA-sub-network/MeContext=customerA-me-context/ManagedElement=1/GNBCUCPFunction=1",
                        notificationId: 1336781711,
                        notificationType: "notifyNewAlarm",
                        perceivedSeverity: "MAJOR",
                        probableCause: 307,
                        systemDN: "ManagedElement=1,MnsAgent=FM",
                        )
    }

    private String getHeartbeatNotification() {
        return "{\n" +
                    "  \"event\": {\n" +
                    "    \"commonEventHeader\": {\n" +
                    "      \"domain\": \"stndDefined\",\n" +
                    "      \"eventId\": \"111\",\n" +
                    "      \"eventName\": \"stndDefined_Vscf:Acs-Ericcson_heartbeat\",\n" +
                    "      \"startEpochMicrosec\": 1678270390895435544,\n" +
                    "      \"lastEpochMicrosec\": 1678270390895435544,\n" +
                    "      \"priority\": \"Normal\",\n" +
                    "      \"reportingEntityName\": \"ibcx0001vm002oam001\",\n" +
                    "      \"sequence\": 0,\n" +
                    "      \"sourceName\": \"scfx0001vm002cap001\",\n" +
                    "      \"version\": \"4.1\",\n" +
                    "      \"vesEventListenerVersion\": \"7.2\",\n" +
                    "      \"stndDefinedNamespace\": \"3GPP-Heartbeat\",\n" +
                    "      \"timeZoneOffset\": \"UTC-05.00\"\n" +
                    "    },\n" +
                    "    \"stndDefinedFields\": {\n" +
                    "      \"schemaReference\": \"https://forge.3gpp.org/rep/sa5/MnS/-/blob/Tag_Rel18_SA97/OpenAPI/TS28532_HeartbeatNtf.yaml#/components/schemas/NotifyHeartbeat\",\n" +
                    "      \"data\": {\n" +
                    "        \"eventTime\": \"2023-11-23T12:41:35.407Z\",\n" +
                    "        \"heartbeatNtfPeriod\": 100,\n" +
                    "        \"href\": \"https://ocp83vcu03o1.MeContext/ManagedElement=ocp83vcu03o1/GNBCUCPFunction=1\",\n" +
                    "        \"notificationId\": 111,\n" +
                    "        \"notificationType\": \"notifyHeartbeat\",\n" +
                    "        \"systemDN\": \"ManagedElement=1,MnsAgent=FM\"\n" +
                    "      },\n" +
                    "      \"stndDefinedFieldsVersion\": \"1.0\"\n" +
                    "    }\n" +
                    "  }\n" +
                    "}";
    }

    HashMap<String, Object> getExpectedNormalizedHeartbeatNotification() {
        return new HashMap<>(
                        startEpochMicrosec:1678270390895435544,
                        eventId:"111",
                        timeZoneOffset:"UTC-05.00",
                        notificationType:"notifyHeartbeat",
                        priority:"Normal",
                        version:"4.1",
                        schemaReference:"https://forge.3gpp.org/rep/sa5/MnS/-/blob/Tag_Rel18_SA97/OpenAPI/TS28532_HeartbeatNtf.yaml#/components/schemas/NotifyHeartbeat",
                        reportingEntityName:"ibcx0001vm002oam001",
                        sequence:0,
                        stndDefinedFieldsVersion:"1.0",
                        systemDN:"ManagedElement=1,MnsAgent=FM",
                        heartbeatNtfPeriod:100,
                        domain:"stndDefined",
                        lastEpochMicrosec:1678270390895435544,
                        eventTime:"2023-11-23T12:41:35.407Z",
                        eventName:"stndDefined_Vscf:Acs-Ericcson_heartbeat",
                        vesEventListenerVersion:"7.2",
                        notificationId:111,
                        sourceName:"scfx0001vm002cap001",
                        stndDefinedNamespace:"3GPP-Heartbeat",
                        href:"https://ocp83vcu03o1.MeContext/ManagedElement=ocp83vcu03o1/GNBCUCPFunction=1"
                        )
    }
}
