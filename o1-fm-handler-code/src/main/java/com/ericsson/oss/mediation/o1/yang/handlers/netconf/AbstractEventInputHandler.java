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

import static com.ericsson.oss.mediation.fm.o1.common.Constants.ACTIVE;
import static com.ericsson.oss.mediation.fm.o1.common.Constants.CLIENT_TYPE;
import static com.ericsson.oss.mediation.fm.o1.common.Constants.EVENT_BASED;
import static com.ericsson.oss.mediation.fm.o1.common.util.EventFormatter.format;

import java.util.HashMap;
import java.util.Map;

import com.ericsson.oss.mediation.fm.o1.common.util.EventFormatter;
import org.slf4j.Logger;

import com.ericsson.oss.itpf.common.event.ComponentEvent;
import com.ericsson.oss.itpf.common.event.handler.EventHandlerContext;
import com.ericsson.oss.itpf.common.event.handler.ResultEventInputHandler;
import com.ericsson.oss.itpf.common.event.handler.exception.EventHandlerException;
import com.ericsson.oss.mediation.flow.events.MediationComponentEvent;

@SuppressWarnings("deprecation")
public abstract class AbstractEventInputHandler implements ResultEventInputHandler {

    protected Map<String, Object> headers = new HashMap<>();
    protected Object payload = null;

    protected abstract Logger getLogger();

    @Override
    public ComponentEvent onEventWithResult(final Object inputEvent) {
        if (inputEvent == null) {
            throw new EventHandlerException("Internal error - input event received is null");
        }
        if (inputEvent instanceof MediationComponentEvent) {
            final MediationComponentEvent mediationComponentEvent = (MediationComponentEvent) inputEvent;

            headers.putAll(mediationComponentEvent.getHeaders());
            payload = mediationComponentEvent.getPayload();

            if (getLogger().isTraceEnabled()) {
                getLogger().trace("with headers: {}", EventFormatter.format(headers));
                getLogger().trace("with payload: {}", EventFormatter.format(payload));
            }
        }
        return new MediationComponentEvent(headers, payload);
    }

    @Override
    public void init(EventHandlerContext eventHandlerContext) {
        headers = eventHandlerContext.getEventHandlerConfiguration().getAllProperties();
    }

    @Override
    public void destroy() {

    }

    protected String getHandlerCanonicalName() {
        return this.getClass().getCanonicalName();
    }

    @Override
    public void onEvent(Object o) {
        throw new EventHandlerException("Not supported");
    }

    protected boolean isSupervisionDisabled() {
        return !(boolean) headers.get(ACTIVE);
    }

    protected boolean isEventBasedClient() {
        return ((String) headers.get(CLIENT_TYPE)).equals(EVENT_BASED);
    }
}
