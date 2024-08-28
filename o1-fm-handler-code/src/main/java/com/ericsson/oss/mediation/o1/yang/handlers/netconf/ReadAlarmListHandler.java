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

import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.Constants.ALARM_LIST;
import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.Constants.ALARM_RECORDS;
import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.Constants.EVENT_ID_FM_SUPERVISION_MISSING_DATA;
import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.Constants.FDN;
import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.Constants.INCLUDE_NS_PREFIX;
import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.Constants.MSG_ALARM_LIST_TYPE_MISSING;
import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.Constants.MSG_ALARM_RECORDS_CANNOT_BE_READ;
import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.Constants.NAMESPACE;
import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.Constants.READ;
import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.Constants.VERSION;

import java.util.Collections;
import java.util.Map;

import org.slf4j.Logger;

import com.ericsson.oss.itpf.common.event.ComponentEvent;
import com.ericsson.oss.itpf.common.event.handler.annotation.EventHandler;
import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;
import com.ericsson.oss.mediation.flow.events.MediationComponentEvent;
import com.ericsson.oss.mediation.fm.o1.engine.api.O1AlarmService;
import com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.HeaderInfo;
import com.ericsson.oss.mediation.o1.yang.handlers.netconf.exception.MOHandlerException;
import com.ericsson.oss.mediation.o1.yang.handlers.netconf.util.FdnUtil;
import com.ericsson.oss.mediation.o1.yang.handlers.netconf.util.HeaderUtil;
import com.ericsson.oss.mediation.translator.model.EventNotification;

import lombok.extern.slf4j.Slf4j;

/**
 * Handler responsible for reading the existing alarms from an O1Node type.
 * <p>
 * Alarms are read from the AlarmList.alarmRecords attribute.
 */
@Slf4j
@EventHandler
public class ReadAlarmListHandler extends AbstractYangMoReadHandler {

    @EServiceRef
    private O1AlarmService alarmService;

    @Override
    protected void initialize(final Map<String, Object> contextProperties) {
        headerInfo = new HeaderInfo();
        headerInfo.setOperation(READ);
        headerInfo.setHeaders(contextProperties);
        headerInfo.setNameSpace(NAMESPACE);
        headerInfo.setType(ALARM_LIST);
        headerInfo.setName("1");
        headerInfo.setVersion(VERSION);
        headerInfo.setReadAttributes(Collections.singletonList(ALARM_RECORDS));
        headerInfo.setIncludeNsPrefix(HeaderUtil.readHeader(contextProperties, INCLUDE_NS_PREFIX, Boolean.TRUE));
    }

    @Override
    protected String getHandlerName() {
        return ReadAlarmListHandler.class.getName();
    }

    @Override
    protected ComponentEvent postOperationExecute(final ComponentEvent inputEvent) {

        if (isFailedOperation(inputEvent)) {
            readSystemRecorder.recordError(EVENT_ID_FM_SUPERVISION_MISSING_DATA, getHandlerName(),
                    headerInfo.getFdn(), MSG_ALARM_LIST_TYPE_MISSING);
            alarmService.sendAlarm(createSyncAbortedAlarm(headerInfo.getFdn()));
            return new MediationComponentEvent(inputEvent.getHeaders(), Collections.emptyList());
        }

        final Object response = getResult(inputEvent);
        log.debug("Response received from the node {}", response);

        if (response instanceof Map && !((Map) response).isEmpty()) {
            final Map<String, ?> returned = (Map<String, ?>) response;
            final Object alarmRecords = returned.get(ALARM_RECORDS);
            inputEvent.getHeaders().put("fdn", headerInfo.getFdn());

            if (alarmRecords == null) {
                readSystemRecorder.recordError(EVENT_ID_FM_SUPERVISION_MISSING_DATA, getHandlerName(),
                        headerInfo.getFdn(), MSG_ALARM_RECORDS_CANNOT_BE_READ);
                alarmService.sendAlarm(createSyncAbortedAlarm(headerInfo.getFdn()));
            }
            return alarmRecords == null
                    ? new MediationComponentEvent(inputEvent.getHeaders(), Collections.emptyList())
                    : new MediationComponentEvent(inputEvent.getHeaders(), alarmRecords);
        }
        return new MediationComponentEvent(inputEvent.getHeaders(), Collections.emptyList());
    }

    @Override
    protected boolean shouldExecuteOperation(final ComponentEvent inputEvent) throws MOHandlerException {
        return isActive();
    }

    @Override
    protected void createFdnForCrudOperation(String managedElementId) {
        final String fmAlarmSupervisionFdn = (String) headerInfo.getHeaders().get(FDN);
        headerInfo.setFdn(FdnUtil.getAlarmListFdn(fmAlarmSupervisionFdn, managedElementId));
    }

    @Override
    protected Logger getLogger() {
        return log;
    }

    private EventNotification createSyncAbortedAlarm(final String fdn) {
        EventNotification eventNotification = com.ericsson.oss.mediation.fm.util.EventNotificationUtil
                .createSyncAlarm(fdn, "", "", "SYNCHRONIZATION_ABORTED");
        eventNotification.addAdditionalAttribute("fdn", FdnUtil.getNetworkElementFdn(fdn));
        log.debug("Created EventNotification: {}", eventNotification);
        return eventNotification;
    }
}
