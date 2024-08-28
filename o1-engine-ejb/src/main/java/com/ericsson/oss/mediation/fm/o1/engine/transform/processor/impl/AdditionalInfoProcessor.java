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

import java.util.Map;

import org.apache.commons.jelly.JellyContext;

import com.ericsson.oss.mediation.fm.o1.engine.transform.processor.AbstractModelProcessor;
import com.ericsson.oss.mediation.translator.model.EventNotification;

/**
 * Processor is invoked by an alarm that contains the 'additionalInformation' field.
 * <p>
 * It takes the entries of 'additionalInformation' and updates 'additionalAttributes' field of the {@code EventNotification} with the same entries.
 * <p>
 * This processor can be used in the following way:
 *
 * <pre>
 * &lt;t:process-model-property  oid="additionalInformation" processor = "AdditionalInfoProcessor" />
 * </pre>
 */
public class AdditionalInfoProcessor extends AbstractModelProcessor {

    public AdditionalInfoProcessor(final String[] args, final JellyContext context) {
        super(args, context);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> void process(final T bean, final String oid, final Object value, final EventNotification eventNotification) {
        if (value != null) {
            for (final Map.Entry<String, String> entry : ((Map<String, String>) value).entrySet()) {
                eventNotification.addAdditionalAttribute(entry.getKey(), entry.getValue());
            }
        }
    }
}
