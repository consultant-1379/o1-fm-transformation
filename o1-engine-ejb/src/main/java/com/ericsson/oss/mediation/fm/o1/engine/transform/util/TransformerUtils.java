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
package com.ericsson.oss.mediation.fm.o1.engine.transform.util;

import com.ericsson.oss.mediation.fm.o1.engine.transform.TransformerException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.Converter;
import org.apache.commons.jelly.JellyContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class contains utility methods for the operations of transformation.
 */
public class TransformerUtils {

    private static final Logger log = LoggerFactory.getLogger(TransformerUtils.class);

    /**
     * Returns the class name of the given target object.
     *
     * @param target the target object.
     * @return the class name of the target object or null if target is null.
     */
    public static String getClassName(Object target) {
        if (target == null) {
            return null;
        }
        return target.getClass().getName();
    }

    /**
     * Creates a new instance of specified class using the specified class
     * loader.
     *
     * @param <T> the object type.
     * @param className the name of class.
     * @param args the arguments to pass in the class constructor.
     * @param classLoader the class loader to use to load class.
     * @param context the jelly context.
     * @return the instance created.
     */
    public static <T> T createInstance(String className, final String[] args, final ClassLoader classLoader, final JellyContext context) {
        Class<T> clazz = null;
        try {
            className = className.replaceFirst(".class", "");
            clazz = (Class<T>) classLoader.loadClass(className);
        } catch (ClassNotFoundException ex) {
            throw new TransformerException("Error loading instance of class: " + className, ex);
        }
        try {
            Constructor<?> constructor = clazz.getConstructor(String[].class, JellyContext.class);
            return (T) constructor.newInstance(args, context);
        } catch (Exception ex) {
            log.error("Error creating instance of using constructor: " + className, ex);
            throw new TransformerException("Error creating instance of class using constructor: " + className, ex);
        }
    }

    /**
     * Returns all fields of the specified class. Fields are returned as map
     * where the key is the name of field and the value is the field itself.
     *
     * @param clazz the target class.
     * @return a map of field-name/field pairs.
     */
    public static final Map<String, Field> getFieldMap(final Class<?> clazz) {
        final Map<String, Field> fields = new HashMap<>();
        Class<?> current = clazz;
        do {
            for (Field field : current.getDeclaredFields()) {
                fields.put(field.getName(), field);
            }
            current = current.getSuperclass();
        } while (!current.equals(Object.class));
        return fields;
    }

    /**
     * Convert the input object into an output object of the specified type.
     *
     * @param <T> the result type of the conversion
     * @param value the input value to be converted
     * @param dstType data type to which this value should be converted
     * @return the converted value.
     */
    public static <T> T convert(final Object value, final Class<T> dstType) {
        final Class sourceType = value == null ? null : value.getClass();
        Object converted = value;
        Converter converter = findConverter(sourceType, dstType);
        if (converter != null) {
            converted = converter.convert(dstType, value);
        }
        converted = getConverted(dstType, converted);
        return (T) converted;
    }

    private static <T> Object getConverted(final Class<T> dstType, Object converted) {
        Converter converter;
        if (dstType == String.class && converted != null && !(converted instanceof String)) {
            converter = ConvertUtils.lookup(String.class);
            if (converter != null) {
                converted = converter.convert(String.class, converted);
            }
            if (converted != null && !(converted instanceof String)) {
                converted = converted.toString();
            }
        }
        return converted;
    }

    /**
     * Finds a converter by source and destination types.
     *
     * @param srcType class of the value being converted
     * @param dstType class of the value to be converted to
     * @return the Converter or null if not found
     */
    public static Converter findConverter(final Class<?> srcType, final Class<?> dstType) {
        if (dstType == null) {
            throw new IllegalArgumentException("dstType is null");
        }
        if (srcType == null) {
            return ConvertUtils.lookup(dstType);
        }
        Converter converter = null;
        if (dstType == String.class) {
            converter = ConvertUtils.lookup(srcType);
            if (converter == null && (srcType.isArray() || Collection.class.isAssignableFrom(srcType))) {
                converter = ConvertUtils.lookup(String[].class);
            }
            if (converter == null) {
                converter = ConvertUtils.lookup(String.class);
            }
            return converter;
        }
        if (dstType == String[].class) {
            if (srcType.isArray() || Collection.class.isAssignableFrom(srcType)) {
                converter = ConvertUtils.lookup(srcType);
            }
            if (converter == null) {
                converter = ConvertUtils.lookup(String[].class);
            }
            return converter;
        }
        return ConvertUtils.lookup(dstType);
    }
}
