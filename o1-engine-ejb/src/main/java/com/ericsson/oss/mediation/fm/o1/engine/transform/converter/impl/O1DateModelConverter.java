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

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.apache.commons.jelly.JellyContext;

import com.ericsson.oss.mediation.fm.o1.engine.transform.converter.AbstractModelConverter;
import com.ericsson.oss.mediation.fm.o1.engine.transform.converter.ModelConverterException;

import lombok.extern.slf4j.Slf4j;

/**
 * A converter implementation that converts a date-time string in the OpenAPI (rfc3339) format to a string that matches the format argument provided
 * to the converter.
 * <p>
 * Here is an example of how the converter should be used:
 *
 * <pre>
 * &lt;t:convert-set-model-property oid="eventTime"
 *      mappedBy="eventTime"
 *      converter="O1DateModelConverter"
 *      args="format=yyyyMMddHHmmss.SSS"/>
 * </pre>
 *
 * For example, if used with format string above, '2023-09-06T11:32:01.743Z' would be converted to '20230906113201.743'.
 */
@Slf4j
public class O1DateModelConverter extends AbstractModelConverter<String, String> {

    /**
     * Creates a new O1DateModelConverter.
     *
     * @param args
     *            the arguments passed to this SnmpDateConverter.
     * @param context
     *            the model transformer context.
     */
    public O1DateModelConverter(final String[] args, final JellyContext context) {
        super(args, context);
    }

    /**
     * Converts the input date value provided to the format indicated by the 'format' argument configured for the converter.
     *
     * @param value
     *            the input value to be converted
     * @param dstField
     *            the model field to be set to converted date value
     * @return the converted date value
     */
    @Override
    public String convert(final String value, final Field dstField) {
        if (value == null) {
            return null;
        }

        final String formatValue = getRequiredArg("format");

        final String cleanedValue = removeNonDigitCharacters(value);

        // Extract date and time components
        final int year = Integer.parseInt(cleanedValue.substring(0, 4));
        final int month = Integer.parseInt(cleanedValue.substring(4, 6));
        final int day = Integer.parseInt(cleanedValue.substring(6, 8));
        final int hour = Integer.parseInt(cleanedValue.substring(8, 10));
        final int minute = Integer.parseInt(cleanedValue.substring(10, 12));
        final int second = Integer.parseInt(cleanedValue.substring(12, 14));

        final Calendar c = Calendar.getInstance();
        c.clear();
        c.set(year, month - 1, day, hour, minute, second);
        c.set(Calendar.MILLISECOND, extractMilliseconds(cleanedValue));

        final SimpleDateFormat simpleDateFormat = new SimpleDateFormat(formatValue);
        final String convertedValue = simpleDateFormat.format(c.getTime());

        log.debug("Converted {} to {}", value, convertedValue);
        return convertedValue;
    }

    private String removeNonDigitCharacters(final String value) {
        final String cleanedValue = value.replaceAll("[^0-9]", "");

        // Ensure that the cleaned string has at least 14 characters (YYYYMMDDHHMMSS)
        if (cleanedValue.length() < 14) {
            throw new ModelConverterException("Invalid input value: " + value);
        }
        return cleanedValue;
    }

    private int extractMilliseconds(final String value) {
        if (value.length() <= 14) {
            return 0; // Return 0 if there are no milliseconds.
        }

        // return integers from position 14 to a maximum of the first 3 characters beyond that position
        return Integer.parseInt(value.substring(14, Math.min(value.length(), 17)));
    }
}
