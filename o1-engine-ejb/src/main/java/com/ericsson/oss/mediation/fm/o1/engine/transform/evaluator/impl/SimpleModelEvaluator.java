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

package com.ericsson.oss.mediation.fm.o1.engine.transform.evaluator.impl;

import java.lang.reflect.Field;

import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.jelly.JellyContext;

import com.ericsson.oss.mediation.fm.o1.engine.transform.evaluator.AbstractModelEvaluator;
import com.ericsson.oss.mediation.translator.model.EventNotification;

/**
 * A simple evaluator implementation that assigns a specified value to a specified field in the xml file.
 * <p>
 * Here is an example of how the evaluator should be used:
 * <p>
 * {@code
 *   <t:evaluate-set-model-property mappedBy="recordType" evaluator="SimpleModelEvaluator"
 *   args="value=ALARM" />
 * }
 * <p>
 * This would set the alarm field 'recordType' to the value 'ALARM'
 */
public class SimpleModelEvaluator extends AbstractModelEvaluator<Object> {

    private static final String VALUE_ARG = "value";

    /**
     * Create a new SimplePropertyEvaluator.
     *
     * @param args
     *            the arguments passed to this evaluator.
     * @param context
     *            the model transformer context.
     */
    public SimpleModelEvaluator(final String[] args, final JellyContext context) {
        super(args, context);
    }

    /**
     * Evaluates the value to assign to the specified model property
     *
     * @param dstField
     *            the model field to set.
     * @param eventNotification
     *            the event notification object
     * @return the value to assign to model field.
     */
    @Override
    public Object eval(final Field dstField, final EventNotification eventNotification) {
        final String value = getRequiredArg(VALUE_ARG);
        return ConvertUtils.convert(value, dstField.getType());
    }
}
