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

import static com.ericsson.oss.itpf.sdk.recording.ErrorSeverity.ERROR;
import static com.ericsson.oss.mediation.o1.heartbeat.api.HeartbeatConstants.CACHE_NAME;

import javax.cache.Cache;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.ericsson.oss.itpf.sdk.cache.classic.CacheProviderBean;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.mediation.o1.heartbeat.api.HeartbeatDetails;

import lombok.extern.slf4j.Slf4j;

/**
 * The class that communicates with the cache and will be initialized using lazy initialization, which will be advantageous in the event that a cache
 * problem arises so that the application can continue to function without generating a deployment issue.
 */

@ApplicationScoped
@Slf4j
public class O1HeartbeatCacheManager {

    private Cache<String, HeartbeatDetails> cache = null;

    @Inject
    protected SystemRecorder systemRecorder;

    @Inject
    private CacheProviderBean bean;

    public Cache<String, HeartbeatDetails> getCache() {
        try {
            if (this.cache == null) {
                this.cache = bean.createOrGetModeledCache(CACHE_NAME);
            }
            return cache;
        } catch (final Exception e) {
            log.error("Get cache exception " + CACHE_NAME, e);
            systemRecorder.recordError("O1_HEARTBEAT_SERVICE", ERROR, "", "", CACHE_NAME + " initialization failed");
            return null;
        }
    }

    public boolean isInitialized() {
        return getCache() != null;
    }

    public HeartbeatDetails getEntry(final String key) {
        log.debug("Get entry from " + CACHE_NAME + " for [{}]", key);
        return (isInitialized()) ? getCache().get(key) : null;
    }

    public void removeEntry(final String key) {
        if (isInitialized()) {
            log.debug("Remove entry from " + CACHE_NAME + " for [{}]", key);
            getCache().getAndRemove(key);
        }
    }

    public void putEntry(final String key, final HeartbeatDetails value) {
        if (isInitialized()) {
            log.debug("put entry for [{}] with [{}] in " + CACHE_NAME, key, value);
            getCache().put(key, value);
        }
    }

}
