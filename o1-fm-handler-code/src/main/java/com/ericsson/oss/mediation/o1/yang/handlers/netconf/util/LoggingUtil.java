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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.common.event.ComponentEvent;

/**
 * Provides logging functionality common between all or some of the handlers.
 */
public abstract class LoggingUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingUtil.class);

    private static final String RECORDING_FORMAT_PREFIX = "YANG.";
    private static final String OPEN_PARENTHESIS = " ('";
    private static final String START_TIME = "Start Time=%s,";
    private static final String FINISH_TIME = "Finish Time=%s";
    private static final String OPERATION_TYPE = "Operation Type=%s,";
    private static final String MANAGED_OBJECT = "Managed Object=%s,";
    private static final String MO_NAME = "MO Name=%s,";

    private LoggingUtil() {

    }

    /**
     * Converts the name of the handler to the format that Service Framework API
     * excepts.
     * <p>
     * <b>Example Output Format:</b> "SYNC_NODE.CONVERTED_HANDLER_NAME"
     *
     * @param handlerName
     *            - meaningful name of the handler
     * @return converted name of the handler according to the above format
     */
    public static String convertToRecordingFormat(final String handlerName) {
        if (handlerName == null) {
            throw new IllegalArgumentException("Handler name is null!");
        }

        String name = handlerName;

        final int lastIndex = handlerName.lastIndexOf(".");
        if (lastIndex != -1) {
            name = handlerName.substring(lastIndex + 1).toUpperCase();
        }

        return RECORDING_FORMAT_PREFIX + name.toUpperCase();
    }

    /**
     * Logs attribute details for the <code>ComponentEvent</code> passed.
     *
     * @param event
     *            event for which the attributes are to be logged
     * @throws IllegalArgumentException
     *             if <code>event</code> is null
     */
    public static void logEventDetails(final ComponentEvent event) throws IllegalArgumentException {
        if (event == null) {
            throw new IllegalArgumentException("ComponentEvent passed cannot be null.");
        }

        LOGGER.trace("Event version: '{}', namespace: '{}', class: '{}'", event.getVersion(), event.getNamespace(), event.getClass().getSimpleName());

        LOGGER.trace("=====> Event headers ('{}'): <=====", event.getName());
        for (final Entry<String, Object> entry : event.getHeaders().entrySet()) {
            LOGGER.trace("--> '{}': '{}'", entry.getKey(), entry.getValue());
        }
    }

    /**
     * Creates a specific log line indicating an invocation of a handler.
     * <p>
     * <b>Output Format:</b> "Starting <i>handler</i> (<i>FDN</i>)..."
     *
     * @param handlerName
     *            name of the handler to be included in the log line
     * @param fdn
     *            FDN identifying the node for which the log line is being
     *            constructed
     * @return constructed log line
     */
    public static String constructHandlerInvocationLogLine(final String handlerName, final String fdn) {
        final StringBuilder logLine = new StringBuilder("Starting ");
        logLine.append(handlerName);
        logLine.append(OPEN_PARENTHESIS);
        logLine.append(fdn);
        logLine.append("')...");

        return logLine.toString();
    }

    /**
     * Creates a specific log line for the time taken to execute for a handler
     * or a specific operation (i.e. complete sync).
     * <p>
     * <b>Output Format:</b>
     * "<i>operation</i> (<i>FDN</i>) took <i>x</i> ms to execute."
     *
     * @param operationName
     *            name of the operation for which the total time is calculated
     *            (could be simply the name of the handler)
     * @param fdn
     *            FDN identifying the node for which the log line is being
     *            constructed
     * @param timeTaken
     *            time to be included in the log line
     * @return constructed log line
     */
    public static String constructTimeTakenLogLine(final String operationName, final String fdn, final long timeTaken) {
        final StringBuilder logLine = new StringBuilder(operationName);
        logLine.append(OPEN_PARENTHESIS);
        logLine.append(fdn);
        logLine.append("') took [");
        logLine.append(timeTaken);
        logLine.append("] ms to execute.");

        return logLine.toString();
    }

    /**
     * Creates a specific log line for Instrumentation.
     *
     * @param startTime
     *            start time of operation.
     * @param operationName
     *            name of the operation for which the total time is calculated
     *            (could be simply the name of the handler)
     * @param fdn
     *            FDN identifying the node for which the log line is being
     *            constructed
     * @param moName
     *            MO Name.
     * @param finishTime
     *            finish time of operation.
     * @return constructed log line
     */
    public static String constructMOInstrumentationRecorder(final long startTime, final String operationName, final String fdn, final String moName,
            final long finishTime) {
        final StringBuilder logLine = new StringBuilder();
        final Date startTimeDate = new Date(startTime);
        final Date finishTimeDate = new Date(finishTime);
        final DateFormat formatter = new SimpleDateFormat("HH:mm:ss.SSS");
        logLine.append(String.format(START_TIME, formatter.format(startTimeDate)));
        logLine.append(String.format(OPERATION_TYPE, operationName));
        logLine.append(String.format(MANAGED_OBJECT, fdn));
        logLine.append(String.format(MO_NAME, moName));
        logLine.append(String.format(FINISH_TIME, formatter.format(finishTimeDate)));
        return logLine.toString();
    }

    /**
     * Creates a specific log line for an error that occurred during execution
     * of a handler, including the information about resetting
     * <code>syncStatus</code>.
     * <p>
     * <b>Output Format:</b>
     * "Error in: <i>handler</i> (<i>FDN</i>). Reset syncStatus to <i>UNSYNCHRONIZED</i>. Exception message: <i>message</i>"
     *
     * @param handlerName
     *            name of the handler to be included in the log line
     * @param fdn
     *            FDN identifying the node for which the log line is being
     *            constructed
     * @param exceptionMessage
     *            message for the exception thrown to be included in the log
     *            line
     * @return constructed log line
     */
    public static String constructErrorLogLine(final String handlerName, final String fdn, final String exceptionMessage) {
        final StringBuilder logLine = new StringBuilder("Error in: ");
        logLine.append(handlerName);
        logLine.append(OPEN_PARENTHESIS);
        logLine.append(fdn);
        logLine.append("'). Exception message: ");
        logLine.append(exceptionMessage);

        return logLine.toString();
    }

}
