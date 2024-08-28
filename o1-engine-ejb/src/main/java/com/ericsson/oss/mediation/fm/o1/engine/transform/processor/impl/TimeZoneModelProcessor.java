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

package com.ericsson.oss.mediation.fm.o1.engine.transform.processor.impl;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Calendar;
import java.util.TimeZone;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.jelly.JellyContext;

import com.ericsson.oss.mediation.fm.o1.engine.transform.processor.AbstractModelProcessor;
import com.ericsson.oss.mediation.translator.model.EventNotification;

import lombok.extern.slf4j.Slf4j;

/**
 * Processor is invoked by an alarm that contains the 'eventTime' field.
 * <p>
 * If the eventTime contains the UTC offset it shall be used also to populate the EventNotification::timeZone
 * <p>
 * 
 * <pre>
 * &lt;t:process-model-property  oid="timeZone"
 *                               processor = "TimeZoneModelProcessor" />
 * </pre>
 */
@Slf4j
public class TimeZoneModelProcessor extends AbstractModelProcessor {

    public TimeZoneModelProcessor(final String[] args, final JellyContext context) {
        super(args, context);
    }

    @Override
    public <T> void process(final T bean, final String oid, final Object value, final EventNotification eventNotification) {

        final String eventTime = (value instanceof String) ? value.toString() : null;

        if (eventTime == null) {
            return;
        }

        try {
            if (isUTCOffsetPresent(eventTime)) {
                final Calendar calendar = DatatypeConverter.parseDateTime(eventTime);
                final TimeZone timeZone = calendar.getTimeZone();

                eventNotification.setTimeZone(timeZone.getDisplayName());
            }
        } catch (final Exception ex) {
            log.error("Exception occurred while parsing the event for timeZone data. Exception:{}, eventTime Value:{}", ex.getMessage(), eventTime);
        }
    }

    private boolean isUTCOffsetPresent(final String eventTime) {

        final OffsetDateTime offsetDateTime = OffsetDateTime.parse(eventTime);
        final ZoneOffset zoneOffset = offsetDateTime.getOffset();
        return !zoneOffset.toString().isEmpty();
    }
}
