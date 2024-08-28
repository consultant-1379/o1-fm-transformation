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

/**
 * All key factory must implements this interface.
 *
 * @param <T> the key type.
 */
public interface ModelTransformerKeyFactory<T extends ModelTransformerKey> {

    /**
     * Creates a new key.
     *
     * @param configuration the xml document configuration
     * @return the new key
     */
    T createKey(Document configuration);
}
