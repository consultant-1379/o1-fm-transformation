package com.ericsson.oss.mediation.o1.heartbeat.service.test

import java.time.Instant
import java.time.temporal.ChronoUnit

import javax.ejb.Timer
import javax.inject.Inject

import com.ericsson.cds.cdi.support.providers.stubs.InMemoryCache
import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.itpf.sdk.cache.classic.CacheProviderBean
import com.ericsson.oss.mediation.o1.heartbeat.api.HeartbeatDetails
import com.ericsson.oss.mediation.o1.heartbeat.service.O1HeartbeatCacheManager
import com.ericsson.oss.mediation.o1.heartbeat.service.O1HeartbeatIntervalTimer

class O1HeartBeatIntervalTimerSpec extends CdiSpecification {

    @ObjectUnderTest
    private O1HeartbeatIntervalTimer o1HeartBeatIntervalTimer

    @Inject
    private O1HeartbeatCacheManager o1HeartbeatCacheManager

    @MockedImplementation
    private CacheProviderBean cacheProviderBean;

    static private final String NETWORK_ELEMENT_FDN = "NetworkElement=ocp83vcu03o1"

    static private final long heartbeatTimeout = 300


    private HeartbeatDetails heartbeatDetails;

    def Timer mockTimer = Mock(Timer.class)

    def o1HeartbeatCache = new InMemoryCache<String, HeartbeatDetails>('O1HeartbeatCache')

    def setup() {
        cacheProviderBean.createOrGetModeledCache('O1HeartbeatCache') >> o1HeartbeatCache
        createHeartbeatDetails()
    }

    def "Test when interval timer begins and heartbeat timer is active for network element fdn"() {

        given: "a non empty cache"
            o1HeartbeatCacheManager.putEntry(NETWORK_ELEMENT_FDN, heartbeatDetails);

        when: "The timer interval expires"
            o1HeartBeatIntervalTimer.timeout(mockTimer)

        then: "verify heartbeat timer is active"
           o1HeartbeatCache.get(NETWORK_ELEMENT_FDN).isHeartbeatAlarmRaised() == false
           0 * o1HeartBeatIntervalTimer.alarmService.raiseHeartbeatAlarm(NETWORK_ELEMENT_FDN)

    }

    def "Test when interval timer begins and heartbeat timer expires for network element fdn"() {

        given: "a non empty cache"
             o1HeartbeatCache

        and: "the timestamp is greater than interval"
            heartbeatDetails.setLastHeartbeatRecievedEpochTimestamp(Instant.now().minus(6, ChronoUnit.MINUTES).getEpochSecond())
            o1HeartbeatCacheManager.putEntry(NETWORK_ELEMENT_FDN, heartbeatDetails);

        when: "The timer interval expires"
            o1HeartBeatIntervalTimer.timeout(mockTimer)

        then: "verify heartbeat alarm is raised for networkElementFdn"
           o1HeartbeatCache.get(NETWORK_ELEMENT_FDN).isHeartbeatAlarmRaised() == true
           1 * o1HeartBeatIntervalTimer.alarmService.raiseHeartbeatAlarm(NETWORK_ELEMENT_FDN)
    }

    private void createHeartbeatDetails() {
        heartbeatDetails = new HeartbeatDetails()
        heartbeatDetails.setHeartbeatTimeout(heartbeatTimeout)
        heartbeatDetails.setLastHeartbeatRecievedEpochTimestamp(Instant.now().getEpochSecond())
    }
}
