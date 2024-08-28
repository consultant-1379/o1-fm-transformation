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
 * Thrown by model transformer if a needed argument is missing.
 */
public class MissingArgumentException extends RuntimeException {

    private static final long serialVersionUID = -5070683853416450417L;

    /**
     * Constructs a new missing argument exception with the specified argument
     * name.
     *
     * @param argumentName the missing argument.
     * @param clazz the class that generated the exception.
     */
    public MissingArgumentException(final String argumentName, Class<? extends AbstractArgumentableTransformer> clazz) {
        super("You must define an argument called '" + argumentName + "' for this class: " + clazz.getName());
    }
}
