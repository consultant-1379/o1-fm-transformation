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

package com.ericsson.oss.mediation.o1.heartbeat.service;

import java.time.Duration;
import java.time.Instant;

import javax.annotation.PostConstruct;
import javax.cache.Cache.Entry;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.inject.Inject;

import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;
import com.ericsson.oss.mediation.fm.o1.engine.api.O1AlarmService;
import com.ericsson.oss.mediation.o1.heartbeat.api.HeartbeatDetails;

import lombok.extern.slf4j.Slf4j;

/**
 * During PostConstruct, this class creates an interval timer that is set to run after a predetermined amount of time. It then reads all the data from
 * O1HeartbeatCache and determines whether any heartbeat have expired for a given networkElemementFdn and raise a heartbeat alarm if it expired
 * .
 */
@Singleton
@Startup
@Slf4j
public class O1HeartbeatIntervalTimer {

    @Inject
    private O1HeartbeatCacheManager o1HeartbeatCacheManager;

    @EServiceRef
    private O1AlarmService alarmService;

    private static final int INTERVAL_DURATION_SECS = 20;

    private static final int INTERVAL_DURATION_MSECS = INTERVAL_DURATION_SECS * 1000;

    @Inject
    private TimerService timerService;

    @PostConstruct
    public void init() {
        log.info("Start timer for O1HeartBeat with interval {} ", INTERVAL_DURATION_SECS);
        timerService.createIntervalTimer(INTERVAL_DURATION_MSECS, INTERVAL_DURATION_MSECS, new TimerConfig("O1HeartBeat", false));
    }

    /**
     * method will be called whenever interval timer expires
     */
    @Timeout
    public void timeout(final Timer timer) {
        if (o1HeartbeatCacheManager.isInitialized()) {
            log.debug("O1HeartBeat failure check started");
            checkForHeartbeatFailures();
            log.debug("O1HeartBeat failure check is finished and will restart in {} seconds.", INTERVAL_DURATION_SECS);
        }
    }

    private void checkForHeartbeatFailures() {
        for (Entry<String, HeartbeatDetails> entry : o1HeartbeatCacheManager.getCache()) {
            HeartbeatDetails heartbeatDetails = entry.getValue();
            final String networkElementFdn = entry.getKey();
            if (!heartbeatDetails.isHeartbeatAlarmRaised()) {

                log.debug("Heartbeat timer details {} {}", networkElementFdn, heartbeatDetails);
                if (isHeartBeatExpired(heartbeatDetails)) {
                    setRaiseHeartbeatAlarm(heartbeatDetails, networkElementFdn);
                    alarmService.raiseHeartbeatAlarm(networkElementFdn);
                }

            }
        }
    }

    private void setRaiseHeartbeatAlarm(HeartbeatDetails heartbeatDetails, final String networkElementFdn) {
        log.debug("HeartBeat timer expired  for {} ", networkElementFdn);
        heartbeatDetails.setHeartbeatAlarmRaised(true);
        o1HeartbeatCacheManager.putEntry(networkElementFdn, heartbeatDetails);
        log.debug("isHeartbeatAlarmRaised value is set to true for {} ", networkElementFdn);
    }

    private boolean isHeartBeatExpired(final HeartbeatDetails heartbeatDetails) {
        final Instant cacheTimeStamp = Instant.ofEpochSecond(heartbeatDetails.getLastHeartbeatRecievedEpochTimestamp());
        final Instant currentTimeStamp = Instant.now();
        final Duration timeElapsed = Duration.between(cacheTimeStamp, currentTimeStamp);
        return timeElapsed.getSeconds() > heartbeatDetails.getHeartbeatTimeout();
    }

}
