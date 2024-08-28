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

package com.example.ves.tests;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import static javax.jms.Session.AUTO_ACKNOWLEDGE;


import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.api.jms.ActiveMQJMSClient;
import org.apache.activemq.artemis.api.jms.JMSFactoryType;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import static org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants.HOST_PROP_NAME;
import static org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants.PORT_PROP_NAME;
import static org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants.HTTP_UPGRADE_ENABLED_PROP_NAME;
import static org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants.HTTP_UPGRADE_ENDPOINT_PROP_NAME;
import org.junit.After;
import org.junit.Before;

import java.util.HashMap;
import java.util.Map;

public class BaseITClass {

    private Connection connection = null;
    private Session session = null;
    protected MessageConsumer consumer = null;

    @Before
    public void setup() throws JMSException {
        Map<String, Object> connectionParams = new HashMap<String, Object>();
        connectionParams.put(HOST_PROP_NAME, "localhost");
        connectionParams.put(PORT_PROP_NAME, "8080");
        connectionParams.put(HTTP_UPGRADE_ENABLED_PROP_NAME, "true");
        connectionParams.put(HTTP_UPGRADE_ENDPOINT_PROP_NAME, "http-acceptor");

        TransportConfiguration transportConfiguration = new TransportConfiguration("org.apache.activemq.artemis.core.remoting.impl.netty.NettyConnectorFactory", connectionParams);

        ActiveMQConnectionFactory jmsConnnectionFactory = ActiveMQJMSClient.createConnectionFactoryWithoutHA(JMSFactoryType.QUEUE_CF, transportConfiguration);

        connection = jmsConnnectionFactory.createConnection("guest", "guest");
        connection.start();

        session = connection.createSession(false, AUTO_ACKNOWLEDGE);
        final Destination destination = session.createQueue("O1FmNotifications");

        consumer = session.createConsumer(destination);
    }

    @After
    public void cleanup() throws JMSException {
        consumer.close();
        session.close();
        connection.close();
    }
}
