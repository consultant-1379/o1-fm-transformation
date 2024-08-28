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

package com.ericsson.oss.mediation.fm.o1.engine.transform;

public class Constants {

    public static final String DEFINE_MODEL_TRANSFORMER_TAG = "t:define-model-transformer";
    public static final String DST_OBJECT = "dstObject";
    public static final String ENUMS_ELEMENT_XPATH = "model-transformer-config/enums";
    public static final String ENUMS_MAP = "enumsMap";
    public static final String FILE_ATTRIBUTE = "file";
    public static final String ID_ATTRIBUTE = "id";
    public static final String IMPORT_TRANSFORMER_CONFIG_TAG = "t:import-transformer-config";
    public static final String INCLUDE_MODEL_TRANSFORMER_TAG = "t:include-model-transformer";
    public static final String MODEL_TRANSFORMER_ELEMENT_XPATH = "model-transformer-config/model-transformers/model-transformer";
    public static final String MODEL_TRANSFORMER_IMPORTS_ELEMENT_XPATH = "model-transformer-config/model-transformer-imports";
    public static final String MODEL_TRANSFORMERS_ELEMENT_XPATH = "model-transformer-config/model-transformers";
    public static final String NAME_ATTRIBUTE = "name";
    public static final String REGISTERED_CONVERTERS = "registeredConverters";
    public static final String REGISTERED_EVALUATORS = "registeredEvaluators";
    public static final String REGISTERED_PROCESSORS = "registeredProcessors";
    public static final String SRC_OBJECT = "srcObject";
    public static final String SRC_VALUES_MAP = "srcValuesMap";
    public static final String TAG_LIBRARIES_ELEMENT_XPATH = "model-transformer-config/register-tag-libraries";
    public static final String TRANSFORMER_ATTRIBUTE = "transformer";
    public static final String TRANSFORMER_CONFIGURATION_FILE_URL = "transformerConfigUrl";
    public static final String URI_ATTRIBUTE = "uri";

    private Constants() {}
}
