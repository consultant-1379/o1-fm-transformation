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

package com.ericsson.oss.mediation.o1.event.utils;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion.VersionFlag;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public class O1NotificationUtil {

    private static final String VES_TOP_LEVEL_KEY = "event";
    private static final List<String> VES_SPECIFIC_KEYS = Arrays.asList(VES_TOP_LEVEL_KEY, "commonEventHeader", "stndDefinedFields", "data");

    /**
     * Accepts an O1 notification and maps the contents of the {@code VES_SPECIFIC_KEYS} into a flattened map.
     *
     * @param o1Notification
     *            the complete O1 Notification
     * @param normalisedNotification
     *            a flattened map containing only the contents of the {@code VES_SPECIFIC_KEYS}
     *            * Note : in the case of 3GPP specified O1 Notification - this relates to the contents found in "data" key of a Ves Notification
     */
    public static void normaliseO1Notification(final Map<String, Object> o1Notification, final Map<String, Object> normalisedNotification) {
        if (o1Notification == null) {
            return;
        }

        normalise(VES_TOP_LEVEL_KEY, o1Notification, normalisedNotification);
    }

    public static JsonSchema getJsonSchema() {
        log.debug("creating CommonEvent JsonSchema");

        final InputStream inputStream = getResource("CommonEventFormat_30.2.1_ONAP.json");
        final JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V4);
        final JsonSchema schema = factory.getSchema(inputStream);

        log.debug("CommonEvent JsonSchema created successfully");
        return schema;
    }

    @SuppressWarnings("unchecked")
    private static void normalise(final String key, final Object value, final Map<String, Object> normalisedEvent) {
        if (value instanceof Map<?, ?>) {
            if (VES_SPECIFIC_KEYS.contains(key)) {
                ((Map<String, ?>) value).entrySet().forEach(e -> normalise(e.getKey(), e.getValue(), normalisedEvent));
                return;
            }

            normalisedEvent.put(key, value);
            return;
        }

        normalisedEvent.put(key, value);
    }

    private static InputStream getResource(final String fileName) {
        return O1NotificationUtil.class.getClassLoader().getResourceAsStream(fileName);
    }
}
