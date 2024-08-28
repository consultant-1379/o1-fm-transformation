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
package com.ericsson.oss.mediation.o1.heartbeat.service.test

import com.ericsson.cds.cdi.support.providers.stubs.InMemoryCache
import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.itpf.sdk.cache.classic.CacheProviderBean
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder
import com.ericsson.oss.mediation.o1.heartbeat.api.HeartbeatDetails
import com.ericsson.oss.mediation.o1.heartbeat.service.O1HeartbeatCacheManager
import com.ericsson.oss.mediation.o1.heartbeat.service.O1HeartbeatServiceImpl

import javax.inject.Inject
import java.time.Instant

import static com.ericsson.oss.itpf.sdk.recording.ErrorSeverity.ERROR
import static org.junit.Assert.assertTrue

class O1HeartBeatServiceImplSpec extends CdiSpecification {

    static private final long heartbeatTimeout = 300
    static private final String NETWORK_ELEMENT_FDN = "NetworkElement=ocp83vcu03o1"


    @ObjectUnderTest
    private O1HeartbeatServiceImpl o1HeartBeatServiceImpl

    @Inject
    private O1HeartbeatCacheManager o1HeartbeatCacheManager

    @MockedImplementation
    private CacheProviderBean cacheProviderBean

    @Inject
    private SystemRecorder systemRecorder

    private HeartbeatDetails heartbeatDetails

    def o1HeartbeatCache = new InMemoryCache<String, HeartbeatDetails>('O1HeartbeatCache')

    def setup() {
        cacheProviderBean.createOrGetModeledCache('O1HeartbeatCache') >> o1HeartbeatCache
        createHeartbeatDetails()
    }

    def "Test add entry in cache"() {

        given: "a empty cache"
            assert o1HeartbeatCache.size() == 0

        when: "The request to add a entry in cache is received"
            o1HeartBeatServiceImpl.putEntryInHeartbeatCache(NETWORK_ELEMENT_FDN, heartbeatDetails)

        then: "entry added in cache successfully"
            assert o1HeartbeatCache.size() == 1
    }

    def "Test update entry in cache"() {

        given: "a non empty cache"
            addEntryInCache()
            assert o1HeartbeatCache.size() == 1
            heartbeatDetails.setHeartbeatAlarmRaised(true)

        when: "The request to update the cache is received"
            o1HeartBeatServiceImpl.putEntryInHeartbeatCache(NETWORK_ELEMENT_FDN, heartbeatDetails)

        then: "cache updated successfully"
            assertTrue(o1HeartbeatCache.get(NETWORK_ELEMENT_FDN).isHeartbeatAlarmRaised())
            assert o1HeartbeatCache.size() == 1

    }

    def "Test get entry from cache"() {

        given: "a non empty cache"
            addEntryInCache()
            assert o1HeartbeatCache.size() > 0

        when: "The request to get an entry from cache is received"
            HeartbeatDetails details = o1HeartBeatServiceImpl.getEntryFromHeartbeatCache(NETWORK_ELEMENT_FDN)

        then: "entry retrieved from cache successfully"
            assert o1HeartbeatCache.get(NETWORK_ELEMENT_FDN) == details
    }

    def "Test remove entry from cache"() {

        given: "a non empty cache"
            addEntryInCache()
            assert o1HeartbeatCache.size() > 0

        when: "The request to remove an entry from cache is received"
            o1HeartBeatServiceImpl.removeEntryFromHeartbeatCache(NETWORK_ELEMENT_FDN)

        then: "entry removed from cache successfully"
            assert o1HeartbeatCache.size() == 0
    }

    def "Test when exception occurs in cache"() {

        when: "initializing a cache"
            o1HeartbeatCacheManager.getCache()

        then: "exception occurred during initialization of cache"
            cacheProviderBean.createOrGetModeledCache('O1HeartbeatCache') >> {
                throw new RuntimeException()
            }

        and: "exception is recorded using system recorder"
            1 * systemRecorder.recordError("O1_HEARTBEAT_SERVICE", ERROR, "", "", "O1HeartbeatCache initialization failed");

    }

    private void createHeartbeatDetails() {
        heartbeatDetails = new HeartbeatDetails()
        heartbeatDetails.setHeartbeatTimeout(heartbeatTimeout)
        heartbeatDetails.setLastHeartbeatRecievedEpochTimestamp(Instant.now().getEpochSecond())
    }

    private void addEntryInCache() {
        o1HeartBeatServiceImpl.putEntryInHeartbeatCache(NETWORK_ELEMENT_FDN, heartbeatDetails)
    }
}
