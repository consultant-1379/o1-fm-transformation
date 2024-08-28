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

package com.ericsson.oss.mediation.fm.o1.handlers;

import static com.ericsson.oss.mediation.fm.o1.common.Constants.HREF;
import static com.ericsson.oss.mediation.fm.o1.common.Constants.JMS;
import static com.ericsson.oss.mediation.fm.o1.common.Constants.NETWORK_ELEMENT_EQUALS;
import static com.ericsson.oss.mediation.fm.o1.common.Constants.OBJECT_MESSAGE;
import static com.ericsson.oss.mediation.fm.o1.common.util.FdnUtil.getMeContextId;
import static com.ericsson.oss.mediation.fm.o1.handlers.util.HandlerMessages.DN_PREFIX_MISSING;
import static com.ericsson.oss.mediation.fm.o1.handlers.util.HandlerMessages.UNKNOWN_NOTIFICATION;
import static com.ericsson.oss.mediation.fm.o1.handlers.util.HandlerMessages.UNSUPPORTED_NOTIFICATION;

import java.util.HashMap;

import com.ericsson.oss.mediation.fm.o1.instrumentation.O1HandlerStatistics;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.ericsson.oss.itpf.common.event.handler.annotation.EventHandler;
import com.ericsson.oss.itpf.common.event.handler.exception.EventHandlerException;
import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;
import com.ericsson.oss.mediation.flow.events.MediationComponentEvent;
import com.ericsson.oss.mediation.fm.o1.common.util.HrefUtil;
import com.ericsson.oss.mediation.fm.o1.engine.api.O1AlarmService;
import com.ericsson.oss.mediation.fm.o1.handlers.util.Attributes;
import com.ericsson.oss.mediation.fm.o1.handlers.util.NotificationTypeUtil;
import com.ericsson.oss.mediation.fm.util.EventNotificationUtil;
import com.ericsson.oss.mediation.translator.model.EventNotification;

import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;

/**
 * Handler responsible for receiving JMS messages from the 'O1FmNotifications' queue. It extracts the object from the message and verifies it is of
 * the expected type. It should be a {@code Map<String, Object>} containing the alarm fields. It adds the object extracted to the payload of
 * the {@code MediationComponentEvent} sent to the next handler.
 * The node suspended status is checked if alarm flow rate detection is enabled.
 * If the node is suspended, the header 'isNodeSuspended' is set to true and the remaining handlers in the flow will ignore the notification.
 * <p>
 * This Handler must be triggered as follows:
 * <ul>
 * <li>via JMS messages received on the 'O1FmNotifications' queue.</li>
 * <ul>
 * <p>
 */
@Slf4j
@EventHandler
public class FmNotificationHandler extends AbstractFmMediationHandler {

    private static final String HANDLER = FmNotificationHandler.class.getSimpleName();

    @EServiceRef
    private O1AlarmService alarmService;

    @Inject
    O1HandlerStatistics o1HandlerStatistics;

    @Override
    public Object onEventWithResult(final Object inputEvent) {

        log.debug("Received event of type {} ", inputEvent.getClass().getName());
        o1HandlerStatistics.incrementTotalNoOfAlarmsReceived();

        super.onEventWithResult(inputEvent);

        final HashMap<String, Object> alarmObject = getNotificationObject();
        validateAlarmObject(alarmObject);

        if (alarmService.isAlarmFlowRateEnabled()) {

            checkNotificationRate(alarmObject);

        } else {

            processAlarmNotification();
        }

        return new MediationComponentEvent(headers, alarmObject);
    }

    @Override
    protected Logger getLogger() {
        return log;
    }

    private void validateAlarmObject(final HashMap<String, Object> alarmObject) {
        final String notificationType = (String) alarmObject.get(Attributes.NOTIFICATION_TYPE);

        if (NotificationTypeUtil.notificationTypeSupportStatus.containsKey(notificationType)
                && !NotificationTypeUtil.notificationTypeSupportStatus.get(notificationType)) {
            getRecorder().recordError(HANDLER, JMS, OBJECT_MESSAGE, UNSUPPORTED_NOTIFICATION);
            throw new EventHandlerException(UNSUPPORTED_NOTIFICATION);
        } else if (!NotificationTypeUtil.notificationTypeSupportStatus.containsKey(notificationType)) {
            getRecorder().recordError(HANDLER, JMS, OBJECT_MESSAGE, UNKNOWN_NOTIFICATION);
            throw new EventHandlerException(UNKNOWN_NOTIFICATION);
        }
    }

    private String getDnPrefix(final String hrefValue) {
        final String dnPrefix = HrefUtil.extractDnPrefix(hrefValue);

        if (StringUtils.isBlank(dnPrefix)) {

            getRecorder().recordError(HANDLER, JMS, OBJECT_MESSAGE, DN_PREFIX_MISSING);
            throw new EventHandlerException(DN_PREFIX_MISSING);
        }
        return dnPrefix;
    }

    private void checkNotificationRate(final HashMap<String, Object> alarmObject) {

        final String hrefValue = (String) alarmObject.get(HREF);
        final String dnPrefix = getDnPrefix(hrefValue);
        final String networkElementId = getMeContextId(dnPrefix);

        if (alarmService.isNodeSuspended(networkElementId)) {
            handleSuspendedNode(networkElementId);
        } else {
            alarmService.updateNodeSuspenderCache(networkElementId);
            handleNodeSuspendedAfterIncrement(networkElementId);
        }
    }

    private void handleSuspendedNode(final String networkElementId) {
        alarmService.updateNodeSuspenderCache(networkElementId);
        discardAlarmNotification(networkElementId);
    }

    private void handleNodeSuspendedAfterIncrement(final String networkElementId) {

        if (alarmService.isNodeSuspended(networkElementId)) {
            final String networkElementFdn = NETWORK_ELEMENT_EQUALS + networkElementId;
            alarmService.sendAlarm(createNodeSuspendedEvent(networkElementFdn));
            discardAlarmNotification(networkElementId);
        } else {
            processAlarmNotification();
        }
    }

    private EventNotification createNodeSuspendedEvent(final String networkElementFdn) {
        final EventNotification nodeSuspendedEvent =
                EventNotificationUtil.createNodeSuspensedAlarm(networkElementFdn, "", "");
        getRecorder().recordEvent("FM_O1_NODE_SUSPENDER", "", "", "Node Suspended : Alarm raised for " + networkElementFdn);
        return nodeSuspendedEvent;
    }

    private void discardAlarmNotification(final String networkElementId) {
        headers.put("isNodeSuspended", true);
        log.trace("Node is suspended and the notification will not be processed for network element : {}", networkElementId);
    }

    private void processAlarmNotification() {
        headers.put("isNodeSuspended", false);
    }

}
