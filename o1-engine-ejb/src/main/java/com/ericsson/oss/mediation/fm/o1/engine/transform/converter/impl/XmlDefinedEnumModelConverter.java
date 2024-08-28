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

package com.ericsson.oss.mediation.fm.o1.engine.transform.converter.impl;

import com.ericsson.oss.mediation.fm.o1.engine.transform.Constants;
import com.ericsson.oss.mediation.fm.o1.engine.transform.converter.AbstractModelConverter;
import com.ericsson.oss.mediation.fm.o1.engine.transform.converter.ModelConverterException;

import java.lang.reflect.Field;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.jelly.JellyContext;

/**
 * A converter implementation that converts a enum defined in a xml file into a String. This converter can be used in the following way:
 * <p>
 * <pre>
 *  &lt;t:enums>
 *     &lt;t:enum name="ItuPerceivedSeverity">
 *          &lt;t:enum-entries>
 *              &lt;t:enum-entry ordinal="1">CLEARED</t:enum-entry>
 *              &lt;t:enum-entry ordinal="2">INDETERMINATE</t:enum-entry>
 *              &lt;t:enum-entry ordinal="3">CRITICAL</t:enum-entry>
 *              &lt;t:enum-entry ordinal="4">MAJOR</t:enum-entry>
 *              &lt;t:enum-entry ordinal="5">MINOR</t:enum-entry>
 *              &lt;t:enum-entry ordinal="6">WARNING</t:enum-entry>
 *          &lt;/t:enum-entries>
 *      &lt;/t:enum>
 * &lt;/t:enums>
 * .....................
 * &lt;t:convert-set-model-property oid="severity"
 *      mappedBy="perceivedSeverity"
 *      converter="XmlDefinedEnumModelConverter"
 *      args="enum=ItuPerceivedSeverity"/>
 * </pre>
 * The argument 'enum' is mandatory.
 * The enum entry value is determined by the integer value received from network equipment (ordinal)
 */
@Slf4j
public class XmlDefinedEnumModelConverter extends AbstractModelConverter<Object, String> {

    private static final String ENUM_ARG_NAME = "enum";

    /**
     * Creates a new XmlDefinedEnumConverter.
     *
     * @param args
     *         the arguments passed to this XmlDefinedEnumModelConverter.
     * @param context
     *         the model transformer context.
     */
    public XmlDefinedEnumModelConverter(final String[] args, final JellyContext context) {
        super(args, context);
    }

    /**
     * Convert the specified ordinal integer value to an enum value in string form.
     *
     * @param updatedValue
     *         the input ordinal value to be converted
     * @param dstField
     *         the model field to be set to converted value
     * @return the converted enum value
     */
    @Override
    public String convert(final Object updatedValue, final Field dstField) {
        Integer value = null;
        if (updatedValue != null) {
            if (updatedValue instanceof Integer) {
                value = (Integer) updatedValue;
            } else if (updatedValue instanceof String) {
                value = Integer.parseInt((String) updatedValue);
            } else {
                log.warn("Probable cause type is not valid");
            }
        } else {
            return null;
        }

        final String enumName = getRequiredArg(ENUM_ARG_NAME);
        final JellyContext parent = context.getParent();
        final Map<String, Map<Integer, String>> enumsMap = (Map<String, Map<Integer, String>>) parent.getVariable(Constants.ENUMS_MAP);
        if (enumsMap != null) {
            final Map<Integer, String> enumMap = enumsMap.get(enumName);
            if (enumMap == null) {
                log.error("No such enum: " + enumName);
                throw new ModelConverterException("No such enum: " + enumName);
            }
            if (enumMap.containsKey(value)) {
                return enumMap.get(value);
            } else {
                return String.valueOf(value);
            }
        } else {
            log.error("Invalid XmlDefinedEnumConverter usage: no such enum defined with name: " + enumName);
            throw new ModelConverterException("Invalid XmlDefinedEnumConverter usage: no such enum defined with name: " + enumName);
        }
    }
}
