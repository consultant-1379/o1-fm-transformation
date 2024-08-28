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

package com.ericsson.oss.mediation.fm.o1.model.transformer.processor.impl;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ericsson.oss.mediation.fm.snmp.model.transformer.processor.ModelProcessorException;
import com.ericsson.oss.mediation.translator.model.EventNotification;

@RunWith(value = MockitoJUnitRunner.class)
public class XpathObjectOfReferenceProcessorTest {

    @InjectMocks
    XpathObjectOfReferenceProcessor xpathObjectOfReferenceProcessor = new XpathObjectOfReferenceProcessor(null, null);

    @Mock
    EventNotification eventNotification;

    @Test
    public void testProcessWhenMOInstanceIsInXpathFormat() {
        final String snmpValue = "/epg:epg/sgw/function[name='SGW Session Controller']/board[name='gc-0/3/1']";
        eventNotification = new EventNotification();
        xpathObjectOfReferenceProcessor.process(null, "0.11.01", snmpValue, eventNotification);
        assertEquals(eventNotification.getManagedObjectInstance(), "epg=1,sgw=1,function='SGW Session Controller',board='gc-0/3/1'");
    }

    @Test
    public void testProcessWhenMOInstanceIsInXpathFormatWithEmptyTokens() {
        final String snmpValue = "//epg:epg//sgw////function[name='SGW Session Controller']//board[name='gc-0///3/1']";
        eventNotification = new EventNotification();
        xpathObjectOfReferenceProcessor.process(null, "0.11.01", snmpValue, eventNotification);
        assertEquals("epg=1,sgw=1,function='SGW Session Controller',board='gc-0/3/1'", eventNotification.getManagedObjectInstance());
    }

    @Test
    public void testProcessWhenMOInstanceIsInXpathFormatWithExtraSlashes() {
        final String snmpValue = "//epg:epg//sgw////fun//ction[name='SGW Session Controller']//board[name='gc-0///3/1']";
        eventNotification = new EventNotification();
        xpathObjectOfReferenceProcessor.process(null, "0.11.01", snmpValue, eventNotification);
        assertEquals("epg=1,sgw=1,fun=1,ction='SGW Session Controller',board='gc-0/3/1'", eventNotification.getManagedObjectInstance());
    }

    @Test
    public void testProcessWhenMOInstanceIsInXpathFormatWithExtraSquareBrackets() {
        final String snmpValue =
                "/nrfae:nrf-agent/non_modeled/local[service-name=eric-nrfagent-register]/remote[id=nrf-server-0][service-name=nnrf-nfm]]";
        eventNotification = new EventNotification();
        xpathObjectOfReferenceProcessor.process(null, "0.11.01", snmpValue, eventNotification);
        assertEquals("nrf-agent=1,non_modeled=1,local=eric-nrfagent-register,remote=nrf-server-0,service-name=nnrf-nfm",
                eventNotification.getManagedObjectInstance());
    }

    @Test
    public void testProcessWhenMOInstanceIsInXpathFormatWithExtraMultipleSquareBrackets() {
        final String snmpValue =
                "/nrfae:nrf-agent/non_modeled/local[service-name=eric-nrfagent-register][service-name=eric-nrfagent-register]/remote[id=nrf-server-0][service-name=nnrf-nfm]]";
        eventNotification = new EventNotification();
        xpathObjectOfReferenceProcessor.process(null, "0.11.01", snmpValue, eventNotification);
        assertEquals(
                "nrf-agent=1,non_modeled=1,local=eric-nrfagent-register,service-name=eric-nrfagent-register,remote=nrf-server-0,service-name=nnrf-nfm",
                eventNotification.getManagedObjectInstance());
    }

    @Test
    public void testProcessWhenMOInstanceIsNull() {
        eventNotification = new EventNotification();
        xpathObjectOfReferenceProcessor.process(null, "0.11.01", null, eventNotification);
        assertEquals(eventNotification.getManagedObjectInstance(), "");

    }

    @Test(expected = ModelProcessorException.class)
    public void testProcessWhenMOInstanceIsInteger() {
        eventNotification = new EventNotification();
        xpathObjectOfReferenceProcessor.process(null, null, 1, eventNotification);

    }

}
