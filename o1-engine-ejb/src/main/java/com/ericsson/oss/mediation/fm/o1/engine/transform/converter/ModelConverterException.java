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
package com.ericsson.oss.mediation.fm.o1.engine.transform.converter;

/**
 * Thrown by transformer converter.
 */
public class ModelConverterException extends RuntimeException {

    private static final long serialVersionUID = -1153171467622567342L;

    /**
     * Constructs a new converter exception with the specified detail message.
     *
     * @param message the detail message.
     */
    public ModelConverterException(final String message) {
        super(message);
    }

    /**
     * Constructs a new converter exception with the specified detail message.
     *
     * @param message the detail message.
     * @param cause the cause.
     */
    public ModelConverterException(final String message, final Throwable cause) {
        super(message, cause);
    }

}
