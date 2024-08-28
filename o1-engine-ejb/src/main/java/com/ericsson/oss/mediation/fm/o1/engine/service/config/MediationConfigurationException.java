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

package com.ericsson.oss.mediation.fm.o1.engine.service.config;

public class MediationConfigurationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public MediationConfigurationException(final String message) {
        super(message);
    }

    public MediationConfigurationException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public MediationConfigurationException(final Throwable cause) {
        super(cause);
    }
}
