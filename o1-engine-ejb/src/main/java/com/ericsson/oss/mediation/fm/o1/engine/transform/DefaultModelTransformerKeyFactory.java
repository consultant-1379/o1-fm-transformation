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

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * DefaultModelTransformerKeyFactory is the default implementation of
 * ModelTransformerKeyFactory. This factory creates instances of
 * ModelTransformerKey.
 */
public class DefaultModelTransformerKeyFactory implements ModelTransformerKeyFactory<ModelTransformerKey> {

    /**
     * Creates a new ModelTransformerKey.
     *
     * @param configuration the model transformer configuration.
     * @return a new ModelTransformerKey.
     */
    @Override
    public ModelTransformerKey createKey(final Document configuration) {
        if (configuration == null) {
            throw new IllegalArgumentException("configuration cannot be null");
        }
        final Element documentElement = configuration.getDocumentElement();
        final String id = getAttribute(documentElement,"id");
        final String version = getAttribute(documentElement,"version");
        return new ModelTransformerKey(id, version);
    }

    private String getAttribute(final Element documentElement, final String attributeName) {
        final String value = documentElement.getAttribute(attributeName);
        if (value == null || value.isEmpty()) {
            throw new TransformerException(attributeName + " cannot be null or empty: " + value);
        }
        return value;
    }
}
