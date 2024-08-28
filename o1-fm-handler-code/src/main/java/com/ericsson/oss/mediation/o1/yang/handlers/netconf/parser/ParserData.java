/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.oss.mediation.o1.yang.handlers.netconf.parser;

import com.ericsson.oss.itpf.modeling.modelservice.typed.core.cdt.ComplexDataTypeSpecification;
import com.ericsson.oss.mediation.cm.handlers.datastructure.InsertOrderedMap;
import com.ericsson.oss.mediation.cm.handlers.datastructure.MoDataObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * This class is used to store datastructures, attributes and state information during parsing.
 */
public class ParserData {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParserData.class);

    private boolean isCdtList;
    private final ElementStack<MoDataObject> elementStack;
    private final Map<String, String> xmlPrefixToNamespaceMap;
    private final StringBuilder attrValueBuffer = new StringBuilder();
    private ParserState parserState;
    private final InsertOrderedMap<String, MoDataObject> yangMOs;
    private Map<String, Object> cdtMembers;
    private ComplexDataTypeSpecification cdts;

    /**
     * Parses data and stores result.
     *
     * @param elementStack data to parse
     */
    public ParserData(final ElementStack<MoDataObject> elementStack) {
        this.elementStack = elementStack;
        this.isCdtList = false;
        this.parserState = ParserState.UNKNOWN;
        this.xmlPrefixToNamespaceMap = new HashMap<String, String>();
        yangMOs = new InsertOrderedMap<String, MoDataObject>();
    }

    public void addManagedObject(final String name, final MoDataObject moData) {
        if (yangMOs.containsKey(name)) {
            throw new IllegalArgumentException(String.format("ManagedObject with name <%s> already exists..", name));
        }
        if (name != null) {
            LOGGER.trace("ManagedObject fdn <{}>", name);
            moData.setFdn(name);
        }
        yangMOs.put(name, moData);
    }

    public InsertOrderedMap<String, MoDataObject> getYangMOs() {
        return yangMOs;
    }

    /**
     * Returns the ElementStack reference.
     *
     * @return elementStack element stack reference
     */
    public ElementStack<MoDataObject> getElementStack() {
        return elementStack;
    }

    /**
     * Returns the xmlNameSpace.
     *
     * @return xmlNameSpace the xmlNameSpace
     */
    public String getXmlNameSpace(final String prefix) {
        return xmlPrefixToNamespaceMap.get(prefix);
    }

    /**
     * Sets the xmlNameSpace.
     *
     * @param xmlNameSpace the xmlNameSpace
     */
    public void addXmlNameSpace(final String prefix, final String xmlNameSpace) {
        this.xmlPrefixToNamespaceMap.put(prefix, xmlNameSpace);
    }

    /**
     * Returns the temporary attribute value.
     *
     * @return attrValue the vallue of attribute
     */
    public String getAttrValue() {
        return attrValueBuffer.toString();
    }

    /**
     * Appends the attribute value with the value passed.
     *
     * @param value the value to append
     */
    public void appendAttrValue(final String value) {
        attrValueBuffer.append(value);
    }

    /**
     * Resets the attribute value.
     */
    public void resetAttrValue() {
        attrValueBuffer.setLength(0);
    }

    /**
     * Returns the parser state.
     *
     * @return parserState the parser state
     */
    public ParserState getParserState() {
        return parserState;
    }

    /**
     * Sets the parser state.
     *
     * @param parserState the parser state
     */
    public void setParserState(final ParserState parserState) {
        this.parserState = parserState;
    }

    /**
     * Gets the cdts model.
     *
     * @param parserState ComplexDataTypeSpecification
     */
    public ComplexDataTypeSpecification getCdts() {
        return cdts;
    }

    /**
     * Sets the cdts model.
     *
     * @param setCdts ComplexDataTypeSpecification
     */
    public void setCdts(final ComplexDataTypeSpecification cdts) {
        this.cdts = cdts;
    }

    /**
     * Gets the cdt members.
     *
     * @return cdtMembers the map of cdt members
     */
    public Map<String, Object> getCdtMembers() {
        return cdtMembers;
    }

    /**
     * Sets the cdt members.
     *
     * @param cdtMembers the map of cdt members
     */
    public void setCdtMembers(final Map<String, Object> cdtMembers) {
        this.cdtMembers = cdtMembers;
    }

    /**
     * Gets the cdt is list or not
     *
     * @return isCdtList boolean value
     */
    public boolean getIsCdtList() {
        return isCdtList;
    }

    /**
     * Sets the cdt list or not.
     *
     * @param isCdtList boolean value
     */
    public void setIsCdtList(final boolean isCdtList) {
        this.isCdtList = isCdtList;
    }

}
