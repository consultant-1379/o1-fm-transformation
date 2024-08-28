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

package com.example.ves.util;

import org.json.JSONObject;

public class VesBodyGenerator {

    private static String VES_LISTENER_URI = "http://ves-sdo-collector:8080/eventListener/v1";

    private VesBodyGenerator() {}

    public static String getEvent() {
        final JSONObject commonEventHeaderObject = new JSONObject();
        commonEventHeaderObject.put("eventId", "#RandomString(20)");
        commonEventHeaderObject.put("sourceName", "PATCHED_sourceName");
        commonEventHeaderObject.put("version", 3.0);

        final JSONObject eventObject = new JSONObject();
        eventObject.put("commonEventHeader", commonEventHeaderObject);

        final JSONObject requestBody = new JSONObject();
        requestBody.put("vesServerUrl", VES_LISTENER_URI);
        requestBody.put("event", eventObject);

        return requestBody.toString();
    }
}
