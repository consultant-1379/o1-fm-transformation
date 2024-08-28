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

import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.Constants.FDN;
import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.Constants.INCLUDE_NS_PREFIX;
import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.Constants.MANAGED_ELEMENT;
import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.Constants.MERGE;
import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.Constants.NAMESPACE;
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
import com.ericsson.oss.mediation.o1.yang.handlers.netconf.util.HeaderUtil;
import com.ericsson.oss.mediation.util.netconf.api.editconfig.Operation;
import lombok.extern.slf4j.Slf4j;

/**
 * A handler to update the 'ManagedElement.dnPrefix' attribute on the node with the MeContext FDN.
 */
@Slf4j
@EventHandler
public class SetDnPrefixHandler extends AbstractYangMoWriterHandler {

    @Override
    public boolean shouldExecuteOperation(final ComponentEvent inputEvent) throws MOHandlerException {
        return isActive();
    }

    @Override
    protected String getHandlerName() {
        return SetDnPrefixHandler.class.getSimpleName();
    }

    @Override
    protected void initialize(final Map<String, Object> allProperties) {
        operationType = Operation.MERGE;
        headerInfo = new HeaderInfo();
        headerInfo.setHeaders(allProperties);
        headerInfo.setNameSpace(NAMESPACE);
        headerInfo.setType(MANAGED_ELEMENT);
        headerInfo.setVersion(VERSION);
        headerInfo.setOperation(MERGE);
        headerInfo.setIncludeNsPrefix(HeaderUtil.readHeader(allProperties, INCLUDE_NS_PREFIX, Boolean.TRUE));
        final String meContextMo = getMeContextMo();
        final Map<String, Object> modifiedAttributes = new HashMap<>();
        modifiedAttributes.put("dnPrefix", meContextMo);
        headerInfo.setModifyAttributes(modifiedAttributes);
    }

    @Override
    protected void fillMoAttributes(final MO2EditConfigOperationConverter converter) {
        converter.setAttributes(getHeaderInfo().getModifyAttributes());
    }

    @Override
    protected void createFdnForCrudOperation(final String managedElementId) {
        headerInfo.setFdn(getManagedElementFdn(managedElementId));
    }

    private String getMeContextMo() {
        final String fmAlarmSupervisionFdn = (String) headerInfo.getHeaders().get(FDN);
        final String networkElementFdn = fmAlarmSupervisionFdn.replace(",FmAlarmSupervision=1", "");
        return dpsRead.getMeContextFdn(networkElementFdn);
    }

    private String getManagedElementFdn(final String managedElementId) {
        final String fmAlarmSupervisionFdn = (String) headerInfo.getHeaders().get(FDN);
        return FdnUtil.getManagedElementFdn(fmAlarmSupervisionFdn, managedElementId);
    }

    @Override
    protected Logger getLogger() {
        return log;
    }
}
