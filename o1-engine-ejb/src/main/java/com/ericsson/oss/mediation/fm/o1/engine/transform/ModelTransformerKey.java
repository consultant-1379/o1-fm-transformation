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

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * ModelTransformerKey is used to help identify the correct transformer script to use.
 */
@Getter
@ToString
@EqualsAndHashCode
public class ModelTransformerKey {

    private String id;
    private String version;

    /**
     * Creates a new config key with the given identifier and the given version.
     * @param id the identifier.
     * @param version the version.
     */
    public ModelTransformerKey(final String id, final String version) {
        this.id = id;
        this.version = version;
    }
}
