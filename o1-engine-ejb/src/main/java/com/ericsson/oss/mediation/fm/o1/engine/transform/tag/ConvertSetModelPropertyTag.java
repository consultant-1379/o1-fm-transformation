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

import com.ericsson.oss.mediation.fm.o1.engine.transform.Constants;
import com.ericsson.oss.mediation.fm.o1.engine.transform.util.TransformerUtils;
import com.ericsson.oss.mediation.fm.o1.engine.transform.converter.AbstractModelConverter;
import org.apache.commons.jelly.JellyTagException;
import org.apache.commons.jelly.MissingAttributeException;

import java.lang.reflect.Field;
import java.util.Map;

public class ConvertSetModelPropertyTag extends AbstractArgumentableTag {

    private String converter; 

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
        if (converter == null) {
            logger.error("Attribute 'converter' not set");
            throw new MissingAttributeException("converter");
        }
        final Map<String, String> registeredConverters = (Map<String, String>) context.getVariable(Constants.REGISTERED_CONVERTERS);
        if (registeredConverters != null && registeredConverters.containsKey(converter)) {
            this.converter = registeredConverters.get(converter);
        } 
        try {
            final AbstractModelConverter propertyConverter = TransformerUtils.createInstance(converter, getArguments(),
                    context.getClassLoader(), context);
            final Map<String, Field> fieldMap = TransformerUtils.getFieldMap(eventNotification.getClass());
            final Field dstField = fieldMap.get(mappedBy);
            final Object alarmValue = getValue();
            final Object converted = propertyConverter.convert(alarmValue, dstField);
            setModelProperty(converted);
        } catch (Exception ex) {
            logger.error("Error executing ConvertSetModelPropertyTag. Converter is: "
                    + converter + " Oid is: " + oid + ", mappedBy is: " + mappedBy, ex);
            throw new JellyTagException("Error executing ConvertSetModelPropertyTag. Converter is: "
                    + converter + " Oid is: " + oid + ", mappedBy is: " + mappedBy, ex);
        }
    }

    public String getConverter() {
        return converter;
    }

    public void setConverter(final String converter) {
        this.converter = converter;
    } 

}
