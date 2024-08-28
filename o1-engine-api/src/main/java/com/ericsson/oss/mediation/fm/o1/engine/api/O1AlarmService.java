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

package com.ericsson.oss.mediation.fm.o1.engine.api;

import java.util.List;
import java.util.Map;

import javax.ejb.Remote;

import com.ericsson.oss.itpf.sdk.core.annotation.EService;
import com.ericsson.oss.mediation.translator.model.EventNotification;

/**
 * Service that provides FM functionality to manage O1Node types.
 */
@EService
@Remote
public interface O1AlarmService {

    /**
     * Sends the transformed {@code EventNotification} representing the O1 alarm to FM.
     *
     * @param eventNotification
     *            the {@code EventNotification} representing the O1 alarm.
     */
    void sendAlarm(final EventNotification eventNotification);

    /**
     * Sends the transformed {@code EventNotifications} representing the O1 alarms to FM.
     *
     * @param eventNotifications
     *            the {@code EventNotifications} representing the O1 alarms.
     */
    void sendAlarms(final List<EventNotification> eventNotifications);

    /**
     * Translate the fields of an alarm that originated as a VES event from the node into a normalized EventNotification.
     *
     * @param alarm
     *            the fields of the alarm to be translated.
     * @return an {@code EventNotification} containing the transformed alarm fields.
     */
    EventNotification translateAlarm(final Map<String, Object> alarm);

    /**
     * Translate the fields of alarms that were read from the 'AlarmList.alarmRecords' object on the node into normalized EventNotifications.
     *
     * @param alarms
     *            a list of maps containing the properties of the alarms to be transformed.
     * @param meContextFdn
     *            the meContext FDN of the node from which the alarms were read.
     * @return a {@code List<EventNotification>} containing the transformed alarms.
     */
    List<EventNotification> translateAlarms(final List<Map<String, Object>> alarms, String meContextFdn);

    /**
     * Sends a request to raise the heartbeat alarm for the specified Node FDN
     *
     * @param networkElementFdn
     *            The FDN of the NetworkElement
     */
    void raiseHeartbeatAlarm(String networkElementFdn);

    /**
     * Sends a request to clear the heartbeat alarm for the specified Node FDN
     *
     * @param networkElementFdn
     *            The FDN of the NetworkElement
     */
    void clearHeartbeatAlarm(final String networkElementFdn);

    /**
     * Retrieves the entry in the cache matching the id provided and updates as follows:
     * <ul>
     * <li>Increments the notification count</li>
     * <li>If the notification count crosses the alarm rate threshold it sets the suspension status</li>
     * </ul>
     *
     * @param networkElementId
     *            The id to use for the cache entry.
     */
    void updateNodeSuspenderCache(final String networkElementId);

    /**
     * Adds the node to the node suspender cache
     *
     * @param networkElementId
     *            The id to use for the cache entry.
     */
    void addToNodeSuspenderCache(final String networkElementId);

    /**
     * Removes the node from the node suspender cache
     *
     * @param networkElementId
     *            The id to use for the cache entry.
     */
    void removeFromNodeSuspenderCache(final String networkElementId);

    /**
     * Resets the count of alarms received for all network elements in the node suspender cache
     */
    void resetNodeSuspenderCache();

    /**
     * Checks if alarm flow rate detection is enabled.
     * <p>
     * This determines whether nodes should be monitored for excessive notifications.
     */
    boolean isAlarmFlowRateEnabled();

    /**
     * Gets the node suspension status of the node from node suspender cache
     *
     * @param networkElementId
     *            The id to use for the cache entry.
     */
    boolean isNodeSuspended(final String networkElementId);

}
