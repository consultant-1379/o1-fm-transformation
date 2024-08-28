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
 * Base class for all tags (AbstractSetModelPropertyTag) that have the attribute 'args'.
 */
public abstract class AbstractArgumentableTag extends AbstractO1TransformerTag {

    private String args;

    /**
     * Returns the arguments.
     *
     * @return the arguments.
     */
    public String getArgs() {
        return args;
    }

    /**
     * Set the arguments.
     *
     * @param args
     *            the arguments.
     */
    public void setArgs(final String args) {
        this.args = args;
    }

    /**
     * Returns the arguments as string array, or null if args string is null.
     *
     * @return the arguments as string array, or null if args string is null.
     */
    protected String[] getArguments() {
        if (args == null) {
            return null;
        }
        final String[] splitedArgs = args.split("\\,");
        for (int i = 0; i < splitedArgs.length; i++) {
            splitedArgs[i] = splitedArgs[i].trim();
        }
        return splitedArgs;
    }

}
