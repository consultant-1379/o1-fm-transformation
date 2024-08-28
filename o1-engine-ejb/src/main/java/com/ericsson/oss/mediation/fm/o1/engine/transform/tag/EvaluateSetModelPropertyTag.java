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

package com.ericsson.oss.mediation.fm.o1.engine.transform.tag;

import java.lang.reflect.Field;
import java.util.Map;

import org.apache.commons.jelly.JellyTagException;
import org.apache.commons.jelly.MissingAttributeException;

import com.ericsson.oss.mediation.fm.o1.engine.transform.Constants;
import com.ericsson.oss.mediation.fm.o1.engine.transform.evaluator.AbstractModelEvaluator;
import com.ericsson.oss.mediation.fm.o1.engine.transform.util.TransformerUtils;

/**
 * TAG class created for the XML tag {@code ModelTransformerTagLibrary#EVALUATE_SET_MODEL_PROPERTY_TAG_NAME}.
 * <p>
 * See {@code SimpleModelEvaluator} for example of evaluator that can be used with this tag.
 */
public class EvaluateSetModelPropertyTag extends AbstractArgumentableTag {

    private String evaluator;

    @Override
    public void doTag() throws JellyTagException {
        if (mappedBy == null) {
            logger.error("Attribute 'mappedBy' not set");
            throw new MissingAttributeException("mappedBy");
        }
        if (evaluator == null) {
            logger.error("Attribute 'evaluator' not set");
            throw new MissingAttributeException("evaluator");
        }
        final Map<String, String> registeredEvaluators = (Map<String, String>) context.getVariable(Constants.REGISTERED_EVALUATORS);
        if (registeredEvaluators.containsKey(evaluator)) {
            evaluator = registeredEvaluators.get(evaluator);
        }
        try {
            final AbstractModelEvaluator propertyEvaluator = TransformerUtils.createInstance(evaluator, getArguments(),
                    context.getClassLoader(), context);
            final Map<String, Field> fieldMap = TransformerUtils.getFieldMap(eventNotification.getClass());
            final Field dstField = fieldMap.get(mappedBy);
            final Object evaluated = propertyEvaluator.eval(dstField, eventNotification);
            setModelProperty(evaluated);
        } catch (final Exception ex) {
            throw new JellyTagException("Error executing EvaluateSetModelPropertyTag. Evaluator is: "
                    + evaluator + " Oid is: " + oid + ", mappedBy is: " + mappedBy, ex);
        }
    }

    public void setEvaluator(final String evaluator) {
        this.evaluator = evaluator;
    }
}
