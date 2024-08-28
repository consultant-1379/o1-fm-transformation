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
package com.ericsson.oss.mediation.fm.o1.engine.transform.converter;

import com.ericsson.oss.mediation.fm.o1.engine.transform.AbstractArgumentableTransformer;
import com.ericsson.oss.mediation.fm.o1.engine.transform.MissingArgumentException;
import java.lang.reflect.Field;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.jelly.JellyContext;

/**
 * Base class for all transformer converters. 
 * The result of the 'convert' method is set to the field 
 * of the model identified by mappedBy; if the alarm data does not contain a value
 * with the OID associated with the converter the java default value will be set for
 * that type of field (null for object, false for boolean, zero for numbers etc.)
 * A converter can be defined in the following way:
 *
 * <convert-set-model-property oid="alarmId"
 * mappedBy="eventAgentId" converter="x.y.z.MyConverter.class" args="arg1=value1,arg2=value2, argN=valueN"/>
 *
 * @param <T> the converter input type
 * @param <V> the converter return type
 */
@Slf4j
public abstract class AbstractModelConverter<V, T> extends
        AbstractArgumentableTransformer {

    /**
     * Creates a new converter.
     *
     * @param args the arguments passed to this converter in the form:
     * arg1=value1, arg2=value2, ..., argN=valueN
     * @param context the model transformer context
     */
    public AbstractModelConverter(final String[] args, final JellyContext context) {
        super(args, context);
    }

    /**
     * Called by the framework.
     * Convert the specified value.
     *
     * @param value the input value to be converted
     * @param dstField the model field to be set to converted value
     * @return the converted value
     */
    public abstract T convert(V value, Field dstField);

    /**
     * Returns the argument with the specified name and throws a
     * MissingArgumentException if argument does not exist.
     *
     * @param argName the argument name
     * @return the argument value
     */
    protected String getRequiredArg(String argName) {
        if (args.containsKey(argName)) {
            return args.get(argName);
        }
        log.error("No '" + argName + "' arg specified");
        throw new MissingArgumentException(argName, getClass());
    }
}
