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

package com.ericsson.oss.mediation.fm.o1.handlers.util;

import com.ericsson.oss.mediation.core.events.MediationClientType;
import com.ericsson.oss.services.fm.service.model.FmMediationAlarmSyncRequest;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

import static com.ericsson.oss.mediation.fm.o1.common.Constants.COMMA_FM_ALARM_SUPERVISION_RDN;

@Slf4j
public class MediationTaskRequestUtil {

    public static FmMediationAlarmSyncRequest createFmMediationAlarmSyncRequest(final String networkElementFdn) {

        final FmMediationAlarmSyncRequest event = new FmMediationAlarmSyncRequest();
        event.setClientType(MediationClientType.EVENT_BASED.name());
        event.setProtocolInfo("FM");
        event.setJobId(UUID.randomUUID().toString());
        event.setNodeAddress(networkElementFdn.concat(COMMA_FM_ALARM_SUPERVISION_RDN));

        return event;
    }
}
