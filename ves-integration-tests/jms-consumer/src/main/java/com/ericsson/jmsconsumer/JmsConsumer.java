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

package com.ericsson.jmsconsumer;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.TextMessage;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Slf4j
public class JmsConsumer {

    private static final Logger log = LoggerFactory.getLogger(JMSConsumer.class);
    private Context jndiContext = null;
    private JMSContext jmsContext = null;
    private static Properties properties = null;

    public static void main(String[] args) {
        final JmsConsumer consumer = new JmsConsumer();

        properties = new Properties();

        try(InputStream InputStream = new FileInputStream("consumer.properties")){
            properties.load(InputStream);
        } catch (IOException e) {
            log.error("Issue with loading properties file : "+e);
        }

        consumer.receiveMessages();
    }

    private void receiveMessages() {
        log.info("Press q to exit...");
        try {

            jndiContext = new InitialContext(properties);

            final ConnectionFactory cf = (ConnectionFactory) jndiContext.lookup(properties.getProperty("connection.factory"));
            log.info("Found Connection Factory...");
            final Destination queue = (Destination) jndiContext.lookup(properties.getProperty("queue.name"));
            log.info("Found Queue...");

            jmsContext = cf.createContext(properties.getProperty("wildfly.user"), properties.getProperty("wildfly.pass"));
            log.info("Created jms context...");
            TextMessage message = null;
            try(final JMSConsumer consumer = jmsContext.createConsumer(queue)) {
                log.info("Connected to queue...");
                while (System.in.read() != 'q') {
                    message = (TextMessage) consumer.receive(1000);
                    if (message != null) {
                        log.info("Received message: '" + message.getText() + "'");
                    }
                }
            }

        } catch (Exception e) {
            log.error("Issue when connecting to queue to recieve messages"+e);
        } finally {
            if (jmsContext != null) {
                jmsContext.close();
            }

            if (jndiContext != null) {
                try {
                    jndiContext.close();

                } catch (NamingException e) {
                   log.error("Issue when closing jndiContext : "+e);
                }
            }
        }
    }

}
