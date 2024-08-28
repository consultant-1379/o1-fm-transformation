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
package com.ericsson.oss.mediation.fm.o1.engine.transform.tag;

/**
 * A ContextVariableNotFound is thrown if the context does not 
 * contain an expected variable.
 * 
 */
public class ContextVariableNotFoundException extends RuntimeException {

    private static final long serialVersionUID = -6481615965427443903L;
    private final String variable;

    /**
     * Creates a new ContextVariableNotFound with the given variable name.
     * 
     * @param variable the name of missing variable.
     */
    public ContextVariableNotFoundException(String variable) {
        super("Context variable '" + variable + "' not found");
        this.variable = variable;
    } 

    /**
     * Returns the name of missing variable.
     * @return the name of missing variable.
     */
    public String getVariable() {
        return variable;
    }

}
