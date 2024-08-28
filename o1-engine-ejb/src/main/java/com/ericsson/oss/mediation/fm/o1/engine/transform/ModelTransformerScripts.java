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
 * This class is a container of transformer scripts.
 * All scripts all saved in an internal map in the form:
 * ID of Script / Pre-compiled Script
 */
public class ModelTransformerScripts {

    private final JellyContext rootContext;
    private final Map<String, ScriptDecorator> scripts;

    /**
     * Creates a new ModelTransformerScripts with the specified root context.
     *
     * @param rootContext the root context for this ModelTransformerScripts.
     */
    public ModelTransformerScripts(final JellyContext rootContext) {
        this.rootContext = rootContext;
        this.scripts = new HashMap<>();
    }

    /**
     * Returns the root context.
     *
     * @return the root context.
     */
    JellyContext getRootContext() {
        return rootContext;
    }

    /**
     * Add a script to this ModelTransformerScripts.
     *
     * @param id the id of associated to the script.
     * @param script the script to add.
     */
    void addScript(final String id, final ScriptDecorator script) {
        scripts.put(id, script);
    }

    /**
     * Returns the script with the specified id or null if this registry
     * contains no script for the id.
     *
     * @param id the id.
     * @return the script to which the specified id is mapped, or null if this
     * registry contains no script for the id.
     */
    ScriptDecorator getScript(final String id) {
        return scripts.get(id);
    }
}
