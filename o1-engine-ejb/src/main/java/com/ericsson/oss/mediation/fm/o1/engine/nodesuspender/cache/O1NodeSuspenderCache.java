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

package com.ericsson.oss.mediation.fm.o1.engine.nodesuspender.cache;

import static com.ericsson.oss.mediation.o1.api.Constants.NODESUSPENDER_CACHE_NAME;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.cache.Cache;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.ericsson.oss.itpf.sdk.cache.classic.CacheProviderBean;
import com.ericsson.oss.mediation.fm.o1.engine.instrumentation.O1EngineStatistics;
import com.ericsson.oss.mediation.fm.o1.engine.nodesuspender.config.NodeSuspenderConfigurationListener;
import com.ericsson.oss.mediation.o1.api.NodeSuspensionEntry;

import lombok.extern.slf4j.Slf4j;

/**
 * Class responsible to initialize the node suspender cache.
 * This holds the details of a NE such as count of event notifications received in a period of time and node suspended status
 */
@ApplicationScoped
@Slf4j
public class O1NodeSuspenderCache {

    private Cache<String, NodeSuspensionEntry> nodeSuspenderCache;

    @Inject
    private CacheProviderBean cacheProviderBean;

    @Inject
    private NodeSuspenderConfigurationListener nodeSuspenderConfigurationListener;

    @Inject
    O1EngineStatistics o1EngineStatistics;

    @PostConstruct
    public synchronized void initializeCache() {
        try {
            this.nodeSuspenderCache = cacheProviderBean.createOrGetModeledCache(NODESUSPENDER_CACHE_NAME);
        } catch (Exception e) {
            log.error("Failed to initialize {} due to {}", NODESUSPENDER_CACHE_NAME, e);
        }
        log.info("{} created", NODESUSPENDER_CACHE_NAME);
    }

    public void updateNodeSuspenderCache(final String networkElementId) {
        getCache();
        final NodeSuspensionEntry entry = nodeSuspenderCache.get(networkElementId);
        entry.setCount(entry.getCount() + 1);
        if (entry.getCount() >= nodeSuspenderConfigurationListener.getAlarmRateThreshold()) {
            entry.setSuspended(true);
        }
        nodeSuspenderCache.put(networkElementId, entry);
        log.trace("Current count for the network element {} is {} ", networkElementId, nodeSuspenderCache.get(networkElementId));
    }

    public void addNodeToCache(final String networkElementId) {
        getCache();
        if (!nodeSuspenderCache.containsKey(networkElementId)) {
            o1EngineStatistics.incrementTotalNoOfSupervisedNodes();
        }
        final NodeSuspensionEntry entry = new NodeSuspensionEntry(0, false);
        nodeSuspenderCache.put(networkElementId, entry);
        log.debug("Added node {} to cache", networkElementId);
    }

    public void removeNodeFromCache(final String networkElementId) {
        getCache();
        if (nodeSuspenderCache.containsKey(networkElementId)) {
            nodeSuspenderCache.remove(networkElementId);
            log.debug("Removed node {} from cache", networkElementId);
            o1EngineStatistics.decrementTotalNoOfSupervisedNodes();
        }
    }

    public void resetCountForAllNodes() {
        for (Cache.Entry<String, NodeSuspensionEntry> entry : nodeSuspenderCache) {
            NodeSuspensionEntry valuePresent = entry.getValue();
            valuePresent.setCount(0);
            nodeSuspenderCache.put(entry.getKey(), valuePresent);
        }
        log.trace("Counter reset for all nodes in cache");
    }

    public Map<String, Integer> getSuspendedIdDetails() {
        Map<String, Integer> neAndValue = new HashMap<>();
        for (Cache.Entry<String, NodeSuspensionEntry> entry : nodeSuspenderCache) {
            NodeSuspensionEntry value = entry.getValue();
            if (value.isSuspended()) {
                neAndValue.put(entry.getKey(), value.getCount());
            }
        }
        return neAndValue;
    }

    public void resetNodeSuspensionStatus(List<String> networkElementIds) {
        log.debug("Resetting suspension status");
        for (String networkElementId : networkElementIds) {
            NodeSuspensionEntry value = nodeSuspenderCache.get(networkElementId);
            value.setSuspended(false);
            nodeSuspenderCache.put(networkElementId, value);
        }
    }

    public boolean isNodeSuspended(final String networkElementId) {
        getCache();
        log.trace("Node suspended status: {} for network element : {}", nodeSuspenderCache.get(networkElementId).isSuspended(),
                networkElementId);
        return nodeSuspenderCache.get(networkElementId).isSuspended();

    }

    private Cache<String, NodeSuspensionEntry> getCache() {
        if (nodeSuspenderCache == null) {
            log.warn(NODESUSPENDER_CACHE_NAME + " should have initialized post construct!");
            initializeCache();
        }
        return nodeSuspenderCache;
    }

}
