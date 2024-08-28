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

package com.ericsson.oss.mediation.o1.event.validator;

import static com.ericsson.oss.mediation.o1.event.utils.O1NotificationUtil.getJsonSchema;

import java.util.Set;

import org.json.JSONObject;

import com.ericsson.oss.mediation.o1.event.exception.O1ValidationException;
import com.ericsson.oss.mediation.o1.event.model.O1Notification;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.ValidationMessage;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class O1NotificationValidator {

    public void validate(final O1Notification o1Notification) {
        log.info("Validating O1Notification: {}", o1Notification);
        conformsToSchema(o1Notification.getNotification(), getJsonSchema());
    }

    private void conformsToSchema(final JSONObject payload, final JsonSchema schema) {

        Set<ValidationMessage> messageSet = null;
        try {
            final ObjectMapper mapper = new ObjectMapper();
            final String content = payload.toString();
            final JsonNode node = mapper.readTree(content);
            messageSet = schema.validate(node);
        } catch (final Exception e) {
            throw new O1ValidationException("Failed to validate the payload using schema", e);
        }

        if (!messageSet.isEmpty()) {
            messageSet.forEach(it -> log.debug("Schema validation error: {}", it.getMessage()));
            throw new O1ValidationException("Schema validation failed: " + messageSet);
        }
    }

}
