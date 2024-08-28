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

import org.apache.commons.jelly.JellyContext;

import com.ericsson.oss.mediation.fm.o1.common.util.FdnUtil;
import com.ericsson.oss.mediation.fm.o1.common.util.HrefUtil;
import com.ericsson.oss.mediation.fm.o1.engine.transform.processor.AbstractModelProcessor;
import com.ericsson.oss.mediation.translator.model.EventNotification;

/**
 * Processor is invoked by an alarm that contains the 'href' field.
 * <p>
 * It extracts the dnPrefix from the value of the 'href' and sets the 'additionalAttributes.fdn' field of the {@code EventNotification} to the
 * dnPrefix.
 * <p>
 * This processor can be used in the following way:
 *
 * <pre>
 * &lt;t:process-model-property  oid="href" processor = "HrefProcessor" />
 * </pre>
 */
public class HrefProcessor extends AbstractModelProcessor {

    public HrefProcessor(final String[] args, final JellyContext context) {
        super(args, context);
    }

    @Override
    public <T> void process(final T bean, final String oid, final Object value, final EventNotification eventNotification) {
        final String dnPrefix = HrefUtil.extractDnPrefix((String) value);
        final String networkElementFdn = FdnUtil.getNetworkElementFdn(dnPrefix);

        eventNotification.addAdditionalAttribute("fdn", networkElementFdn);
    }
}
