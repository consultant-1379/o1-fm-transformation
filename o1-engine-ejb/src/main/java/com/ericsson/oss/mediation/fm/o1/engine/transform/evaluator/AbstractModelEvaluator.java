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

package com.ericsson.oss.mediation.fm.o1.engine.transform.evaluator;

import java.lang.reflect.Field;

import org.apache.commons.jelly.JellyContext;

import com.ericsson.oss.mediation.fm.o1.engine.transform.AbstractArgumentableTransformer;
import com.ericsson.oss.mediation.translator.model.EventNotification;

/**
 * Base class for all model property evaluators.
 * <p>
 * A model property evaluator can be used to set model properties to fixed values (i.e. not dependent on the alarm data).
 * <p>
 * A model property evaluator should be defined in the following way:
 * <p>
 * {@code
 *   <t:evaluate-set-model-property mappedBy="modelField" evaluator="evaluator implementation class"
 *   args="arg1=value1, argN=valueN"/>
 * }
 *
 * @param <T>
 *            The evaluator return type
 */
public abstract class AbstractModelEvaluator<T> extends AbstractArgumentableTransformer {

    /**
     * Creates a new evaluator.
     *
     * @param args
     *            the arguments passed to this evaluator in the form:
     *            arg1=value1, arg2=value2, ..., argN=valueN
     * @param context
     *            the jelly context
     */
    protected AbstractModelEvaluator(final String[] args, final JellyContext context) {
        super(args, context);
    }

    /**
     * The evaluator callback method invoked by the framework.
     * <p>
     * The result of the 'eval' method is set to the field of the model identified by 'mappedBy'.
     *
     * @param dstField
     *            the destination field on the model object to set
     * @param e
     *            the event notification object
     * @return the value to set on the specified field
     */
    public abstract T eval(Field dstField, EventNotification e);
}
