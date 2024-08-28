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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class TimedInstrumentedOperation<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(TimedInstrumentedOperation.class);

    protected T result;

    protected Exception suppressedPerformOperationException;

    public T getResult() {
        return result;
    }

    public void setResult(final T result) {
        this.result = result;
    }

    public Exception getSuppressedPerformOperationException() {
        return suppressedPerformOperationException;
    }

    public void preOperation() {
        try {
            preInstrumentation();
        } catch (final Exception e) {
            LOGGER.warn("Instrumentation failure", e);
        }
    }

    public void preInstrumentation() {}

    public void preOperationExceptionHandler(final Exception encounteredException) {
        LOGGER.warn("Exception encountered in preOperation", encounteredException);
    }

    public abstract void performOperation();

    public void performOperationExceptionHandler(final Exception encounteredException) {
        LOGGER.warn("Exception encountered in performOperation", encounteredException);
    }

    public void postOperation(final long operationProcessingTime) {
        try {
            postInstrumentation(operationProcessingTime);
        } catch (final Exception e) {
            LOGGER.warn("Instrumentation failure", e);
        }
    }

    public void postOperationExceptionHandler(final Exception encounteredException) {
        LOGGER.warn("Exception encountered in postOperation", encounteredException);
    }

    public void postInstrumentation(final long operationProcessingTime) {}

    public void execute() {

        long startTime = -1;
        try {
            startTime = System.currentTimeMillis();
            preOperation();
        } catch (final Exception e) {
            try {
                preOperationExceptionHandler(e);
            } catch (final Exception exceptionHandlerException) {
                LOGGER.warn("Exception encountered in preOperation exception handler", exceptionHandlerException);
            }
        }

        try {
            performOperation();
        } catch (final Exception e) {
            try {
                suppressedPerformOperationException = e;
                performOperationExceptionHandler(e);
            } catch (final Exception exceptionHandlerException) {
                LOGGER.warn("Exception encountered in performOperation exception handler", exceptionHandlerException);
            }
        }

        try {
            postOperation(startTime < 0 ? -1 : System.currentTimeMillis() - startTime);
        } catch (final Exception e) {
            try {
                postOperationExceptionHandler(e);
            } catch (final Exception exceptionHandlerException) {
                LOGGER.warn("Exception encountered in postOperation exception handler", exceptionHandlerException);
            }
        }
    }
}
