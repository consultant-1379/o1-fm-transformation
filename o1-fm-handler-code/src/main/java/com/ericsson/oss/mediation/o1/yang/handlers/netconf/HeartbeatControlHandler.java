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
import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.Constants.FDN;
import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.Constants.HEARTBEAT_CONTROL;
import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.Constants.HEARTBEAT_INTERVAL;
import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.Constants.HEARTBEAT_NTF_PERIOD;
import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.Constants.ID;
import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.Constants.NAMESPACE;
import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.Constants.VERSION;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;

import com.ericsson.oss.itpf.common.event.ComponentEvent;
import com.ericsson.oss.itpf.common.event.handler.annotation.EventHandler;
import com.ericsson.oss.mediation.adapter.netconf.jca.xa.yang.provider.MO2EditConfigOperationConverter;
import com.ericsson.oss.mediation.netconf.session.api.operation.NetconfSessionOperation;
import com.ericsson.oss.mediation.netconf.session.api.operation.NetconfSessionOperationsStatus;
import com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.HeaderInfo;
import com.ericsson.oss.mediation.o1.yang.handlers.netconf.util.FdnUtil;
import com.ericsson.oss.mediation.o1.yang.handlers.netconf.util.O1NetconfSessionOperationStatus;
import com.ericsson.oss.mediation.util.netconf.api.editconfig.Operation;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * Creates HeartbeatControl MO on the node. It is child class of NtfSubscriptionControl MO.
 * Note the creation of the MO is skipped if it finds that the HeartbeatControl MO has already been created - this is
 * done to support the idempotent use case.
 * </p><p>
 * The value HeartbeatControl.heartbeatNtfPeriod is set with the value of FmFunction.heartbeatinterval
 * </p>
 */
@Slf4j
@EventHandler
public class HeartbeatControlHandler extends AbstractYangMoWriterHandler {

    @Override
    protected boolean isToBeSkipped(NetconfSessionOperationsStatus operationsStatus) {
        final O1NetconfSessionOperationStatus opStatus = (O1NetconfSessionOperationStatus) operationsStatus.getOperationStatus(NetconfSessionOperation.GET);
        if (opStatus.heartbeatControlMoAlreadyExists()) {
            log.info("Found that {} is already created, skipping creation.", HEARTBEAT_CONTROL);
            return true;
        }
        return false;
    }

    @Override
    public void initialize(final Map<String, Object> allProperties) {
        headerInfo = new HeaderInfo();
        operationType = Operation.CREATE;
        headerInfo.setHeaders(allProperties);
        headerInfo.setNameSpace(NAMESPACE);
        headerInfo.setType(HEARTBEAT_CONTROL);
        headerInfo.setVersion(VERSION);
        headerInfo.setOperation(CREATE);
        headerInfo.setIncludeNsPrefix(true);
        headerInfo.setCreateAttributes(getHeartbeatControlMoAttributes());
    }

    @Override
    protected String getHandlerName() {
        return HeartbeatControlHandler.class.getSimpleName();
    }

    @Override
    protected boolean shouldExecuteOperation(final ComponentEvent inputEvent) {
        return isActive();
    }

    @Override
    protected void fillMoAttributes(final MO2EditConfigOperationConverter converter) {
        converter.setAttributes(getHeaderInfo().getCreateAttributes());
    }

    @Override
    protected void createFdnForCrudOperation(String managedElementId) {
        headerInfo.setFdn(getHeartbeatControlFdn(managedElementId));
    }

    private String getHeartbeatControlFdn(final String managedElementId) {
        final String fmAlarmSupervisionFdn = (String) headerInfo.getHeaders().get(FDN);
        return FdnUtil.getHeartbeatControlFdn(fmAlarmSupervisionFdn, managedElementId);
    }

    private Map<String, Object> getHeartbeatControlMoAttributes() {
        final int heartbeatInterval = (Integer) headerInfo.getHeaders().get(HEARTBEAT_INTERVAL);
        final Map<String, Object> attributes = new HashMap<>();
        attributes.put(ID, "1");
        attributes.put(HEARTBEAT_NTF_PERIOD, heartbeatInterval);
        return attributes;
    }

    @Override
    protected Logger getLogger() {
        return log;
    }

}
