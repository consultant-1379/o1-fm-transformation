/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.oss.mediation.o1.yang.handlers.netconf.parser;

import com.ericsson.oss.itpf.modeling.modelservice.exception.ConstraintViolationException;
import com.ericsson.oss.itpf.modeling.modelservice.typed.core.DataType;

import java.util.List;

/**
 * Constants for model parser.
 */
public final class ParserConstants {

    public static final String SINGLETON_MANAGEDOBJECTNAME = "1";
    public static final String DPS_LIST_START = "[";
    public static final String DPS_LIST_VALUE_SEPARATOR = ",";
    public static final String DPS_LIST_END = "]";
    public static final String XML_NAMESPACE_ATTR = "xmlns";
    public static final String XML_NAMESPACE_PREFIX_SEPARATOR = ":";
    public static final String DATA = "data";
    public static final String NETOPEER = "netopeer";

    private ParserConstants() {
    }


    /**
     * Parses the given string to a numeric/decimal data type.
     *
     * @param dataType       data type
     * @param attributeValue attribute value to be parsed
     * @return the number parsed
     * @throws ConstraintViolationException in case the string could not be parsed to the specified numeric/decimal data type
     */
    public static Number parseNumber(final DataType dataType, final String attributeValue) throws ConstraintViolationException {
        try {
            if (dataType == DataType.BYTE) {
                return Byte.valueOf(attributeValue);
            } else if (dataType == DataType.SHORT) {
                return Short.valueOf(attributeValue);
            } else if (dataType == DataType.INTEGER) {
                return Integer.valueOf(attributeValue);
            } else if (dataType == DataType.LONG) {
                return Long.valueOf(attributeValue);
            } else if (dataType == DataType.DOUBLE) {
                return Double.valueOf(attributeValue);
            } else {
                throw new ConstraintViolationException(String.format("Unsupported numeric/decimal data type <%s>", dataType.name()));
            }
        } catch (final NumberFormatException ex) {
            throw new ConstraintViolationException(String.format("Could not parse <%s> to data type <%s>", attributeValue, dataType.name()));
        }
    }

    public static String appendDpsList(final Object keyValue) {
        final List<?> keyValuesList = (List<?>) keyValue;
        final StringBuilder str = new StringBuilder();
        if (!keyValuesList.isEmpty()) {
            str.append(ParserConstants.DPS_LIST_START);
            int index = 0;
            for (; index < keyValuesList.size() - 1; index++) {
                str.append(keyValuesList.get(index)).append(ParserConstants.DPS_LIST_VALUE_SEPARATOR);
            }
            str.append(keyValuesList.get(index));
            str.append(ParserConstants.DPS_LIST_END);
        }
        return str.toString();
    }

    public static String removeNodeAppInstanceName(final String input) {

        final String[] split = input.split("---");

        if (split.length != 3) {
            return input;
        }

        if (split[0].trim().isEmpty()) {
            return input;
        }
        if (split[1].trim().isEmpty()) {
            return input;
        }
        if (split[2].trim().isEmpty()) {
            return input;
        }

        return split[0].trim() + "-" + split[2].trim();
    }
}