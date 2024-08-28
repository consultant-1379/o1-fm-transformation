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

import org.apache.commons.jelly.JellyTagException;
import org.apache.commons.jelly.MissingAttributeException;

public class SetModelPropertyTag extends AbstractO1TransformerTag {

    @Override
    public void doTag() throws JellyTagException {
        if (mappedBy == null) {
            logger.error("Attribute 'mappedBy' not set");
            throw new MissingAttributeException("mappedBy");
        }
        if (oid == null && !(getParent() instanceof ValueHolder)) {
            logger.error("Attribute 'oid' not set");
            throw new MissingAttributeException("oid");
        }
        try {
            final Object value = getValue();
            setModelProperty(value);
        } catch (Exception ex) {
            logger.error("Error executing SetModelPropertyTag. Oid is: "
                    + oid + ", mappedBy is: " + mappedBy, ex);
            throw new JellyTagException("Error executing SetModelPropertyTag. Oid is: "
                    + oid + ", mappedBy is: " + mappedBy, ex);
        }
    }
}
