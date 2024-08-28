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
package com.ericsson.oss.mediation.fm.o1.engine.transform.helper

class NotifyAlarm {

    static Map<String, Object> createNotifyAlarm(final String notificationType) {
        def updatedAlarmFields = CommonAlarmFields.createCommonAlarmFields()
        Map<String, Object> ALARM_FIELDS = CommonAlarmFields.createCommonNotifyAlarmFields()
        ALARM_FIELDS.put('notificationType', notificationType)

        if (notificationType.matches("notifyNewAlarm|notifyChangedAlarmGeneral|notifyChangedAlarm")) {
            ALARM_FIELDS.put('additionalInformation', ['additionalKeyOne': 'additionalValue1', 'additionalKeyTwo': 'additionalValue2'])
            ALARM_FIELDS.put('specificProblem', 'some problem')
        }

        if (notificationType.matches("notifyAlarmListRebuilt")) {
            ALARM_FIELDS.put('additionalInformation', ['reason': 'System restarts'])
            ALARM_FIELDS.put('recordType', 'CLEARALL')
            ALARM_FIELDS.put('probableCause', 'reinitialized')
            ALARM_FIELDS.put('perceivedSeverity', 'WARNING')
            ALARM_FIELDS.put('specificProblem', 'NE and OSS alarms are not in sync')
        }

        ALARM_FIELDS.each { key, value ->
            updatedAlarmFields[key] = value
        }

        return updatedAlarmFields
    }
}
