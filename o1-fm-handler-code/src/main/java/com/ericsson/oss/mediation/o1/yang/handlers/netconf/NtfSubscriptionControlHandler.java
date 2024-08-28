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

package com.ericsson.oss.mediation.o1.yang.handlers.netconf;

import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.Constants.CREATE;
import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.Constants.DELETE;
import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.Constants.EVENT_LISTENER_PORT;
import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.Constants.EVENT_LISTENER_REST_URL;
import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.Constants.FDN;
import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.Constants.ID;
import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.Constants.NAMESPACE;
import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.Constants.NOTIFICATION_RECIPIENT_ADDRESS;
import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.Constants.NOTIFICATION_TYPES;
import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.Constants.NOTIFICATION_TYPES_SUPPORTED;
import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.Constants.NTF_SUBSCRIPTION_CONTROL;
import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.Constants.NTF_SUBSCRIPTION_CONTROL_ID;
import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.Constants.VERSION;
import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.Constants.VIP;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;

import com.ericsson.oss.itpf.common.event.handler.annotation.EventHandler;
import com.ericsson.oss.mediation.adapter.netconf.jca.xa.yang.provider.MO2EditConfigOperationConverter;
import com.ericsson.oss.mediation.netconf.session.api.operation.NetconfSessionOperation;
import com.ericsson.oss.mediation.netconf.session.api.operation.NetconfSessionOperationsStatus;
import com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.HeaderInfo;
import com.ericsson.oss.mediation.o1.yang.handlers.netconf.exception.MOHandlerException;
import com.ericsson.oss.mediation.o1.yang.handlers.netconf.util.FdnUtil;
import com.ericsson.oss.mediation.o1.yang.handlers.netconf.util.GlobalPropUtils;
import com.ericsson.oss.mediation.o1.yang.handlers.netconf.util.O1NetconfSessionOperationStatus;
import com.ericsson.oss.mediation.util.netconf.api.editconfig.Operation;
import lombok.extern.slf4j.Slf4j;

/**
 * Creates the 'NtfSubscriptionControl' MO on the node. This MO is required to subscribe to FM notifications.
 * Note the creation of the MO is skipped if it finds that the HeartbeatControl MO has already been created - this is
 * done to support the idempotent use case.
 * <p>
 * The 'NtfSubscriptionControl.notificationRecipientAddress' attribute is set to the URL of the VES/SDO collector component in ENM that listens for
 * alarm notifications from the node.
 * <p>
 * The ipaddress used in the URL is determined from a global property, the name of which is provided as a header called 'VIP'. On pENM this will be
 * set to the haproxy_sb ipaddress while on cENM this will be set to the FM vip.
 */
@Slf4j
@EventHandler
public class NtfSubscriptionControlHandler extends AbstractYangMoWriterHandler {

    @Override
    protected boolean isToBeSkipped(NetconfSessionOperationsStatus operationsStatus) {
        final O1NetconfSessionOperationStatus opStatus = (O1NetconfSessionOperationStatus) operationsStatus.getOperationStatus(NetconfSessionOperation.GET);
        if (opStatus.heartbeatControlMoAlreadyExists()) {
            log.info("Found that {} is already created, skipping creation.", NTF_SUBSCRIPTION_CONTROL);
            return true;
        }
        return false;
    }

    @Override
    public void initialize(final Map<String, Object> allProperties) {
        headerInfo = new HeaderInfo();
        headerInfo.setHeaders(allProperties);
        headerInfo.setNameSpace(NAMESPACE);
        headerInfo.setType(NTF_SUBSCRIPTION_CONTROL);
        headerInfo.setVersion(VERSION);
        headerInfo.setIncludeNsPrefix(true);
        setOperationTypeAttributes();
    }

    @Override
    protected String getHandlerName() {
        return NtfSubscriptionControlHandler.class.getSimpleName();
    }

    @Override
    protected void fillMoAttributes(final MO2EditConfigOperationConverter converter) {
        if (isActive()) {
            converter.setAttributes(getHeaderInfo().getCreateAttributes());
        }
    }

    @Override
    protected void createFdnForCrudOperation(final String managedElementId) {
        headerInfo.setFdn(getNtfSubscriptionControlFdn(managedElementId));
    }

    private String getNtfSubscriptionControlFdn(final String managedElementId) {
        final String fmAlarmSupervisionFdn = (String) headerInfo.getHeaders().get(FDN);
        return FdnUtil.getNtfSubscriptionControlFdn(fmAlarmSupervisionFdn, managedElementId);
    }

    private void setOperationTypeAttributes() {
        if (isActive()) {
            operationType = Operation.CREATE;
            headerInfo.setOperation(CREATE);
            setCreateAttributes();
        } else {
            operationType = Operation.DELETE;
            headerInfo.setOperation(DELETE);
        }
    }

    private void setCreateAttributes() {
        final Map<String, Object> createAttributes = new HashMap<>();
        createAttributes.put(ID, NTF_SUBSCRIPTION_CONTROL_ID);
        createAttributes.put(NOTIFICATION_TYPES, NOTIFICATION_TYPES_SUPPORTED);
        createAttributes.put(NOTIFICATION_RECIPIENT_ADDRESS, getNotificationRecipientUrl());
        headerInfo.setCreateAttributes(createAttributes);
    }

    private String getNotificationRecipientUrl() {
        try {
            final URL url = new URL("http", getVesCollectorIp(), EVENT_LISTENER_PORT, EVENT_LISTENER_REST_URL);
            return url.toString();
        } catch (final MalformedURLException ex) {
            throw new MOHandlerException("Failed to build URL {}", ex);
        }
    }

    private String getVesCollectorIp() {
        final String vesCollectorIpPropertyName = (String) headerInfo.getHeaders().get(VIP);
        if (vesCollectorIpPropertyName == null || vesCollectorIpPropertyName.isEmpty()) {
            throw new MOHandlerException("Cannot determine VES collector ipaddress as 'VIP' header has not been set");
        }

        return GlobalPropUtils.getGlobalValue(vesCollectorIpPropertyName, String.class);
    }

    @Override
    protected Logger getLogger() {
        return log;
    }
}
