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

import java.util.Map;

import org.apache.commons.jelly.TagLibrary;

public class ModelTransformerTagLibrary extends TagLibrary {

    public static final String NAMESPACE_URI = "jelly:enm";
    public static final String MODEL_TRANSFORMER_TAG_NAME = "model-transformer";
    public static final Class MODEL_TRANSFORMER_TAG_CLASS = ModelTransformerTag.class;
    public static final String SET_MODEL_PROPERTY_TAG_NAME = "set-model-property";
    public static final Class SET_MODEL_PROPERTY_TAG_CLASS = SetModelPropertyTag.class;
    public static final String SET_MODEL_ATTRIBUTE_TAG_NAME = "set-model-attribute";
    public static final Class SET_MODEL_ATTRIBUTE_TAG_CLASS = SetModelAttributeTag.class;
    public static final String CONVERT_SET_MODEL_PROPERTY_TAG_NAME = "convert-set-model-property";
    public static final Class CONVERT_SET_MODEL_PROPERTY_TAG_CLASS = ConvertSetModelPropertyTag.class;
    public static final String EVALUATE_SET_MODEL_PROPERTY_TAG_NAME = "evaluate-set-model-property";
    public static final Class EVALUATE_SET_MODEL_PROPERTY_TAG_CLASS = EvaluateSetModelPropertyTag.class;
    public static final String PROCESS_SET_MODEL_PROPERTY_TAG_NAME = "process-model-property";
    public static final Class PROCESS_SET_MODEL_PROPERTY_TAG_CLASS = ProcessSetModelPropertyTag.class;

    /**
     * Creates a model transformer tag library.
     */
    public ModelTransformerTagLibrary() {
        registerTag(MODEL_TRANSFORMER_TAG_NAME, MODEL_TRANSFORMER_TAG_CLASS);
        registerTag(SET_MODEL_PROPERTY_TAG_NAME, SET_MODEL_PROPERTY_TAG_CLASS);
        registerTag(SET_MODEL_ATTRIBUTE_TAG_NAME, SET_MODEL_ATTRIBUTE_TAG_CLASS);
        registerTag(CONVERT_SET_MODEL_PROPERTY_TAG_NAME, CONVERT_SET_MODEL_PROPERTY_TAG_CLASS);
        registerTag(EVALUATE_SET_MODEL_PROPERTY_TAG_NAME, EVALUATE_SET_MODEL_PROPERTY_TAG_CLASS);
        registerTag(PROCESS_SET_MODEL_PROPERTY_TAG_NAME, PROCESS_SET_MODEL_PROPERTY_TAG_CLASS);
    }

    @Override
    public Map getTagClasses() {
        return super.getTagClasses();
    }
}
