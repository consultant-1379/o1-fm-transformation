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


class AlarmRecords {

    static final Map<String, Object> ALARM_ONE = [
            'alarmId'          : 'dd03de9f-1fa3-4733-8688-9b2f23b6d13b',
            'objectInstance'   : 'ManagedElement=1,GNBDUFunction=2,NRSectorCarrier=3',
            'notificationId'   : 599801250,
            'alarmChangedTime' : '2023-10-20T07:01:27.02+00:00',
            'alarmType'        : 'COMMUNICATIONS_ALARM',
            'probableCause'    : '14',
            'specificProblem'  : 'GNBCUCP Service Unavailable',
            'perceivedSeverity': 'MINOR',
            'additionalText'   : 'No F1-C link exists to gNodeB-CU-CP listed in Additional Information'
    ]
    static final Map<String, Object> ALARM_TWO = [
            'alarmId'          : 'dd03de9f-1fa3-4733-8688-9b2f23b6d13c',
            'objectInstance'   : 'ManagedElement=1,GNBDUFunction=2,NRSectorCarrier=3',
            'notificationId'   : 599801250,
            'alarmChangedTime' : '2023-10-20T07:01:27.02+00:00',
            'alarmType'        : 'COMMUNICATIONS_ALARM',
            'probableCause'    : '2',
            'specificProblem'  : 'GNBCUUP Service Unavailable',
            'perceivedSeverity': 'WARNING',
            'additionalText'   : 'No F1-C link exists to gNodeB-CU-UP listed in Additional Information'
    ]
    static final Map<String, Object> ALARM_THREE = [
            'alarmId'          : 'dd03de9f-1fa3-4733-8688-9b2f23b6d13d',
            'objectInstance'   : 'ManagedElement=1,GNBDUFunction=2,NRSectorCarrier=3',
            'notificationId'   : 599801250,
            'alarmChangedTime' : '2023-10-20T07:01:27.02+00:00',
            'alarmType'        : 'COMMUNICATIONS_ALARM',
            'probableCause'    : '1',
            'specificProblem'  : 'GNBDU Service Unavailable',
            'perceivedSeverity': 'MAJOR',
            'additionalText'   : 'No F1-C link exists to gNodeB-DU listed in Additional Information'
    ]
    static final Map<String, Object> INVALID_ALARM = [
            'alarmId'          : 1,
            'objectInstance'   : 2,
            'notificationId'   : 599801250,
            'alarmChangedTime' : 3,
            'alarmType'        : 3,
            'probableCause'    : 3,
            'specificProblem'  : 3,
            'perceivedSeverity': 3,
            'additionalText'   : 3
    ]
    static final String OBJECT_INSTANCE_FDN = 'SubNetwork=5G141O1001,MeContext=5G141O1001,'


    static final List<Map<String, Object>> ALARM_RECORDS = Arrays.asList(ALARM_ONE, ALARM_TWO, ALARM_THREE);

    static final List<Map<String, Object>> FAULTY_ALARM_RECORDS = Arrays.asList(ALARM_ONE, ALARM_TWO, ALARM_THREE, INVALID_ALARM);

}
