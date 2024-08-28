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

import org.apache.commons.jelly.JellyContext;

import java.util.HashMap;
import java.util.Map;

/**
 * Basic class for converters and their arguments.
 */
public abstract class AbstractArgumentableTransformer {

    protected final Map<String, String> args = new HashMap<>();
    protected JellyContext context;

    /**
     * Creates a new 'argumentable' transformer.
     *
     * @param args    the arguments passed to this model transformer in the form:
     *                argument1=value1, argument2=value2, ..., argumentN=valueN
     * @param context the model transformer jelly context
     */
    public AbstractArgumentableTransformer(final String[] args, final JellyContext context) {
        this.context = context;
        if (args == null) {
            return;
        }
        for (String arg : args) {
            final String[] splited = arg.split("=", 2);
            if (splited.length == 2) {
                final String argName = splited[0];
                final String argValue = splited[1];
                this.args.put(argName, argValue);
            } else {
                throw new TransformerException("Invalid args form: '" + arg + "'. Args must be in the form key=value");
            }
        }
    }

    /**
     * Returns the argument with the specified name or null if argument does not
     * exist.
     *
     * @param argName the argument name
     * @return the argument value
     */
    protected String getArg(String argName) {
        return args.get(argName);
    }

    /**
     * Returns the argument with the specified name.
     *
     * @param argName the argument name
     * @return the argument value
     * @throws MissingArgumentException if argument does not exist
     */
    protected String getRequiredArg(String argName) {
        if (args.containsKey(argName)) {
            return args.get(argName);
        }
        throw new MissingArgumentException("No '" + argName + "' arg specified", getClass());
    }

    /**
     * Checks if the argument with the given name is present in the arguments
     * list.
     *
     * @param argName the argument name
     * @return true if argument is the arguments list, false otherwise.
     */
    protected boolean hasArg(String argName) {
        return args.containsKey(argName);
    }
}
