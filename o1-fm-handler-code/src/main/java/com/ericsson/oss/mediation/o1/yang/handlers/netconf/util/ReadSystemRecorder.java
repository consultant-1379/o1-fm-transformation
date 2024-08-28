/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
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
import com.ericsson.oss.itpf.sdk.recording.ErrorSeverity;
import com.ericsson.oss.itpf.sdk.recording.EventLevel;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;

import lombok.extern.slf4j.Slf4j;

/**
 * System recorder helper class.
 */
@Slf4j
public class ReadSystemRecorder {

    private static final String SOURCE_FLOW_ENGINE = "Read flow";
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
    public void recordTimeTaken(final String handlerName, final String nodeFdn, final long elapsedTime) {
        final String timeTakenLogLine = LoggingUtil.constructTimeTakenLogLine(handlerName, nodeFdn, elapsedTime);
        log.debug(timeTakenLogLine);
        systemRecorder.recordEvent(LoggingUtil.convertToRecordingFormat(handlerName),
                EventLevel.DETAILED, SOURCE_FLOW_ENGINE, RESOURCE_DPS, timeTakenLogLine);
    }

    /**
     * @param eventId
     *            the type of the error recorded. The error type is considered API, i.e. once baselined may not be modified spontaneously. Consumers
     *            of recordings may code their logic against error types. The error type should contain a simplified namespace to avoid name clashes,
     *            e.g. "OLTP_DATABASE.VOLUME_FULL". Must not be null or empty String.
     * @param handlerName
     *            the name of the handler where the error occurred.
     * @param resource
     *            the entity directly affected by the error. Good examples are a Cell ID, a FDN, a Subscriber ID etc., in other words, the entity
     *            which directly relates to the event.
     * @param additionalInformation
     *            text with additional information. Must not exceed 50KB in size.
     */
    public void recordError(final String eventId, final String handlerName, final String resource, final String additionalInformation) {
        log.debug("{} - Handler '{}' failed on resource '{}' with message '{}'", eventId, handlerName, resource, additionalInformation);
        systemRecorder.recordError(eventId, ErrorSeverity.ERROR, handlerName, resource, additionalInformation);
    }

    /**
     * @param eventId
     *            the type of the error recorded. The error type is considered API, i.e. once baselined may not be modified spontaneously. Consumers
     *            of recordings may code their logic against error types. The error type should contain a simplified namespace to avoid name clashes,
     *            e.g. "OLTP_DATABASE.VOLUME_FULL". Must not be null or empty String.
     * @param handlerName
     *            the name of the handler where the error occurred.
     * @param resource
     *            the entity directly affected by the error. Good examples are a Cell ID, a FDN, a Subscriber ID etc., in other words, the entity
     *            which directly relates to the event.
     * @param additionalInformation
     *            text with additional information. Must not exceed 50KB in size.
     */
    public void recordWarning(final String eventId, final String handlerName, final String resource, final String additionalInformation) {
        log.debug("{} - Handler '{}' failed on resource '{}' with message '{}'", eventId, handlerName, resource, additionalInformation);
        systemRecorder.recordError(eventId, ErrorSeverity.WARNING, handlerName, resource, additionalInformation);
    }
}
