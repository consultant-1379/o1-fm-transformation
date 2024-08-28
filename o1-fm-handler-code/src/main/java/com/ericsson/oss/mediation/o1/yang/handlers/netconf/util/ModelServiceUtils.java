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

package com.ericsson.oss.mediation.o1.yang.handlers.netconf.util;

import static com.ericsson.oss.itpf.modeling.modelservice.typed.core.target.TargetTypeInformation.CATEGORY_NODE;
import static com.ericsson.oss.mediation.cm.handlers.instrumentation.InstrumentationUtil.getNetworkElementId;
import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.Constants.NE_RELEASE_VERSION;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.ericsson.oss.itpf.datalayer.dps.modeling.modelservice.typed.persistence.primarytype.NamingScopeUtil;
import com.ericsson.oss.itpf.datalayer.dps.modeling.modelservice.typed.persistence.primarytype.PrimaryTypeAttributeSpecification;
import com.ericsson.oss.itpf.datalayer.dps.modeling.modelservice.typed.persistence.primarytype.PrimaryTypeSpecification;
import com.ericsson.oss.itpf.modeling.common.info.ModelInfo;
import com.ericsson.oss.itpf.modeling.modelservice.ModelService;
import com.ericsson.oss.itpf.modeling.modelservice.typed.core.EModelSpecification;
import com.ericsson.oss.itpf.modeling.modelservice.typed.core.target.Target;
import com.ericsson.oss.itpf.modeling.modelservice.typed.core.target.TargetTypeInformation;
import com.ericsson.oss.itpf.modeling.modelservice.typed.core.target.TargetTypeVersionInformation;
import com.ericsson.oss.itpf.modeling.schema.util.SchemaConstants;
import com.ericsson.oss.mediation.cm.handlers.datastructure.MoDataObject;
import lombok.extern.slf4j.Slf4j;

/**
 * Model service utility.
 */
@Slf4j
@ApplicationScoped
public class ModelServiceUtils {

    @Inject
    ModelService modelService;

    private Map<String, Map<String, Map<String, String>>> targetModelVersions = new HashMap<>();

    public void initializeMap(final String neType) {

        final Map<String, Map<String, String>> modelIdentitiesMap = targetModelVersions.computeIfAbsent(neType, e -> new HashMap<>());

        final TargetTypeInformation targetTypeInformation = modelService.getTypedAccess().getModelInformation(TargetTypeInformation.class);
        final TargetTypeVersionInformation targetTypeVersionInformation = targetTypeInformation
                .getTargetTypeVersionInformation(TargetTypeInformation.CATEGORY_NODE, neType);

        targetTypeVersionInformation.getTargetModelIdentities().forEach(modelIdentity -> {
            modelIdentitiesMap.computeIfAbsent(modelIdentity, key -> {
                final Map<String, String> mimMap = new HashMap<>();
                targetTypeVersionInformation.getMimsMappedTo(key).forEach(mim -> mimMap.put(mim.getNamespace(), mim.getVersion()));
                return mimMap;
            });
        });
    }

    /**
     * Gets the PrimaryTypeSpecification from model service.
     *
     * @param namespace
     *            The namespace of the model.
     * @param version
     *            The version of the model.
     * @param managedObjectType
     *            The managedObjectType of the model.
     * @return Return a PrimaryTypeSpecification object.
     */
    public PrimaryTypeSpecification getPrimaryTypeSpecification(final String namespace, final String version, final String managedObjectType,
            final String targetType, final String targetName, final String targetIdentity) {
        if (equalsNullOrEmpty(namespace) || equalsNullOrEmpty(version) || equalsNullOrEmpty(managedObjectType)) {
            throw new IllegalArgumentException("Namespace, Version and Type are mandatory to fetch the model information..");
        }
        final Target target = new Target("NODE", targetType, targetName, targetIdentity);
        return modelService.getTypedAccess().getEModelSpecification(
                new ModelInfo(SchemaConstants.DPS_PRIMARYTYPE, namespace, managedObjectType, version), PrimaryTypeSpecification.class, target);
    }

    /**
     * This method checks weather string is null or empty.
     *
     * @param str
     *            string to check
     * @return boolean true if null or empty else false
     */
    public static boolean equalsNullOrEmpty(final String str) {
        return str == null || str.isEmpty();
    }

    /**
     * This method filters the auto generated keys and returns the model keys.
     *
     * @param primaryTypeSpecification
     *            specification of the primary type (list)
     * @return Return a model keys LinkedHashSet.
     */
    public static Set<String> getModelKeys(final PrimaryTypeSpecification primaryTypeSpecification) {
        return primaryTypeSpecification.getKeyAttributeNames().stream()
                .filter(keyAttributeName -> !isAutoGeneratedKey(primaryTypeSpecification, keyAttributeName))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Determines if a key is auto generated or not.
     *
     * @param primaryTypeSpecification
     *            specification of the primary type (list)
     * @param keyAttributeName
     *            key attribute name
     * @return true - if the key is auto generated, false otherwise
     */
    public static boolean isAutoGeneratedKey(final PrimaryTypeSpecification primaryTypeSpecification, final String keyAttributeName) {
        final PrimaryTypeAttributeSpecification ptas = primaryTypeSpecification.getAttributeSpecification(keyAttributeName);
        return ptas != null && isAutoGeneratedKeyAttr(ptas);
    }

    public static String makeKey(final MoDataObject parentMo, final String typeName, final String name) {
        final String moKey = String.format("%s=%s", typeName, name);
        final String parentInfo = parentMo != null ? makeKey(parentMo) + "," : "";
        return parentInfo + moKey;
    }

    public static String makeKey(final MoDataObject moData) {
        final ModelInfo modelInfo = moData.getPrimaryTypeSpecification().getModelInfo();
        final String type = NamingScopeUtil.getUnscopedType(modelInfo.getName());
        return ModelServiceUtils.makeKey(moData.getParentMo(), type, moData.getName());
    }

    public String getModelVersion(final String neType, final String ossIdentity, final String nameSpace) {

        final Map<String, Map<String, String>> modelIdentitiesMap = targetModelVersions.get(neType);
        if (modelIdentitiesMap == null) {
            return null;
        }
        final Map<String, String> mimMap = modelIdentitiesMap.get(ossIdentity);
        if (mimMap != null) {
            return mimMap.get(nameSpace);
        }
        return null;
    }

    /**
     * Check for original yang name.
     *
     * @param primaryTypeSpecification
     *            {@code PrimaryTypeSpecification} object
     * @param type
     *            element name
     * @return true if element name is equal to yang original name or equal to moType name, else false
     */
    public static boolean isOriginalYangNameMatch(final PrimaryTypeSpecification primaryTypeSpecification, final String type) {
        final Map<String, String> metaData = primaryTypeSpecification.getMetaInformation();
        if (metaData != null && metaData.containsKey("YANG_ORIGINAL_NAME")) {
            return metaData.get("YANG_ORIGINAL_NAME").equals(type);
        }
        return primaryTypeSpecification.getModelInfo().getName().equals(type);
    }

    public ModelInfo getModelInfo(final MoDataObject moDataObject) {
        return new ModelInfo(SchemaConstants.DPS_PRIMARYTYPE, moDataObject.getNamespace(), moDataObject.getTypeName(), moDataObject.getVersion());
    }

    public ModelInfo getModelInfo(final String namespace, final String type, final String version) {
        return new ModelInfo(SchemaConstants.DPS_PRIMARYTYPE, namespace, type, version);
    }

    public <T extends EModelSpecification> T getPrimaryTypeSpecification(final ModelInfo modelInfo, final Class<T> typeSpecification,
            final Target target) {
        return modelService.getTypedAccess().getEModelSpecification(modelInfo, typeSpecification, target);
    }


    public static Target getTarget(final String fdn, final String nodeType) {
        final String targetName = getNetworkElementId(fdn);
        log.debug("Creating target for fdn {} with attributes: {} {} {} {}", fdn, CATEGORY_NODE, nodeType, targetName, NE_RELEASE_VERSION);

        return new Target(CATEGORY_NODE, nodeType, targetName, NE_RELEASE_VERSION);
    }

    private static boolean isAutoGeneratedKeyAttr(final PrimaryTypeAttributeSpecification ptas) {
        return ptas.getMetaInformation().containsKey("YANG_ARTIFIAL_KEY");
    }
}
