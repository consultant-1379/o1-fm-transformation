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

import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.Constants.ADMINISTRATIVE_STATE;
import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.Constants.ALARM_LIST;
import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.Constants.FDN;
import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.Constants.MERGE;
import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.Constants.NAMESPACE;
import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.Constants.UNLOCKED;
import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.Constants.VERSION;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;

import com.ericsson.oss.itpf.common.event.ComponentEvent;
import com.ericsson.oss.itpf.common.event.handler.annotation.EventHandler;
import com.ericsson.oss.mediation.adapter.netconf.jca.xa.yang.provider.MO2EditConfigOperationConverter;
import com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.HeaderInfo;
import com.ericsson.oss.mediation.o1.yang.handlers.netconf.exception.MOHandlerException;
import com.ericsson.oss.mediation.o1.yang.handlers.netconf.util.FdnUtil;
import com.ericsson.oss.mediation.util.netconf.api.editconfig.Operation;
import lombok.extern.slf4j.Slf4j;

/**
 * Handler responsible for updating the AlarmList.administrativeState attribute to UNLOCKED.
 */
@Slf4j
@EventHandler
public class UnlockAlarmListHandler extends AbstractYangMoWriterHandler {

    @Override
    public boolean shouldExecuteOperation(final ComponentEvent inputEvent) throws MOHandlerException {
        return isActive();
    }

    @Override
    protected String getHandlerName() {
        return UnlockAlarmListHandler.class.getSimpleName();
    }

    @Override
    protected void initialize(final Map<String, Object> allProperties) {
        headerInfo = new HeaderInfo();
        operationType = Operation.MERGE;
        headerInfo.setHeaders(allProperties);
        headerInfo.setNameSpace(NAMESPACE);
        headerInfo.setType(ALARM_LIST);
        headerInfo.setName("1");

        final Map<String, Object> modifiedAttributes = new HashMap<>();
        modifiedAttributes.put(ADMINISTRATIVE_STATE, UNLOCKED);

        headerInfo.setVersion(VERSION);
        headerInfo.setOperation(MERGE);
        headerInfo.setIncludeNsPrefix(true);
        headerInfo.setModifyAttributes(modifiedAttributes);
    }

    @Override
    protected void fillMoAttributes(final MO2EditConfigOperationConverter converter) {
        converter.setAttributes(getHeaderInfo().getModifyAttributes());
    }

    @Override
    protected void createFdnForCrudOperation(final String managedElementId) {
        final String fmAlarmSupervisionFdn = (String) headerInfo.getHeaders().get(FDN);
        headerInfo.setFdn(FdnUtil.getAlarmListFdn(fmAlarmSupervisionFdn, managedElementId));
    }

    @Override
    protected Logger getLogger() {
        return log;
    }
}
