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

package com.ericsson.oss.mediation.fm.o1.engine.transform;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.commons.collections4.MapUtils;

import com.ericsson.oss.mediation.fm.o1.engine.service.config.MediationConfigurationBean;
import com.ericsson.oss.mediation.fm.o1.engine.service.config.NetworkElementConfiguration;
import com.ericsson.oss.mediation.fm.o1.engine.service.config.NetworkElementId;
import com.ericsson.oss.mediation.fm.o1.common.util.FdnUtil;
import com.ericsson.oss.mediation.translator.model.EventNotification;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class TransformManager {

    private static final String TYPE = "O1";
    private static final String VERSION = "1.0.0";
    private final Map<NetworkElementId, ModelTransformerKey> mapperRegistry = new ConcurrentHashMap<>();

    @Inject
    private ModelTransformer modelTransformer;

    @Inject
    private MediationConfigurationBean mediationConfigurationBean;

    public void initialize() {
        final List<NetworkElementConfiguration> networkElementConfigurations = mediationConfigurationBean.getConfigurations();
        addTransformerConfigurations(networkElementConfigurations);
    }

    public EventNotification transformAlarm(final Map<String, Object> alarm) {
        if (MapUtils.isEmpty(alarm)) {
            throw new TransformerException("Received request to transform alarm with no properties.");
        }

        return modelTransformer.transformAlarm(alarm, findNodeMapper());
    }

    public List<EventNotification> transformAlarms(final List<Map<String, Object>> alarms, final String meContextFdn) {
        if (alarms == null || alarms.isEmpty()) {
            throw new TransformerException("Received request to transform null or empty alarms list.");
        }

        final List<EventNotification> eventNotifications = modelTransformer.transformAlarms(alarms, findNodeMapper());

        for (final EventNotification notification : eventNotifications) {
            prependMeContextFdnToManagedObjectInstance(notification, meContextFdn);
            addNetworkElementFdnToAdditionalAttrs(notification);
        }
        return eventNotifications;
    }

    private void addTransformerConfigurations(final List<NetworkElementConfiguration> networkElementConfigurations) {
        for (final NetworkElementConfiguration networkElementConfiguration : networkElementConfigurations) {
            final ModelTransformerKey transformerKey = modelTransformer.addModelTransformerConfig(networkElementConfiguration.getTransformerFile());
            mapperRegistry.put(networkElementConfiguration.getNetworkElementId(), transformerKey);
            log.info("Added Model Transformer {} for {} transformer key: {} ", networkElementConfiguration.getTransformerFile(),
                    networkElementConfiguration.getNetworkElementId(), transformerKey.toString());
        }
    }

    private void prependMeContextFdnToManagedObjectInstance(final EventNotification eventNotification, final String MeContextFdn) {
        eventNotification.setManagedObjectInstance(MeContextFdn + eventNotification.getManagedObjectInstance());
        log.debug("Prepended [{}] to managedObjectInstance in EventNotification", MeContextFdn);
    }

    private void addNetworkElementFdnToAdditionalAttrs(final EventNotification eventNotification) {
        final String networkElementFdn = FdnUtil.getNetworkElementFdn(eventNotification.getManagedObjectInstance());
        eventNotification.addAdditionalAttribute("fdn", networkElementFdn);
        log.debug("Included network element fdn [{}] in EventNotification", networkElementFdn);
    }

    private ModelTransformerKey findNodeMapper() {
        // Hardcoding the node info for now
        final ModelTransformerKey modelTransformerKey = mapperRegistry.get(new NetworkElementId(TYPE, VERSION));
        if (modelTransformerKey == null) {
            throw new TransformerException("ModelTransformerKey not found for type " + TYPE + " and version " + VERSION);
        }
        return modelTransformerKey;
    }
}
