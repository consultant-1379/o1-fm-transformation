/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2024
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.oss.mediation.fm.o1.common.util;

import static com.ericsson.oss.mediation.translator.model.Constants.FAILURE;
import static com.ericsson.oss.mediation.translator.model.Constants.SUCCESS;

import com.ericsson.oss.mediation.fm.supervision.response.AlarmSupervisionResponse;

/**
 * Helper class for creating AlarmSupervisionResponse success or failure events.
 */
public class AlarmSupervisionResponseHelper {

    AlarmSupervisionResponseHelper() {};

    public static final AlarmSupervisionResponse createAlarmSupervisionResponseFailure(final String nodeFdn, final boolean active) {
        return createAlarmSupervisionResponse(nodeFdn, active, FAILURE);
    }

    public static final AlarmSupervisionResponse createAlarmSupervisionResponseSuccess(final String nodeFdn, final boolean active) {
        return createAlarmSupervisionResponse(nodeFdn, active, SUCCESS);
    }

    private static AlarmSupervisionResponse createAlarmSupervisionResponse(final String nodeFdn, final boolean active, final String result) {
        final AlarmSupervisionResponse response = new AlarmSupervisionResponse();
        response.setNodeFdn(nodeFdn);
        response.setActive(active);
        response.setResult(result);
        return response;
    }
}
