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
package com.ericsson.oss.mediation.fm.o1.engine.service

import com.ericsson.cds.cdi.support.providers.stubs.InMemoryCache
import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.itpf.datalayer.dps.stub.RuntimeConfigurableDps
import com.ericsson.oss.itpf.sdk.cache.classic.CacheProviderBean
import com.ericsson.oss.itpf.sdk.recording.ErrorSeverity
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder
import com.ericsson.oss.mediation.fm.o1.engine.instrumentation.O1EngineStatistics
import com.ericsson.oss.mediation.fm.o1.engine.nodesuspender.config.NodeSuspenderConfigurationListener
import com.ericsson.oss.mediation.fm.o1.engine.service.config.LocalConfigRepoRule
import com.ericsson.oss.mediation.fm.o1.engine.transform.ModelTransformerException
import com.ericsson.oss.mediation.fm.o1.engine.transform.TransformManager
import com.ericsson.oss.mediation.fm.o1.engine.transform.TransformManagerStartup
import com.ericsson.oss.mediation.fm.o1.engine.transform.TransformerException
import com.ericsson.oss.mediation.fm.o1.engine.transform.helper.AlarmRecords
import com.ericsson.oss.mediation.fm.o1.engine.transform.helper.NotifyAlarm
import com.ericsson.oss.mediation.o1.api.NodeSuspensionEntry
import com.ericsson.oss.mediation.translator.model.EventNotification
import org.junit.Rule
import spock.lang.Unroll

import javax.inject.Inject
import java.beans.BeanInfo
import java.beans.Introspector
import java.beans.PropertyDescriptor
import java.lang.reflect.Method

import static com.ericsson.oss.itpf.sdk.recording.EventLevel.DETAILED
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.fail

class O1AlarmServiceImplSpec extends CdiSpecification {

    static private final String NETWORK_ELEMENT_FDN = "NetworkElement=NodeA"

    @ObjectUnderTest
    private O1AlarmServiceImpl o1AlarmService

    @MockedImplementation
    private FmEventBufferFacade fmEventBufferFacade

    @Rule
    private LocalConfigRepoRule localConfigRepoRule

    @Inject
    private TransformManagerStartup transformManagerStartup

    @Inject
    private TransformManager transformManager

    @Inject
    private SystemRecorder systemRecorder

    @Inject
    private NodeSuspenderConfigurationListener nodeSuspenderConfigurationListener

    @Inject
    O1EngineStatistics o1EngineStatistics

    def nodeSuspensionCache = new InMemoryCache<String, NodeSuspensionEntry>('O1NodeSuspenderCache')

    @MockedImplementation
    private CacheProviderBean cacheProviderBean

    def setup() {
        RuntimeConfigurableDps configurableDps = cdiInjectorRule.getService(RuntimeConfigurableDps)
        configurableDps.addManagedObject()
                .type("MeContext")
                .name("5G141O1001")
                .build()
    }

    def "Test translate alarm when alarm map is null"() {

        when: "translateAlarm is called with null alarm map"
            o1AlarmService.translateAlarm(null)

        then: "TransformerException is thrown"
            def e = thrown(TransformerException.class)
            assert e.message.contains("Received request to transform alarm with no properties")
    }

    def "Test translate empty alarm list"() {

        given: "an empty arrayList"
            final List<Map<String, Object>> alarmList = new ArrayList<>()

        when: "translateAlarm is called with empty alarm map"
            o1AlarmService.translateAlarms(alarmList, null)

        then: "TransformerException is thrown"
            def e = thrown(TransformerException.class)
            assert e.message.contains("Received request to transform null or empty alarms list")
    }

    def "Test translate null alarm list"() {

        given: "an empty arrayList"
            final List<Map<String, Object>> alarmList = null;

        when: "translateAlarm is called with empty alarm map"
            o1AlarmService.translateAlarms(alarmList, null)

        then: "TransformerException is thrown"
            def e = thrown(TransformerException.class)
            assert e.message.contains("Received request to transform null or empty alarms list")
    }

    @Unroll
    def "Test valid notification #notificationType gets transformed correctly"() {

        given: "a valid NotifyAlarm"
            def alarmProperties = NotifyAlarm.createNotifyAlarm(notificationType)

        and: "TransformManager is initialized"
            assert transformManagerStartup.isInitialized()

        when: "the alarm service is called to translate alarm"
            final EventNotification event = o1AlarmService.translateAlarm(alarmProperties)

        then: "a transformed alarm is returned"
            assertEventNotificationEquals(createExpectedTransformedEventNotification(notificationType), event)

        and: "successful transformation statistics updated"
            o1EngineStatistics.getTotalNoOfSuccessfulTransformations() == 1

        where: "notificationType to transform is"
            notificationType            | _
            "notifyClearedAlarm"        | _
            "notifyNewAlarm"            | _
            "notifyChangedAlarmGeneral" | _
            "notifyChangedAlarm"        | _
            "notifyAlarmListRebuilt"    | _
    }

    def "Test valid list of AlarmList.alarmRecords gets transformed correctly"() {

        given: "a valid list of alarmRecords"
            def alarmRecords = AlarmRecords.ALARM_RECORDS;

        and: "TransformManager is initialized"
            assert transformManagerStartup.isInitialized()

        when: "the alarm service is called to translate the AlarmRecords"
            final List<EventNotification> event = o1AlarmService.translateAlarms(alarmRecords, AlarmRecords.OBJECT_INSTANCE_FDN);

        then: "a list of transformed alarm is returned"
            assertEventNotificationEquals(createExpectedTransformedEventNotificationList().get(0), event.get(0))
            assertEventNotificationEquals(createExpectedTransformedEventNotificationList().get(1), event.get(1))
            assertEventNotificationEquals(createExpectedTransformedEventNotificationList().get(2), event.get(2))

        and: "successful transformation statistics updated"
            o1EngineStatistics.getTotalNoOfSuccessfulTransformations() == 3
    }

    def "Test translate alarm when alarm map is missing href"() {

        given: "an invalid alarm when href is not present"
            def alarmProperties = NotifyAlarm.createNotifyAlarm("notifyNewAlarm")
            alarmProperties.remove("href")

        and: "TransformManager is initialized"
            assert transformManagerStartup.isInitialized()

        when: "the alarm service is called"
            final EventNotification event = o1AlarmService.translateAlarm(alarmProperties)

        then: "ModelTransformerException is thrown"
            def e = thrown(ModelTransformerException.class)
            assert e.getCause().cause.message.contains("Alarm is missing mandatory 'href' field")

        and: "transformation statistics not updated because of error"
            o1EngineStatistics.getTotalNoOfSuccessfulTransformations() == 0
    }

    def "Test translated alarm is sent to FM"() {

        given: "a translated alarm"
            final EventNotification event = new EventNotification()
            event.setManagedObjectInstance("ManagedElement=Node1")

        when: "the alarm service is called"
            o1AlarmService.sendAlarm(event)

        then: "the alarm service is successfully invoked for the translated alarm"
            1 * fmEventBufferFacade.sendEvent(_)

        and: "transformation statistics not updated because of error"
            o1EngineStatistics.getTotalNoOfForwardedAlarmEventNotifications() == 1
    }

    def "Test translated alarm list is sent to FM"() {

        given: "a translated alarm"
            List<EventNotification> eventList = new ArrayList<>();
            final EventNotification event = new EventNotification();
            event.setRecordType("SYNCHRONIZATION_ALARM")
            eventList.add(event)

        when: "the alarm service is called"
            o1AlarmService.sendAlarms(eventList)

        then: "the alarm service is successfully invoked for the translated alarm"
            1 * fmEventBufferFacade.sendEventList(eventList)

        and: "transformation statistics not updated because of error"
            o1EngineStatistics.getTotalNoOfForwardedSyncAlarmEventNotifications() == 1
    }

    def "Test raising a heartbeat alarm"() {

        when: "request sent to alarm service to raise heartbeat alarm"
            o1AlarmService.raiseHeartbeatAlarm(NETWORK_ELEMENT_FDN)

        then: "request received to raise the heartbeat alarm"
            1 * systemRecorder.recordEvent('FM_O1_ALARM_SERVICE', DETAILED, '', NETWORK_ELEMENT_FDN, 'Heartbeat Failed : Alarm raised for ' + NETWORK_ELEMENT_FDN);

        and: "raised heartbeat increments total number of heratbeat failures"
            o1EngineStatistics.getTotalNoOfHeartbeatFailures() == 1

        and: "the raise heartbeat alarm is sent"
            1 * fmEventBufferFacade.sendEvent({
                it.toString().contains("eventType=Communications alarm, probableCause=LAN Error/Communication Error, perceivedSeverity=CRITICAL,") &&
                        it.getManagedObjectInstance().equals(NETWORK_ELEMENT_FDN) && it.getSourceType().equals("O1Node") && it.getEventTime() != null
            })
    }

    def "Test clearing a heartbeat alarm"() {

        when: "request sent to alarm service to clear the raised heartbeat alarm"
            o1AlarmService.raiseHeartbeatAlarm(NETWORK_ELEMENT_FDN)
            o1EngineStatistics.getTotalNoOfHeartbeatFailures() == 1
            o1AlarmService.clearHeartbeatAlarm(NETWORK_ELEMENT_FDN)

        then: "request received to clear the heartbeat alarm"
            1 * systemRecorder.recordEvent("FM_O1_ALARM_SERVICE", DETAILED, "", NETWORK_ELEMENT_FDN, "Clearing heartbeat alarm for " + NETWORK_ELEMENT_FDN);

        and: "clear heartbeat decrements the total number of heartbeat failures"
            o1EngineStatistics.getTotalNoOfHeartbeatFailures() == 0

        and: "the clear heartbeat alarm is sent"
            1 * fmEventBufferFacade.sendEvent({
                it.toString().contains("eventType=Communications alarm, probableCause=LAN Error/Communication Error, perceivedSeverity=CLEARED,") &&
                        it.getManagedObjectInstance().equals(NETWORK_ELEMENT_FDN) && it.getSourceType().equals("O1Node") && it.getEventTime() != null
            })
    }

    def "Test that other alarms are transformed when an alarm in the list throws an exception"() {

        given: "a list of alarms to be transformed including an invalid alarm"
            def alarmRecords = AlarmRecords.FAULTY_ALARM_RECORDS;
            assert alarmRecords.size() == 4

        when: "the alarm service is called"
            def transformedAlarm = o1AlarmService.translateAlarms(alarmRecords, AlarmRecords.OBJECT_INSTANCE_FDN)

        then: "the alarm service is successfully invoked for the translated alarm"
            assertEventNotificationEquals(createExpectedTransformedEventNotificationList().get(0), transformedAlarm.get(0))
            assertEventNotificationEquals(createExpectedTransformedEventNotificationList().get(1), transformedAlarm.get(1))
            assertEventNotificationEquals(createExpectedTransformedEventNotificationList().get(2), transformedAlarm.get(2))
            assert transformedAlarm.size() == 3
            1 * systemRecorder.recordError("Issue transforming alarms", ErrorSeverity.WARNING, "ModelTransformer", "", "Transformation failed for the alarm with the following ID: " + 1,)

        and: "the successful transformation statistics are recorded"
            o1EngineStatistics.getTotalNoOfSuccessfulTransformations() == 3
    }

    def "Test that a Network Element is added to node suspender cache"() {

        given: "A cache created with name O1NodeSuspenderCache"
            cacheProviderBean.createOrGetModeledCache('O1NodeSuspenderCache') >> nodeSuspensionCache

        when: "the alarm service addToNodeSuspenderCache is called with an NE id"
            o1AlarmService.addToNodeSuspenderCache("Node1")

        then: "the cache is successfully updated with the NE id"
            nodeSuspensionCache.size() == 1
            nodeSuspensionCache.get("Node1").getCount() == 0

        and: "Supervised nodes updated if new FDN entry added to cache"
            o1EngineStatistics.getTotalNoOfSupervisedNodes() == 1
    }

    def "Test that a Network Element is removed from node suspender cache"() {

        given: "A cache created with name O1NodeSuspenderCache"
            cacheProviderBean.createOrGetModeledCache('O1NodeSuspenderCache') >> nodeSuspensionCache

        and: "the cache contains an entry"
            o1AlarmService.addToNodeSuspenderCache("Node1")

        when: "the alarm service removeFromNodeSuspenderCache is called with an NE id"
            o1AlarmService.removeFromNodeSuspenderCache("Node1")

        then: "the cache is successfully updated with entry removed"
            nodeSuspensionCache.size() == 0

        and: "Supervised nodes decreased if entry removed from cache"
            o1EngineStatistics.getTotalNoOfSupervisedNodes() == 0
    }

    def "Test that for a Network element node suspender counter cache is updated"() {

        given: "A cache created with name O1NodeSuspenderCache"
            cacheProviderBean.createOrGetModeledCache('O1NodeSuspenderCache') >> nodeSuspensionCache

        and: "the cache contains an entry"
            o1AlarmService.addToNodeSuspenderCache("Node1")

        when: "the alarm service updateNodeSuspenderCache is called with an NE id"
            o1AlarmService.updateNodeSuspenderCache("Node1")

        then: "notification count for cache is increased for the node"
            nodeSuspensionCache.get("Node1").getCount() == 1
    }

    def "Test that for a Network element node suspender counter cache is incremented for every call"() {

        given: "A cache created with name O1NodeSuspenderCache"
            cacheProviderBean.createOrGetModeledCache('O1NodeSuspenderCache') >> nodeSuspensionCache

        and: "the cache contains two entries"
            o1AlarmService.addToNodeSuspenderCache("Node1")
            o1AlarmService.addToNodeSuspenderCache("Node2")

        when: "the alarm service updateNodeSuspenderCache is called with multiple NE ids"
            o1AlarmService.updateNodeSuspenderCache("Node1")
            o1AlarmService.updateNodeSuspenderCache("Node1")
            o1AlarmService.updateNodeSuspenderCache("Node1")
            o1AlarmService.updateNodeSuspenderCache("Node2")

        then: "the alarm service is successfully invoked for the translated alarm"
            nodeSuspensionCache.get("Node1").getCount() == 3
            nodeSuspensionCache.get("Node2").getCount() == 1

        and: "Supervised nodes updated if new entries with unique node FDNs added to cache"
            o1EngineStatistics.getTotalNoOfSupervisedNodes() == 2

    }

    def "Test resetting the node suspender cache"() {

        given: "A cache created with name O1NodeSuspenderCache"
            cacheProviderBean.createOrGetModeledCache('O1NodeSuspenderCache') >> nodeSuspensionCache

        and: "The alarm rate threshold is set to 2"
            nodeSuspenderConfigurationListener.alarmRateThreshold = 2

        and: "the cache is populated for two nodes"
            o1AlarmService.addToNodeSuspenderCache("Node1")
            o1AlarmService.addToNodeSuspenderCache("Node2")
            o1AlarmService.updateNodeSuspenderCache("Node1")
            o1AlarmService.updateNodeSuspenderCache("Node1")
            o1AlarmService.updateNodeSuspenderCache("Node1")
            o1AlarmService.updateNodeSuspenderCache("Node2")
            nodeSuspensionCache.get("Node1").getCount() == 3
            nodeSuspensionCache.get("Node2").getCount() == 1

        when: "the alarm service is called"
            o1AlarmService.resetNodeSuspenderCache()

        then: "the cache entries are reset for all nodes"
            nodeSuspensionCache.get("Node1").getCount() == 0
            nodeSuspensionCache.get("Node2").getCount() == 0
            o1AlarmService.isNodeSuspended("Node1")
            !o1AlarmService.isNodeSuspended("Node2")
    }


    def "Test that the cache is updated correctly when alarm flow threshold exceeded"() {

        given: "A cache created with name O1NodeSuspenderCache"
            cacheProviderBean.createOrGetModeledCache('O1NodeSuspenderCache') >> nodeSuspensionCache

        and: "the cache contains an entry"
            o1AlarmService.addToNodeSuspenderCache("Node1")

        and: "alarmRateThreshold is set"
            nodeSuspenderConfigurationListener.alarmRateThreshold = 1

        and: "notification count is initially 0"
            nodeSuspensionCache.get("Node1").getCount() == 0

        when: "the alarm service is called to update the cache"
            o1AlarmService.updateNodeSuspenderCache("Node1")

        then: "notification count in increased"
            nodeSuspensionCache.get("Node1").getCount() == 1

        and: "node suspended status is returned as true"
            o1AlarmService.isNodeSuspended("Node1")

    }

    def "Test that the cache is updated correctly when alarm flow threshold not exceeded"() {

        given: "A cache created with name O1NodeSuspenderCache"
            cacheProviderBean.createOrGetModeledCache('O1NodeSuspenderCache') >> nodeSuspensionCache

        and: "the cache contains an entry"
            o1AlarmService.addToNodeSuspenderCache("Node1")

        and: "alarmRateThreshold is set"
            nodeSuspenderConfigurationListener.alarmRateThreshold = 10

        and: "notification count is initially 0"
            nodeSuspensionCache.get("Node1").getCount() == 0

        when: "the alarm service is called to update the cache"
            o1AlarmService.updateNodeSuspenderCache("Node1")

        then: "notification count in increased"
            nodeSuspensionCache.get("Node1").getCount() == 1

        and: "node suspended status is returned as false"
            !o1AlarmService.isNodeSuspended("Node1")

    }
    
    def "Test call to check alarm flow rate"() {
        when: "alarm flow rate is enabled"
            nodeSuspenderConfigurationListener.alarmRateFlowControl = true

        then: "assert alarm flow rate check returns true"
            o1AlarmService.isAlarmFlowRateEnabled()
    }


    private static void assertEventNotificationEquals(EventNotification en1, EventNotification en2) {
        assertNotNull(en1);
        assertNotNull(en2);
        try {
            BeanInfo beanInfo = Introspector.getBeanInfo(EventNotification.class)
            PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors()
            for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
                Method readMethod = propertyDescriptor.getReadMethod();
                Object v1 = readMethod.invoke(en1)
                Object v2 = readMethod.invoke(en2)
                assertEquals(v1, v2)
            }
        } catch (Exception ex) {
            ex.printStackTrace()
            fail()
        }
    }

    private static EventNotification createExpectedTransformedEventNotification(String notificationType) {
        EventNotification en = new EventNotification()
        en.setRecordType("ALARM")
        en.setAcknowledged(false)
        en.addAdditionalAttribute("vesEventId", "fault000001");
        en.addAdditionalAttribute("vesEventName", "stndDefined_Vscf:Acs-Ericsson_ProcessingErrorAlarm-00001")
        en.addAdditionalAttribute("vesEventType", "stndDefined_Vscf:Acs-Ericsson_ProcessingErrorAlarm")
        en.addAdditionalAttribute("vesStartEpochMicrosec", "1698399774107999200")
        en.addAdditionalAttribute("vesLastEpochMicrosec", "1678271515731560000")
        en.addAdditionalAttribute("vesPriority", "Normal")
        en.addAdditionalAttribute("vesReportingEntityName", "ibcx0001vm002oam001")
        en.addAdditionalAttribute("vesSequence", "5")
        en.addAdditionalAttribute("vesSourceName", "scfx0001vm002cap001")
        en.addAdditionalAttribute("vesVersion", "4.1")
        en.addAdditionalAttribute("vesEventListenerVersion", "7.2")
        en.addAdditionalAttribute("notificationId", "1234567")
        en.addAdditionalAttribute("fdn", "NetworkElement=5G141O1001")
        en.setManagedObjectInstance("SubNetwork=top,SubNetwork=skylight,MeContext=5G141O1001")
        en.setEventTime("20230906113201.743")
        en.setTimeZone("GMT+00:00")
        en.addAdditionalAttribute("systemDN", "ManagedElement=1,MnsAgent=FM")
        en.addAdditionalAttribute("generatedAlarmId", "9cf9a4a0-5271-490d-87ce-3727d823f32c")
        en.setEventAgentId("9cf9a4a0-5271-490d-87ce-3727d823f32c")
        en.setEventType("PROCESSING_ERROR_ALARM")
        en.setPerceivedSeverity("MAJOR")
        en.setProbableCause("m3100CallSetupFailure")

        if (notificationType.matches("notifyNewAlarm|notifyChangedAlarmGeneral|notifyChangedAlarm")) {
            en.setSpecificProblem("some problem")
            en.addAdditionalAttribute("additionalKeyOne", "additionalValue1")
            en.addAdditionalAttribute("additionalKeyTwo", "additionalValue2")
        }

        if (notificationType.matches("notifyClearedAlarm")) {
            en.setSpecificProblem(null)
        }

        if (notificationType.matches("notifyAlarmListRebuilt")) {
            en.addAdditionalAttribute("reason", "System restarts")
            en.setRecordType("CLEARALL")
            en.setProbableCause("reinitialized")
            en.setEventType("PROCESSING ERROR")
            en.setPerceivedSeverity("WARNING")
            en.setSpecificProblem("NE and OSS alarms are not in sync")
        }

        return en
    }

    private static List<EventNotification> createExpectedTransformedEventNotificationList() {
        List<EventNotification> enList = new ArrayList<>()
        EventNotification enOne = new EventNotification()
        enOne.setRecordType("SYNCHRONIZATION_ALARM")
        enOne.setAcknowledged(false)
        enOne.addAdditionalAttribute("generatedAlarmId", "dd03de9f-1fa3-4733-8688-9b2f23b6d13b")
        enOne.addAdditionalAttribute("notificationId", "599801250")
        enOne.addAdditionalAttribute("fdn", "NetworkElement=5G141O1001")
        enOne.setManagedObjectInstance("SubNetwork=5G141O1001,MeContext=5G141O1001,ManagedElement=1,GNBDUFunction=2,NRSectorCarrier=3")
        enOne.setEventTime("20231020070127.020")
        enOne.setEventAgentId("dd03de9f-1fa3-4733-8688-9b2f23b6d13b")
        enOne.setEventType("COMMUNICATIONS_ALARM")
        enOne.setPerceivedSeverity("MINOR")
        enOne.setProbableCause("m3100Unavailable")
        enOne.setSpecificProblem("GNBCUCP Service Unavailable")
        enList.add(enOne)

        EventNotification enTwo = new EventNotification()
        enTwo.setRecordType("SYNCHRONIZATION_ALARM")
        enTwo.setAcknowledged(false)
        enTwo.addAdditionalAttribute("generatedAlarmId", "dd03de9f-1fa3-4733-8688-9b2f23b6d13c")
        enTwo.addAdditionalAttribute("notificationId", "599801250")
        enTwo.addAdditionalAttribute("fdn", "NetworkElement=5G141O1001")
        enTwo.setManagedObjectInstance("SubNetwork=5G141O1001,MeContext=5G141O1001,ManagedElement=1,GNBDUFunction=2,NRSectorCarrier=3")
        enTwo.setEventTime("20231020070127.020")
        enTwo.setEventAgentId("dd03de9f-1fa3-4733-8688-9b2f23b6d13c")
        enTwo.setEventType("COMMUNICATIONS_ALARM")
        enTwo.setPerceivedSeverity("WARNING")
        enTwo.setProbableCause("m3100CallSetupFailure")
        enTwo.setSpecificProblem("GNBCUUP Service Unavailable")
        enList.add(enTwo)

        EventNotification enThree = new EventNotification()
        enThree.setRecordType("SYNCHRONIZATION_ALARM")
        enThree.setAcknowledged(false)
        enThree.addAdditionalAttribute("generatedAlarmId", "dd03de9f-1fa3-4733-8688-9b2f23b6d13d")
        enThree.addAdditionalAttribute("notificationId", "599801250")
        enThree.addAdditionalAttribute("fdn", "NetworkElement=5G141O1001")
        enThree.setManagedObjectInstance("SubNetwork=5G141O1001,MeContext=5G141O1001,ManagedElement=1,GNBDUFunction=2,NRSectorCarrier=3")
        enThree.setEventTime("20231020070127.020")
        enThree.setEventAgentId("dd03de9f-1fa3-4733-8688-9b2f23b6d13d")
        enThree.setEventType("COMMUNICATIONS_ALARM")
        enThree.setPerceivedSeverity("MAJOR")
        enThree.setProbableCause("m3100AlarmIndicationSignal")
        enThree.setSpecificProblem("GNBDU Service Unavailable")
        enList.add(enThree)

        return enList
    }

}
