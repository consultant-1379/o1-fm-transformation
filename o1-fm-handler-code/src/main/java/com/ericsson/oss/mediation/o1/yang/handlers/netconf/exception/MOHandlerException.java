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

package com.ericsson.oss.mediation.o1.yang.handlers.netconf.exception;

/**
 * Generic Exception for MO Handlers.
 */
public class MOHandlerException extends RuntimeException {

    private static final long serialVersionUID = -258148023981853346L;

    public MOHandlerException() {
        super();
    }

    public MOHandlerException(final String message) {
        super(message);
    }

    public MOHandlerException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public MOHandlerException(final Throwable cause) {
        super(cause);
    }
}
