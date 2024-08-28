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

package com.ericsson.oss.mediation.o1.yang.handlers.netconf.util;

import javax.inject.Inject;

import com.ericsson.oss.itpf.common.event.ComponentEvent;
import com.ericsson.oss.itpf.sdk.recording.EventLevel;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import lombok.extern.slf4j.Slf4j;

/**
 * System recorder helper class.
 */
@Slf4j
public class WriteSystemRecorder {

    private static final String SOURCE_FLOW_ENGINE = "Write flow";
    private static final String RESOURCE_DPS = "DPS Database";

    @Inject
    private SystemRecorder systemRecorder;

    /**
     * Logs and records the information about handler's start-up and the corresponding input data details.
     *
     * @param handlerName
     *            Handler name, example: com.ericsson.oss.mediation.cba.handlers.GetSchemaAndSWInventoryHandler. Dots will be removed and will be
     *            normalized to "GETSCHEMAANDSWINVENTORYHANDLER".
     * @param event
     *            Handler Event
     */
    public void recordInvocationDetails(final String handlerName, final String fdn, final ComponentEvent event) {
        final String invocationLogLine = LoggingUtil.constructHandlerInvocationLogLine(handlerName, fdn);
        systemRecorder.recordEvent(LoggingUtil.convertToRecordingFormat(handlerName), EventLevel.DETAILED, SOURCE_FLOW_ENGINE, RESOURCE_DPS,
                invocationLogLine);

        LoggingUtil.logEventDetails(event);
    }

    /**
     * Logs and records information about the time it took the handler to do its work.
     *
     * @param handlerName
     *            Handler name, example: com.ericsson.oss.mediation.cba.handlers.GetSchemaAndSWInventoryHandler. Dots will be removed and will be
     *            normalized to "GETSCHEMAANDSWINVENTORYHANDLER".
     * @param nodeFdn
     *            Invocation FDN, for example CMFunctionFDN or SupervisionFDN.
     * @param elapsedTime
     *            Handlers invocation elapsed time.
     */
    public void recordTimeTaken(final String handlerName, final String nodeFdn, final long elapsedTime, final String operationType) {
        final String timeTakenLogLine = LoggingUtil.constructTimeTakenLogLine(handlerName, nodeFdn, elapsedTime);
        log.debug(timeTakenLogLine);
        systemRecorder.recordEvent(LoggingUtil.convertToRecordingFormat(handlerName),
                EventLevel.DETAILED, SOURCE_FLOW_ENGINE + ":" + operationType, RESOURCE_DPS, timeTakenLogLine);
    }
}
