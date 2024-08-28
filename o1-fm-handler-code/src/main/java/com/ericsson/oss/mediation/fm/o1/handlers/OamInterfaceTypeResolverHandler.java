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

package com.ericsson.oss.mediation.fm.o1.handlers;

import static com.ericsson.oss.mediation.fm.o1.handlers.util.HandlerMessages.SUPERVISION_STATUS_OR_OAM_INTERFACE_FOR_FM_NOT_FOUND;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

import com.ericsson.oss.itpf.common.event.handler.annotation.EventHandler;
import com.ericsson.oss.itpf.common.event.handler.exception.EventHandlerException;
import com.ericsson.oss.mediation.flow.events.MediationComponentEvent;
import com.ericsson.oss.mediation.fm.o1.common.util.FdnUtil;
import com.ericsson.oss.mediation.fm.o1.handlers.util.Attributes;

import lombok.extern.slf4j.Slf4j;

/**
 * This handler performs the FM Supervision Flow Choice Selector (EOI/O1) <br>
 * It is activated by SupervisionMediationTaskRequest Modeled event on an object of type FmAlarmSupervision.<br>
 * <br>
 * Input of the handler are header and payLoad<br>
 * The header must contain the following keys:
 * <li>fdn: the FDN of the FmAlarmSupervision MO
 * <li>oamInterfaceForFM: the oamInterfaceForFM of the FmAlarmSupervision MO
 * <li>active: the supervision status (true/false)
 * <br>
 * When the event is received, the oamInterfaceForFM & active attributes are shared by the handlers header in order to enable flow choice.<br>
 */
@Slf4j
@EventHandler
public class OamInterfaceTypeResolverHandler extends AbstractFmMediationHandler {

    private static final String HANDLER = OamInterfaceTypeResolverHandler.class.getSimpleName();

    /**
     * @param inputEvent
     *            the parameter, received by mediation framework, is the event triggering the present handler
     * @return the return code is for passing the event through next handlers
     */

    @Override
    public Object onEventWithResult(final Object inputEvent) {

        super.onEventWithResult(inputEvent);

        final Object activeValue = getSupervisionHeaderOrSuperHeader(Attributes.ACTIVE);
        final Object oamInterfaceForFmValue = getHeader(Attributes.OAM_INTERFACE_FOR_FM);

        if (activeValue == null || oamInterfaceForFmValue == null) {
            getRecorder().recordError(HANDLER, getHeaderFdn(), FdnUtil.getParentFdn(getHeaderFdn()),
                    SUPERVISION_STATUS_OR_OAM_INTERFACE_FOR_FM_NOT_FOUND);
            throw new EventHandlerException(SUPERVISION_STATUS_OR_OAM_INTERFACE_FOR_FM_NOT_FOUND);
        }

        return new MediationComponentEvent(headers, StringUtils.EMPTY);
    }

    @Override
    protected Logger getLogger() {
        return log;
    }
}
