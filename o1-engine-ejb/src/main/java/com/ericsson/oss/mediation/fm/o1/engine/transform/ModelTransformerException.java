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
package com.ericsson.oss.mediation.fm.o1.engine.transform;

/**
 * Thrown by model transformer.
 */
public class ModelTransformerException extends RuntimeException {

    private static final long serialVersionUID = -8199528815357993885L;

    /**
     * Constructs a new model transformer exception with the specified detail
     * message.
     *
     * @param message the detail message.
     */
    public ModelTransformerException(final String message) {
        super(message);
    }

    /**
     * Constructs a new model transformer exception with the specified detail
     * message and cause.
     *
     * @param message the detail message.
     * @param cause the cause.
     */
    public ModelTransformerException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new model transformer exception with the specified cause.
     *
     * @param cause the cause.
     *
     */
    public ModelTransformerException(final Throwable cause) {
        super(cause);
    }

}
