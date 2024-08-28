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

package com.ericsson.oss.mediation.o1.event.jms;

import java.util.HashMap;

import javax.inject.Inject;

import com.ericsson.oss.itpf.sdk.core.util.ServiceIdentity;
import com.ericsson.oss.itpf.sdk.eventbus.Channel;
import com.ericsson.oss.itpf.sdk.eventbus.ChannelConfiguration;
import com.ericsson.oss.itpf.sdk.eventbus.ChannelConfigurationBuilder;
import com.ericsson.oss.itpf.sdk.eventbus.ChannelLocator;
import com.ericsson.oss.itpf.sdk.eventbus.EventConfigurationBuilder;
import com.ericsson.oss.itpf.sdk.eventbus.annotation.PersistenceType;
import com.ericsson.oss.mediation.o1.event.model.O1Notification;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MessageSender {

    private static final String JMS_QUEUE = "jms:/queue/";
    private static final String O1_FM_NOTIFICATIONS_QUEUE = JMS_QUEUE + System.getProperty("o1_fm_notifications_channelId");
    private static final String O1_FM_HEARTBEAT_QUEUE = JMS_QUEUE + System.getProperty("o1_fm_heartbeat_notifications_channelId");

    private final ChannelConfiguration configuration =
            new ChannelConfigurationBuilder().persistence(PersistenceType.PERSISTENT).priority(4).timeToLive(0)
                    .build();

    @Inject
    private ServiceIdentity serviceIdentity;

    @Inject
    private ChannelLocator channelLocator;

    public final void send(final O1Notification notification) {

        final HashMap<String, Object> normalisedNotification = notification.getNormalisedNotification();
        final String nodeId = serviceIdentity.getNodeId();
        final EventConfigurationBuilder eventConfigurationBuilder = new EventConfigurationBuilder();
        eventConfigurationBuilder.addEventProperty("__target_ms_instance", nodeId);

        final Channel channel;
        if (isHeartbeatNotification(notification)) {
            channel = channelLocator.lookupAndConfigureChannel(O1_FM_HEARTBEAT_QUEUE, configuration);
        } else {
            channel = channelLocator.lookupAndConfigureChannel(O1_FM_NOTIFICATIONS_QUEUE, configuration);
        }

        log.debug("Sending message: [{}] to queue [{}] with nodeId [{}]", normalisedNotification, channel.getChannelURI(),
                nodeId);

        channel.send(normalisedNotification, eventConfigurationBuilder.build());
    }

    private boolean isHeartbeatNotification(final O1Notification notification) {
        final String notificationType = (String) notification.getNormalisedNotification().get("notificationType");
        return notificationType.equals("notifyHeartbeat");
    }
}
