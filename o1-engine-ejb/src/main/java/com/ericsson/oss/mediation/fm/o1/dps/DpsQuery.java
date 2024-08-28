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

package com.ericsson.oss.mediation.fm.o1.dps;

import static com.ericsson.oss.mediation.fm.o1.common.Constants.AUTOMATIC_SYNCHRONIZATION;
import static com.ericsson.oss.mediation.fm.o1.common.Constants.COMMA_FM_ALARM_SUPERVISION_RDN;

import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import com.ericsson.oss.itpf.datalayer.dps.BucketProperties;
import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService;
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;

import lombok.extern.slf4j.Slf4j;

/**
 * Class for read only access to DPS.
 * <p>
 * Each method starts a new transaction which is committed when the method returns (nothing is commited).
 * Read only transactions do not need to be rolled back.
 * Mediation is suppressed.
 */
@Slf4j
@Singleton
@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
public class DpsQuery {

    @EServiceRef
    private DataPersistenceService dps;

    /**
     * Returns true if the 'AlarmSupervision.automaticSynchronization' attribute is set to true.
     * <p>
     * Returns false if the MO cannot be found or the attribute value is set to false.
     */
    public boolean isAutomaticSyncSet(final String networkElementFdn) {
        final String alarmSupervisionFdn = networkElementFdn.concat(COMMA_FM_ALARM_SUPERVISION_RDN);
        final ManagedObject mo = getLiveBucket().findMoByFdn(alarmSupervisionFdn);

        if (mo == null) {
            log.error("MO not found for fdn [{}]", alarmSupervisionFdn);
            return false;
        }

        return mo.getAttribute(AUTOMATIC_SYNCHRONIZATION);
    }

    private DataBucket getLiveBucket() {
        dps.setWriteAccess(false);
        return dps.getDataBucket("live", BucketProperties.SUPPRESS_MEDIATION);
    }

}
