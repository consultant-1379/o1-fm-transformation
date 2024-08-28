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

package com.ericsson.oss.mediation.o1.yang.handlers.netconf.api;

import com.ericsson.oss.mediation.engine.api.MediationEngineConstants;

import java.util.Collection;
import java.util.Map;

/**
 * Contains all information required to make the netconf call. Some obtained from flow header.
 */
public class HeaderInfo implements MediationEngineConstants {
    private String action;
    private String fdn;
    private String nameSpace;
    private String type;
    private String version;
    private Map<String, Object> actionAttributes;
    private Map<String, Object> originalAttributes;
    private Map<String, Object> createAttributes;
    private Map<String, Object> headers;
    private Map<String, Object> modifyAttributes;
    private Collection<String> readAttributes;
    private String name;
    private String operation;
    private String parentFdn;
    private String nodeNamespace;
    private String nodeName;
    private boolean includeNsPrefix;

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getFdn() {
        return fdn;
    }

    public void setFdn(String fdn) {
        this.fdn = fdn;
    }

    public String getNameSpace() {
        return nameSpace;
    }

    public void setNameSpace(String nameSpace) {
        this.nameSpace = nameSpace;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Map<String, Object> getActionAttributes() {
        return actionAttributes;
    }

    public void setActionAttributes(Map<String, Object> actionAttributes) {
        this.actionAttributes = actionAttributes;
    }

    public Map<String, Object> getOriginalAttributes() {
        return originalAttributes;
    }

    public void setOriginalAttributes(Map<String, Object> originalAttributes) {
        this.originalAttributes = originalAttributes;
    }

    public Map<String, Object> getCreateAttributes() {
        return createAttributes;
    }

    public void setCreateAttributes(Map<String, Object> createAttributes) {
        this.createAttributes = createAttributes;
    }

    public Map<String, Object> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, Object> headers) {
        this.headers = headers;
    }

    public Map<String, Object> getModifyAttributes() {
        return modifyAttributes;
    }

    public void setModifyAttributes(Map<String, Object> modifyAttributes) {
        this.modifyAttributes = modifyAttributes;
    }

    public Collection<String> getReadAttributes() {
        return readAttributes;
    }

    public void setReadAttributes(Collection<String> readAttributes) {
        this.readAttributes = readAttributes;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public String getParentFdn() {
        return parentFdn;
    }

    public void setParentFdn(String parentFdn) {
        this.parentFdn = parentFdn;
    }

    public String getNodeNamespace() {
        return nodeNamespace;
    }

    public void setNodeNamespace(String nodeNamespace) {
        this.nodeNamespace = nodeNamespace;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public boolean getIncludeNsPrefix() {
        return includeNsPrefix;
    }

    public void setIncludeNsPrefix(boolean includeNsPrefix) {
        this.includeNsPrefix = includeNsPrefix;
    }
}
