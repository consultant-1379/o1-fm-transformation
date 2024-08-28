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

package com.ericsson.oss.mediation.o1.event.exception;

public class O1ValidationException extends RuntimeException {

    private static final long serialVersionUID = 8491835268090727173L;

    public O1ValidationException(final String msg) {
        super(msg);
    }

    public O1ValidationException(final String msg, final Exception ex) {
        super(msg, ex);
    }
}
