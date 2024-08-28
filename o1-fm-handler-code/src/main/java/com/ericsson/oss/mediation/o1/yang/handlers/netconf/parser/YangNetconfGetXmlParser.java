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

package com.ericsson.oss.mediation.o1.yang.handlers.netconf.parser;

import static com.ericsson.oss.itpf.modeling.modelservice.typed.core.target.TargetTypeInformation.CATEGORY_NODE;
import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.Constants.NE_RELEASE_VERSION;
import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.parser.ParserConstants.DATA;
import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.parser.ParserConstants.NETOPEER;
import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.parser.ParserConstants.SINGLETON_MANAGEDOBJECTNAME;
import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.parser.ParserConstants.XML_NAMESPACE_ATTR;
import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.parser.ParserConstants.XML_NAMESPACE_PREFIX_SEPARATOR;
import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.parser.ParserConstants.appendDpsList;
import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.parser.ParserConstants.parseNumber;
import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.parser.ParserConstants.removeNodeAppInstanceName;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.ericsson.oss.itpf.datalayer.dps.modeling.modelservice.typed.persistence.primarytype.HierarchicalPrimaryTypeSpecification;
import com.ericsson.oss.itpf.datalayer.dps.modeling.modelservice.typed.persistence.primarytype.PrimaryTypeAttributeSpecification;
import com.ericsson.oss.itpf.datalayer.dps.modeling.modelservice.typed.persistence.primarytype.PrimaryTypeSpecification;
import com.ericsson.oss.itpf.modeling.common.info.ModelInfo;
import com.ericsson.oss.itpf.modeling.modelservice.ModelService;
import com.ericsson.oss.itpf.modeling.modelservice.exception.ConstraintViolationException;
import com.ericsson.oss.itpf.modeling.modelservice.exception.UnknownModelException;
import com.ericsson.oss.itpf.modeling.modelservice.typed.TypedModelAccess;
import com.ericsson.oss.itpf.modeling.modelservice.typed.core.DataType;
import com.ericsson.oss.itpf.modeling.modelservice.typed.core.DataTypeSpecification;
import com.ericsson.oss.itpf.modeling.modelservice.typed.core.cdt.ComplexDataTypeAttributeSpecification;
import com.ericsson.oss.itpf.modeling.modelservice.typed.core.cdt.ComplexDataTypeSpecification;
import com.ericsson.oss.itpf.modeling.modelservice.typed.core.edt.EnumDataTypeMemberSpecification;
import com.ericsson.oss.itpf.modeling.modelservice.typed.core.edt.EnumDataTypeSpecification;
import com.ericsson.oss.itpf.modeling.modelservice.typed.core.target.Target;
import com.ericsson.oss.mediation.cm.handlers.datastructure.ManagedObjectFlag;
import com.ericsson.oss.mediation.cm.handlers.datastructure.MoDataObject;
import com.ericsson.oss.mediation.o1.yang.handlers.netconf.exception.MOHandlerException;
import com.ericsson.oss.mediation.o1.yang.handlers.netconf.util.ModelServiceUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * This is a SAX based parser which parses the netconf get-config XML response from the node.
 * <p>
 * Uses {@code ModelServiceUtils} to get the EModels and validate the MOs and attributes in the response.
 */
@Slf4j
public class YangNetconfGetXmlParser extends DefaultHandler {

    private static final SAXParserFactory FACTORY = SAXParserFactory.newInstance();

    @Inject
    private ModelService modelService;
    @Inject
    private ModelServiceUtils modelServiceUtils;

    private ParserData parserData;

    private boolean skipAllElements;

    private String managedElementFdn;

    private Map<String, String> additionalInfoMap;

    private int elementSkipCount;

    private String neType;

    private String neName;

    /**
     * This method does the initialization required for YangNetconfGetXmlParser.
     */
    public void init(final String managedElementFdn, final String neType) {
        final ElementStack<MoDataObject> elementStack = new ElementStack<>();
        parserData = new ParserData(elementStack);
        this.managedElementFdn = managedElementFdn;
        this.neType = neType;
        modelServiceUtils.initializeMap(neType);
    }

    /**
     * Parses the XML string.
     *
     * @param xmlString
     *            string to parse
     * @throws ParserConfigurationException
     *             parser exception
     * @throws SAXException
     *             sax parser exeption
     * @throws IOException
     *             io exception
     */
    public ParserData parseXmlString(final String xmlString) throws ParserConfigurationException, SAXException, IOException {
        final String updatedXmlString = "<data>" + xmlString + "</data>";

        log.trace(updatedXmlString);

        final SAXParser saxParser = FACTORY.newSAXParser();
        saxParser.parse(new InputSource(new StringReader(updatedXmlString)), this);
        return parserData;
    }

    /**
     * Takes specific actions at the start of each element.
     *
     * @param uri
     *            uri of element
     * @param localName
     *            local name of element
     * @param qName
     *            qName of element
     * @param attributes
     *            attribute of element
     */
    @Override
    public void startElement(final String uri, final String localName, final String qName, final Attributes attributes) {
        log.trace("Start element : {}", qName);
        String namespace = null;

        /* Ignoring the root element that was added as a workaround */
        if (qName.equals(DATA)) {
            return;
        }

        checkSkipAllElements();

        /* If the element is a child of an unsupported element, skipping it */
        if (skipAllElements) {
            ++elementSkipCount;
            return;
        }

        additionalInfoMap = new HashMap<>();
        /* Expected format: <prefix:elementName ..attributes..> */

        /* Namespace attributes are only considered */
        if (attributes != null) {
            namespace = filterNameSpaceAttribute(attributes, namespace);
        }

        final String name = equalsNullOrEmpty(qName) ? qName : removeNodeAppInstanceName(qName);

        if (!equalsNullOrEmpty(namespace)) {
            namespace = removeNodeAppInstanceName(namespace);
        }

        final String elementName = getElementName(qName, qName.contains(XML_NAMESPACE_PREFIX_SEPARATOR),
                qName.split(XML_NAMESPACE_PREFIX_SEPARATOR));
        log.debug("elementName is {}", elementName);

        // Check if the MO is "attributes" and skip if it of YANG_3GPP_REAGGREGATED_IOC
        if (elementName.equalsIgnoreCase("Attributes")) {
            final MoDataObject stackTop = parserData.getElementStack().peek(); // parent MO
            final PrimaryTypeSpecification primaryTypeSpecification = stackTop.getPrimaryTypeSpecification();
            final Map<String, String> metaInformationMap = primaryTypeSpecification.getMetaInformation();
            if (metaInformationMap.containsKey("YANG_3GPP_REAGGREGATED_IOC")) {
                return;
            }
        }

        /*
         * If the stack is empty, then it is the first element in the xml and a ManagedObject.
         */
        if (parserData.getElementStack().isEmpty()) {
            processMo(name, null, namespace);
        } else {
            /* If the stack is not empty, then the object is a child MO */
            processChild(name, namespace);
        }
    }

    private void checkSkipAllElements() {
        if (elementSkipCount > 0) {
            skipAllElements = true;
        }
    }

    private String filterNameSpaceAttribute(final Attributes attributes, String namespace) {
        for (int index = 0; index < attributes.getLength(); index++) {
            /* Expected name space format : xmlns:prefix="namespace" */
            final String attributeName = attributes.getQName(index); // xmlns:prefix
            namespace = attributes.getValue(index); // namespace
            if (attributeName.contains(XML_NAMESPACE_ATTR)
                    && attributeName.contains(XML_NAMESPACE_PREFIX_SEPARATOR)) {
                final String namespacePrefix = attributeName.split(XML_NAMESPACE_PREFIX_SEPARATOR)[1];
                parserData.addXmlNameSpace(namespacePrefix, namespace);
            } else {
                additionalInfoMap.put(attributeName, namespace);
            }
        }
        return namespace;
    }

    private String getElementName(final String qName, final boolean contains, final String[] split) {
        String elementName = qName;
        if (contains) {
            elementName = split[1];
        }
        return elementName;
    }

    /**
     * Returns if the EModel of the specific MO is supported or not.
     *
     * @param moDataObject
     *            MoDataObject that represents the MO
     * @return true if the EModel is supported, false otherwise
     */
    private boolean isEmodelSupported(final MoDataObject moDataObject) {
        return moDataObject.getTypeName() != null;
    }

    private void processChild(final String qName, final String namespace) {
        // Get current MO in stack
        final MoDataObject stackTop = parserData.getElementStack().peek();

        if (!isEmodelSupported(stackTop)) {
            processMo(qName, stackTop, namespace);
            return;
        }

        final PrimaryTypeSpecification primaryTypeSpecification = stackTop.getPrimaryTypeSpecification();

        /* Expected element format : prefix:elementName */
        String memberName = qName;
        if (qName.contains(XML_NAMESPACE_PREFIX_SEPARATOR)) {
            memberName = qName.split(XML_NAMESPACE_PREFIX_SEPARATOR)[1];
        }

        Collection<String> memNames;
        final ComplexDataTypeSpecification cdts = parserData.getCdts();
        if (cdts != null) {
            if (ParserState.CDT_ATTRIBUTE == parserData.getParserState()) {
                memNames = cdts.getMemberNames();
            } else {
                skipAllElements();
                return;
            }
        } else {
            memNames = primaryTypeSpecification.getMemberNames();
        }

        if (!memNames.contains(memberName)) {
            processMo(qName, stackTop, namespace);
            return;
        }
        final DataType dataType;
        DataType valueDataType = null;
        DataTypeSpecification dataTypeSpec;
        PrimaryTypeAttributeSpecification primaryAttributeSpecification = null;
        if (ParserState.CDT_ATTRIBUTE == parserData.getParserState()) {
            dataTypeSpec = parserData.getCdts().getAttributeSpecification(memberName).getDataTypeSpecification();
            dataType = dataTypeSpec.getDataType();
            log.debug("complex datatype member {}", memberName);
        } else {
            primaryAttributeSpecification = primaryTypeSpecification.getAttributeSpecification(memberName);
            dataTypeSpec = primaryAttributeSpecification.getDataTypeSpecification();
            dataType = dataTypeSpec.getDataType();
            valueDataType = primaryAttributeSpecification.getDataTypeSpecification()
                    .getValuesDataTypeSpecification() != null
                            ? primaryAttributeSpecification.getDataTypeSpecification().getValuesDataTypeSpecification().getDataType()
                            : null;
        }

        if (primaryAttributeSpecification != null || cdts != null) {
            setAttrParserDataType(memberName, dataType, valueDataType, dataTypeSpec);
            return;
        }

        // Check whether it's one of the child type
        processMo(qName, stackTop, namespace);

    }

    private void setAttrParserDataType(final String memberName, final DataType dataType, DataType valueDataType,
            final DataTypeSpecification dataTypeSpec) {
        switch (dataType) {
            case BYTE:
            case INTEGER:
            case LONG:
            case SHORT:
            case DOUBLE:
            case BOOLEAN:
            case STRING:
            case UNION:
            case ENUM_REF:
            case IP_ADDRESS:
            case BITS:
            case COMPLEX_REF:
                log.trace("Identified <{}> as an attribute of data type <{}>", memberName, dataType);
                valueDataType = dataType;
                setAttrParserState(memberName, dataType, valueDataType, dataTypeSpec);
                return;
            case LIST:
                log.trace("Identified <{}> as an attribute of data type <{}>", memberName, dataType);
                setAttrParserState(memberName, dataType, valueDataType, dataTypeSpec);
                return;
            default:
                log.warn("Identified <{}> as an unsupported attribute type <{}>", memberName, dataType);
        }
    }

    private void setAttrParserState(final String memberName, final DataType dataType, final DataType valueDataType,
            final DataTypeSpecification dataTypeSpec) {
        if (!parserData.getParserState().equals(ParserState.CDT_ATTRIBUTE) && DataType.COMPLEX_REF.equals(valueDataType)) {
            final MoDataObject localStackTop = parserData.getElementStack().peek();
            final Map<String, String> addMap = new HashMap<>();
            addMap.putAll(localStackTop.getAdditionalInfoMap());

            log.debug("complexmember Identified {} as an attribute of data type {}", memberName, dataType);
            parserData.setParserState(ParserState.CDT_ATTRIBUTE);
            ModelInfo complexModelInfo;
            if (DataType.LIST.equals(dataTypeSpec.getDataType())) {
                complexModelInfo = dataTypeSpec.getValuesDataTypeSpecification().getReferencedDataType();
                parserData.setIsCdtList(true);
            } else {
                complexModelInfo = dataTypeSpec.getReferencedDataType();
            }

            neName = managedElementFdn != null ? managedElementFdn.replaceAll("(.*)" + "ManagedElement=", "").replaceAll(",(.*)", "") : null;
            final Target target = new Target(CATEGORY_NODE, neType, neName, NE_RELEASE_VERSION);
            ComplexDataTypeSpecification cdtSpecification;

            try {
                cdtSpecification = modelService.getTypedAccess().getEModelSpecification(complexModelInfo,
                        ComplexDataTypeSpecification.class, target);

            } catch (final UnknownModelException e) {
                log.error("Model not found for memberName {} and message {}", memberName, e.getMessage());
                skipAllElements();
                return;
            }
            parserData.setCdtMembers(new HashMap<>());
            parserData.setCdts(cdtSpecification);
        } else {
            if (parserData.getCdts() != null) {
                parserData.setParserState(ParserState.CDT_MEMBER);
            } else if (DataType.BITS == dataTypeSpec.getDataType()) {
                parserData.setParserState(ParserState.BITS_ATTRIBUTE);
            } else {
                parserData.setParserState(ParserState.ATTRIBUTE);
            }
            log.debug("memberName {} parserState {}", memberName, parserData.getParserState());
        }
    }

    private void skipAllElements() {
        skipAllElements = true;
        ++elementSkipCount;
    }

    /**
     * Takes actions for each chunk of character data.
     *
     * @param charArray
     *            array of characters
     * @param start
     *            start of array
     * @param length
     *            length of array
     * @throws SAXException
     *             sax parser exception
     */
    @Override
    public void characters(final char[] charArray, final int start, final int length) throws SAXException {

        /* If the element is a child of netopeer element, skipping it */
        if (skipAllElements) {
            return;
        }

        // For simple attributes, set attributeValue in parserData
        final ParserState status = parserData.getParserState();
        if (status == ParserState.ATTRIBUTE || status == ParserState.CDT_MEMBER) {
            final String str = new String(charArray, start, length);
            if (str.length() > 0) {
                parserData.appendAttrValue(str);
                log.trace("******** characters : {}, attrValue : {}", str, parserData.getAttrValue());
            }
        }
    }

    /**
     * Takes actions for each chunk of character data.
     *
     * @param uri
     *            uri of element
     * @param localName
     *            local name of element
     * @param qName
     *            qName of element
     */
    @Override
    public void endElement(final String uri, final String localName, final String qName) {
        log.trace("End element : {}", qName);

        /* Ignoring the root element that was added as a workaround */
        if (DATA.equals(qName)) {
            return;
        }

        /*
         * Data outside of netopeer element shall be processed. So resetting the flag
         */
        if (NETOPEER.equals(qName)) {
            log.trace("netopeer element ended..");
            --elementSkipCount;
            skipAllElements = false;
            return;
        }

        checkSkipAllElements();

        /* If the element is a child of an unsupported element, skipping it */
        if (skipAllElements) {
            --elementSkipCount;
            if (elementSkipCount == 0) {
                skipAllElements = false;
            }
            return;
        }

        String elementName = removeNodeAppInstanceName(qName);
        /* Expected format: </prefix:elementName> */
        if (elementName.contains(XML_NAMESPACE_PREFIX_SEPARATOR)) {
            elementName = elementName.split(XML_NAMESPACE_PREFIX_SEPARATOR)[1];
        }

        log.debug("EndElement parserState {}", parserData.getParserState());

        if (elementName.equalsIgnoreCase("Attributes")) {
            final MoDataObject stackTop = parserData.getElementStack().peek(); // parent MO
            final Map<String, String> metaInformationMap = stackTop.getPrimaryTypeSpecification().getMetaInformation();
            if (metaInformationMap.containsKey("YANG_3GPP_REAGGREGATED_IOC")) {
                return;
            }
        }

        parserStateEndElement(elementName);
    }

    private void parserStateEndElement(final String elementName) {
        switch (parserData.getParserState()) {
            case BITS_ATTRIBUTE:
                parserData.resetAttrValue();
                parserData.setParserState(ParserState.UNKNOWN);
                break;
            case ATTRIBUTE:
                // Get current MO in stack
                attributeStatusBack(elementName);
                break;
            case CDT_MEMBER:
                try {
                    handleEndElementCdtMember(elementName);
                } catch (final ConstraintViolationException e) {
                    log.error("Exception caught during handling cdt member {}", e.getMessage());
                }
                log.debug("end element CDT MEMBER case {}", elementName);

                // Unset ParserState
                parserData.resetAttrValue();
                parserData.setParserState(ParserState.CDT_ATTRIBUTE);
                break;
            case CDT_ATTRIBUTE:
                handleEndElementCdtAttribute(elementName);

                // Unset ParserState
                parserData.resetAttrValue();
                parserData.setParserState(ParserState.UNKNOWN);
                break;
            default:
                // Get current MO in stack
                log.trace("Popping stack element of <{}>", elementName);
                if (parserData.getElementStack().peek().getTypeName().endsWith(elementName)) {
                    final MoDataObject stackTop = parserData.getElementStack().pop();
                    if (isEmodelSupported(stackTop)) {
                        if (ManagedObjectFlag.TO_BE_CREATED == stackTop.getDpsMoFlag()) {
                            processKeysAndSetMoName(stackTop);
                            log.trace("Finished handling the MO");
                        } else if (ManagedObjectFlag.TO_BE_UPDATED == stackTop.getDpsMoFlag()) {
                            updateDpsMo(stackTop);
                        }
                    } else {
                        log.warn("EModel does not exist for <{}>", elementName);
                    }
                }
                break;
        }
    }

    private void attributeStatusBack(final String elementName) {
        final MoDataObject stackTop = parserData.getElementStack().peek();
        final DataTypeSpecification dataTypeSpecification = stackTop.getPrimaryTypeSpecification().getAttributeSpecification(elementName)
                .getDataTypeSpecification();
        final PrimaryTypeAttributeSpecification primaryTypeAttributeSpecification = stackTop.getPrimaryTypeSpecification()
                .getAttributeSpecification(elementName);
        try {
            final Object attrValueObj =
                    convertAttributeValue(primaryTypeAttributeSpecification, dataTypeSpecification, parserData.getAttrValue());
            log.trace("******** attrValueObj : {}", attrValueObj);
            if (dataTypeSpecification.getDataType().equals(DataType.LIST)) {
                @SuppressWarnings("unchecked")
                List<Object> list = (List<Object>) stackTop.getAttrMap().get(elementName);
                if (list == null) {
                    list = new ArrayList<>();
                    stackTop.getAttrMap().put(elementName, list);
                }
                if (attrValueObj != null) {
                    list.add(attrValueObj);
                }
            } else {
                stackTop.getAttrMap().put(elementName, attrValueObj);
            }
            log.trace("Added <{}> to the attributeMap", elementName);
        } catch (final ConstraintViolationException e) {
            log.error("Received value <{}> for attribute <{}> (belonging to model <{}>) is not compatible with the data type, error: <{}>",
                    parserData.getAttrValue(), elementName, stackTop.getPrimaryTypeSpecification().getModelInfo().toUrn(), e.getMessage());
        }

        if (stackTop.getDpsMoFlag() == ManagedObjectFlag.CREATED) {
            stackTop.setDpsMoFlag(ManagedObjectFlag.TO_BE_UPDATED);
        }

        // Unset ParserState
        parserData.resetAttrValue();
        parserData.setParserState(ParserState.UNKNOWN);
    }

    private void handleEndElementCdtAttribute(final String elementName) {
        log.debug("handling cdt end attribute {}", elementName);
        final MoDataObject stackTop = parserData.getElementStack().peek();
        if (stackTop.getAttrMap().containsKey(elementName) && parserData.getIsCdtList()) {
            log.debug("appending the value {} to cdt list attribute {}", parserData.getCdtMembers(), elementName);
            final List<Object> cdtMembersList = (List<Object>) stackTop.getAttrMap().get(elementName);
            cdtMembersList.add(parserData.getCdtMembers());
            stackTop.getAttrMap().put(elementName, cdtMembersList);
        } else if (parserData.getIsCdtList()) {
            log.debug("updating the value {} to cdt list attribute {}", parserData.getCdtMembers(), elementName);
            final List<Map<String, Object>> complexMemList = new ArrayList<>();
            complexMemList.add(parserData.getCdtMembers());
            stackTop.getAttrMap().put(elementName, complexMemList);
            log.debug("complete list values of cdt list attributue {}", complexMemList);
        } else {
            log.debug("updating the value {} to cdt attribute {}", parserData.getCdtMembers(), elementName);
            stackTop.getAttrMap().put(elementName, parserData.getCdtMembers());
            log.debug("complete list values of cdt attributue {}", parserData.getCdtMembers());
        }

        log.debug("Added {} to the cdt member to map", elementName);
        parserData.setCdtMembers(Collections.emptyMap());
        parserData.setCdts(null);
        parserData.setIsCdtList(false);
    }

    private void handleEndElementCdtMember(final String elementName) throws ConstraintViolationException {
        try {
            final ComplexDataTypeSpecification cdts = parserData.getCdts();
            final ComplexDataTypeAttributeSpecification complexAttrSpec = cdts.getAttributeSpecification(elementName);
            final DataTypeSpecification dataTypeSpecification = complexAttrSpec.getDataTypeSpecification();
            final Object attrValueObj;
            attrValueObj = convertAttributeValueForComplexDataType(complexAttrSpec, dataTypeSpecification, parserData.getAttrValue());
            log.debug("handleEndElementCdtMember******** elementName {} attrValueObj : {}", elementName, attrValueObj);
            final Map<String, Object> cdtMembers = new HashMap<>(parserData.getCdtMembers());
            cdtMembers.put(elementName, attrValueObj);
            parserData.setCdtMembers(cdtMembers);
            log.debug("Added {} to the cdt member to map {}", elementName, parserData.getCdtMembers());
        } catch (final UnknownModelException ex) {
            log.warn("Model not found for cdt member {}", ex.getMessage());
        }
    }

    private void processMo(final String qName, final MoDataObject parentData, final String currentNameSpace) {
        String namespacePrefix = "";
        String elementName = qName;
        String nameSpace;
        neName = managedElementFdn != null ? managedElementFdn.replaceAll("(.*)" + "ManagedElement=", "").replaceAll(",(.*)", "") : null;
        /* Expected element format : prefix:elementName */
        if (qName.contains(XML_NAMESPACE_PREFIX_SEPARATOR)) {
            final String[] nameSpaceArray = qName.split(XML_NAMESPACE_PREFIX_SEPARATOR);
            namespacePrefix = nameSpaceArray[0]; // prefix
            elementName = nameSpaceArray[1]; // elementName
            nameSpace = parserData.getXmlNameSpace(namespacePrefix);
        } else {
            nameSpace = currentNameSpace != null || parentData == null ? currentNameSpace : parentData.getNamespace();
        }

        final String xPath = parentData != null ? parentData.getxPath(elementName) : elementName;
        if (nameSpace == null) {
            throw new IllegalArgumentException(
                    "Could not find any namespace for element <" + elementName + "> with namespacePrefix = <" + namespacePrefix + ">");
        }

        /* Get the version specific to element name */
        final String version = modelServiceUtils.getModelVersion(neType, NE_RELEASE_VERSION, nameSpace);
        if (version == null) {
            log.error("Could not find the version for namespace <{}>", nameSpace);
            ++elementSkipCount;
            return;
        }

        try {
            String uniqueTypeName = elementName;
            final PrimaryTypeSpecification primaryTypeSpecification = getPrimaryTypeSpecification(nameSpace, version, uniqueTypeName, parentData,
                    neName);
            if (primaryTypeSpecification != null) {
                uniqueTypeName = primaryTypeSpecification.getModelInfo().getName();
                handleModel(primaryTypeSpecification, parentData, uniqueTypeName, elementName, xPath, version, nameSpace);
            } else {
                throw new MOHandlerException(String.format("Namespace: %s Name: %s Version: %s", nameSpace, uniqueTypeName, version));
            }
        } catch (final Exception exception) {
            skipAllElements();
            log.warn("Unexpected exception {}", exception.getMessage());
            log.debug("error: ", exception);
        }
    }

    private PrimaryTypeSpecification getPrimaryTypeSpecification(final String namespace, final String version, final String type,
            final MoDataObject parentData, final String neName) {
        try {
            final Target target = new Target(CATEGORY_NODE, neType, neName, NE_RELEASE_VERSION);
            if (parentData != null) {
                return getHierarchicalPrimaryTypeSpecification(namespace, version, type, parentData, target);
            } else {
                final ModelInfo modeInfo = modelServiceUtils.getModelInfo(namespace, type, version);
                return modelServiceUtils.getPrimaryTypeSpecification(modeInfo, PrimaryTypeSpecification.class, target);
            }
        } catch (final MOHandlerException | UnknownModelException exception) {
            if (parentData != null) {
                return modelServiceUtils.getPrimaryTypeSpecification(namespace, version, type, neType, neName, NE_RELEASE_VERSION);
            }
            throw exception;
        }
    }

    public HierarchicalPrimaryTypeSpecification getHierarchicalPrimaryTypeSpecification(final String namespace, final String version,
            final String type, final MoDataObject parentData, final Target target) {
        final HierarchicalPrimaryTypeSpecification parentPrimaryTypeSpecification = modelServiceUtils.getPrimaryTypeSpecification(
                modelServiceUtils.getModelInfo(parentData), HierarchicalPrimaryTypeSpecification.class, target);
        final Optional<HierarchicalPrimaryTypeSpecification> matchedSpecification =
                matchHierarchialType(parentPrimaryTypeSpecification.getNonInheritedChildTypes(), type);
        if (matchedSpecification.isPresent()) {
            return matchedSpecification.get();
        } else {
            throw new MOHandlerException(String.format("Namespace: %s Type: %s Version: %s", namespace, type, version));
        }
    }

    private Optional<HierarchicalPrimaryTypeSpecification> matchHierarchialType(final Collection<HierarchicalPrimaryTypeSpecification> hList,
            final String type) {
        return hList.stream().filter(specification -> ModelServiceUtils.isOriginalYangNameMatch(specification, type)).findFirst();
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

    private void handleModel(final PrimaryTypeSpecification primaryTypeSpecification, final MoDataObject parentData, final String typeName,
            final String elementName, final String xPath, final String version, final String namespace) {

        MoDataObject parentManagedMo = null;

        /* Parent exists */
        if (parentData != null) {
            if (isEmodelSupported(parentData) && parentData.getDpsMoFlag() == ManagedObjectFlag.TO_BE_CREATED) {
                /* Handle the Parent MO for creation in YangDpsWriteHandler */
                parentManagedMo = processKeysAndSetMoName(parentData);
            } else {
                /*
                 * Parent MO is already handled for creation in YangDpsWriteHandler
                 */
                parentManagedMo = parentData;
            }
        }
        // Create ManagedObjectData and push to stack
        final MoDataObject moData = new MoDataObject();
        moData.setTypeName(typeName);
        moData.setVersion(version);
        moData.setNamespace(namespace);
        moData.setPrimaryTypeSpecification(primaryTypeSpecification);
        moData.setDpsMoFlag(ManagedObjectFlag.TO_BE_CREATED);
        // primaryTypeSpecification could be null if the EModel is not supported
        if (primaryTypeSpecification != null) {
            moData.setKeySet(ModelServiceUtils.getModelKeys(primaryTypeSpecification));
        }
        moData.setxPath(xPath);
        moData.setAdditionalInfoMap(additionalInfoMap);
        moData.setParentMo(parentManagedMo);
        if (null != parentManagedMo) {
            parentManagedMo.addChildMos(moData);
        }
        parserData.getElementStack().push(moData);
        log.trace("Pushed ManagedObjectData to Stack for element <{}>", elementName);
    }

    /**
     * Add the MO to the holder for management in YangDpsWriteHandler.
     */
    private MoDataObject processKeysAndSetMoName(final MoDataObject moData) {

        moData.setDpsMoFlag(ManagedObjectFlag.CREATED);
        final Set<String> keySet = moData.getKeySet();

        final String delimiter = moData.getPrimaryTypeSpecification().getMultiKeyDelimiter();
        final String keySeparator = delimiter != null ? delimiter : "..";

        String name;
        if (keySet.isEmpty()) {
            log.trace("Keyset is empty ");
            name = SINGLETON_MANAGEDOBJECTNAME;
        } else {
            // If there are no keys present for the emodel, set name as Emodel Name
            log.trace("Keyset - {}", keySet);

            name = keySet.stream()
                    .map(moData.getAttrMap()::get)
                    .filter(Objects::nonNull)
                    .map(keyValue -> {
                        if (keyValue instanceof List) {
                            return appendDpsList(keyValue);
                        } else {
                            return keyValue.toString();
                        }
                    })
                    .filter(s -> s != null && !s.isEmpty())
                    .collect(Collectors.joining(keySeparator));
        }

        log.trace("AttrMap - {}", moData.getAttrMap());
        moData.setName(name);
        final String moRdn = ModelServiceUtils.makeKey(moData);
        parserData.addManagedObject(managedElementFdn + "," + moRdn, moData);
        /*
         * check with DPS whether there are any performance impacts in creating MibRootMo and regular MO
         * return dpsUtils.createMibRootMO(modelInfo.getNamespace(), modelInfo.getName(), modelInfo.getVersion().toString(),
         */
        return moData;
    }

    /**
     * Validate the data type and check its constraint violations.
     *
     * @param dataTypeSpecification
     *            dataTypeSpecification
     * @param attributeValue
     *            attribute value to convert
     * @return valid data type
     * @throws ConstraintViolationException
     *             invalid constraint
     */
    private Object convertAttributeValue(final PrimaryTypeAttributeSpecification primaryTypeAttributeSpecification,
            final DataTypeSpecification dataTypeSpecification, final String attributeValue)
            throws ConstraintViolationException {
        final DataType dataType = dataTypeSpecification.getDataType();
        switch (dataType) {
            case BYTE:
            case SHORT:
            case INTEGER:
            case LONG:
            case DOUBLE:
                final Number numberValue = parseNumber(dataType, attributeValue);
                dataTypeSpecification.checkConstraints(numberValue);
                return numberValue;
            case BOOLEAN:
                final Boolean booleanValue = dataTypeSpecification.isEmpty() ? Boolean.TRUE : Boolean.valueOf(attributeValue);
                dataTypeSpecification.checkConstraints(booleanValue);
                return booleanValue;
            case STRING:
                dataTypeSpecification.checkConstraints(attributeValue);
                return attributeValue;
            case IP_ADDRESS:
                return attributeValue;
            case ENUM_REF:
                final String enumValue = convertEnumAttributeValue(primaryTypeAttributeSpecification, attributeValue);
                dataTypeSpecification.checkConstraints(enumValue);
                return enumValue;
            case LIST:
                if (attributeValue.isEmpty()) {
                    return null;
                }
                return convertAttributeValue(primaryTypeAttributeSpecification, dataTypeSpecification.getValuesDataTypeSpecification(),
                        attributeValue);
            case UNION:
                for (final DataTypeSpecification unionMemberDataTypeSpecification : dataTypeSpecification.getMembersDataTypeSpecification()) {
                    try {
                        return convertAttributeValue(primaryTypeAttributeSpecification, unionMemberDataTypeSpecification, attributeValue);
                    } catch (final ConstraintViolationException ignored) {
                    }
                }
                throw new ConstraintViolationException(
                        String.format("Received value <%s> is not compatible with any of the union member data type(s)", attributeValue));
            default:
                throw new ConstraintViolationException(
                        String.format("Received value <%s> is not compatible with any of the data type(s)", attributeValue));
        }
    }

    private Object convertAttributeValueForComplexDataType(final ComplexDataTypeAttributeSpecification complexAttrSpec,
            final DataTypeSpecification dataTypeSpecification, final String attributeValue) throws ConstraintViolationException {
        final DataType dataType = dataTypeSpecification.getDataType();
        switch (dataType) {
            case BYTE:
            case SHORT:
            case INTEGER:
            case LONG:
            case DOUBLE:
                return parseNumber(dataType, attributeValue);
            case BOOLEAN:
                return dataTypeSpecification.isEmpty() ? Boolean.TRUE : Boolean.valueOf(attributeValue);
            case STRING:
            case IP_ADDRESS:
                return attributeValue;
            case ENUM_REF:
                return convertEnumAttributeValueForComplexDataType(complexAttrSpec, attributeValue);
            case LIST:
                if (attributeValue.isEmpty()) {
                    return null;
                }
                return convertAttributeValueForComplexDataType(complexAttrSpec, dataTypeSpecification.getValuesDataTypeSpecification(),
                        attributeValue);
            case UNION:
                for (final DataTypeSpecification unionMemberDataTypeSpecification : dataTypeSpecification.getMembersDataTypeSpecification()) {
                    try {
                        return convertAttributeValueForComplexDataType(complexAttrSpec, unionMemberDataTypeSpecification, attributeValue);
                    } catch (final ConstraintViolationException ignored) {
                    }
                }
                throw new ConstraintViolationException(
                        String.format("Received value <%s> is not compatible with any of the union member data type(s)", attributeValue));
            default:
                throw new ConstraintViolationException(
                        String.format("Received value <%s> is not compatible with any of the data type(s)", attributeValue));
        }
    }

    private String convertEnumAttributeValueForComplexDataType(final ComplexDataTypeAttributeSpecification complexAttrSpec,
            final String attributeValue) {
        String enumMemberName = attributeValue;
        String prefix = "";

        if (attributeValue.contains(XML_NAMESPACE_PREFIX_SEPARATOR)) {
            final String[] tokens = attributeValue.split(XML_NAMESPACE_PREFIX_SEPARATOR);
            enumMemberName = tokens[1];
            prefix = tokens[0];
        }
        enumMemberName = enumMemberName.trim();
        if (complexAttrSpec.getDataTypeSpecification().isReferenceDataType()) {
            try {
                final TypedModelAccess typedModelAccess = modelService.getTypedAccess();
                final ModelInfo enumModelInfo = complexAttrSpec.getDataTypeSpecification().getReferencedDataType();
                final Target target = new Target(CATEGORY_NODE, neType, neName, NE_RELEASE_VERSION);
                final EnumDataTypeSpecification edts =
                        typedModelAccess.getEModelSpecification(enumModelInfo, EnumDataTypeSpecification.class, target);
                final Collection<EnumDataTypeMemberSpecification> enumMembers = edts.getAllMembersByName(enumMemberName);
                if (log.isTraceEnabled()) {
                    log.trace("Retrieving enum specification......enumMembers : {}, enumMemberName : {}, prefix : {}", enumMembers, enumMemberName,
                            prefix);
                }
                enumMemberName = handleDuplicateEnum(enumMembers, enumMemberName, prefix);
            } catch (final Exception e) {
                log.error("Exception encountered in fetching enum member:{} and its namespace for attribute:{}, exception msg {}", attributeValue,
                        complexAttrSpec.getName(),
                        e.getMessage());
            }
        }
        return enumMemberName;
    }

    private String convertEnumAttributeValue(final PrimaryTypeAttributeSpecification primaryTypeAttributeSpecification, final String attributeValue) {
        String enumMemberName = attributeValue;
        String prefix = "";

        if (attributeValue.contains(XML_NAMESPACE_PREFIX_SEPARATOR)) {
            final String[] tokens = attributeValue.split(XML_NAMESPACE_PREFIX_SEPARATOR);
            enumMemberName = tokens[1];
            prefix = tokens[0];
        }
        enumMemberName = enumMemberName.trim();
        if (primaryTypeAttributeSpecification.getDataTypeSpecification().isReferenceDataType()) {
            try {
                final Collection<EnumDataTypeMemberSpecification> enumMembers =
                        getEnumMembersByName(modelService.getTypedAccess(), primaryTypeAttributeSpecification, enumMemberName);
                log.trace("Retrieving enum specification......enumMembers : {}, enumMemberName : {}, prefix : {}", enumMembers, enumMemberName,
                        prefix);

                enumMemberName = handleDuplicateEnum(enumMembers, enumMemberName, prefix);
            } catch (final Exception e) {
                log.error("Exception encountered in fetching enum member:{} and its namespace for attribute:{}, exception msg {}", attributeValue,
                        primaryTypeAttributeSpecification.getName(), e.getMessage());
            }
        }

        return enumMemberName;
    }

    private String handleDuplicateEnum(final Collection<EnumDataTypeMemberSpecification> enumMembers, final String enumMemberName,
            final String prefix) {

        if (enumMembers == null || enumMembers.size() < 2) {
            return enumMemberName;
        }

        String namespace;
        for (final EnumDataTypeMemberSpecification enumDataTypeMemberSpecification : enumMembers) {
            final Map<String, String> metaInformation = enumDataTypeMemberSpecification.getMetaInformation();
            if (metaInformation != null) {
                final String enumPrefix = metaInformation.get("YANG_ORIG_MODULE_PREFIX");
                if (enumPrefix != null && !enumPrefix.isEmpty() && enumPrefix.equalsIgnoreCase(prefix)) {
                    log.trace("Recieved metaInformation - {} for enumMember - {} and enumPrefix is {}", metaInformation, enumMemberName,
                            enumPrefix);
                    namespace = enumDataTypeMemberSpecification.getMemberNamespace();
                    return namespace.concat("$$$").concat(enumMemberName);
                }
            }
        }
        log.warn("attribute value - {} returned without prepending namespace as metaInformation doesn't store namespace for prefix - {}",
                enumMemberName, prefix);
        return enumMemberName;
    }

    public Collection<EnumDataTypeMemberSpecification> getEnumMembersByName(final TypedModelAccess typedModelAccess,
            final PrimaryTypeAttributeSpecification primaryTypeAttributeSpecification,
            final String enumMemberName) {
        final ModelInfo enumModelInfo = primaryTypeAttributeSpecification.getDataTypeSpecification().getReferencedDataType();
        final Target target = new Target(CATEGORY_NODE, neType, neName, NE_RELEASE_VERSION);
        final EnumDataTypeSpecification edts = typedModelAccess.getEModelSpecification(enumModelInfo, EnumDataTypeSpecification.class, target);
        return edts.getAllMembersByName(enumMemberName);
    }

    /*
     * Update DPS MO Update is not supported now
     */
    private void updateDpsMo(final MoDataObject moData) {
        moData.setDpsMoFlag(ManagedObjectFlag.UPDATED);
    }
}
