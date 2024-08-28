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

package com.ericsson.oss.mediation.fm.o1.engine.transform.processor;

import org.apache.commons.jelly.JellyContext;

import com.ericsson.oss.mediation.fm.o1.engine.transform.AbstractArgumentableTransformer;
import com.ericsson.oss.mediation.translator.model.EventNotification;

/**
 * Base class for all transformer processors.
 * <p>
 * A processor, similar to a converter, is associated with a particular oid. However, unlike a converter no value is returned and the object model is
 * not automatically updated.
 * <p>
 * Processors must operate directly on the object model using the 'process' method.
 * <p>
 * A processor can be defined in the following way:
 * <p>
 * <t:process-model-property oid="alarmId" processor="AlarmIdProcessor"/>
 * <p>
 * If the alarm data contains a value with oid 'alarmId' then the 'process' method on the 'AlarmIdProcessor' will be invoked.
 */
public abstract class AbstractModelProcessor extends AbstractArgumentableTransformer {

    /**
     * Creates a new processor.
     *
     * @param args
     *            the arguments passed to the processor.
     * @param context
     *            the model transformer context.
     */
    protected AbstractModelProcessor(final String[] args, final JellyContext context) {
        super(args, context);
    }

    /**
     * Invoked by the framework when the associated {@code oid} is found in the alarm data.
     *
     * @param bean
     *            the object containing the alarm data.
     * @param oid
     *            the alarm field which invoked the processor.
     * @param value
     *            the value of the alarm field.
     * @param eventNotification
     *            the object which the alarm data will be transformed to.
     */
    public abstract <T> void process(T bean, String oid, Object value, EventNotification eventNotification);
}
