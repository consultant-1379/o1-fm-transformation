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

import com.ericsson.oss.mediation.fm.o1.engine.transform.util.XmlUtils;
import com.ericsson.oss.mediation.fm.o1.engine.transform.Constants;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.jelly.JellyContext;
import org.apache.commons.jelly.Script;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

@Slf4j
public class ModelTransformerXmlProcessor {

    private static final String ALL_ELEMENTS_XPATH = "child::*";


    /**
     * Creates and compiles all transformation scripts defined in the given document.
     *
     * @param context
     *            the jelly context used to compile the transformation scripts.
     * @param document
     *            the document that contains the transformer configuration.
     * @return a list of pre-compiled transformation scripts
     */
    static List<ScriptDecorator> createAndCompileTransformerScripts(final JellyContext context, final Document document) {
        final List<ScriptDecorator> scripts = new ArrayList<>();
        try {
            final XPathFactory xPathFactory = XPathFactory.newInstance();
            final XPath xPath = xPathFactory.newXPath();
            final NodeList transformerNodeList = (NodeList) xPath.evaluate(Constants.MODEL_TRANSFORMER_ELEMENT_XPATH, document,
                    XPathConstants.NODESET);
            for (int i = 0; i < transformerNodeList.getLength(); i++) {
                final Element documentElement = (Element) document.getDocumentElement().cloneNode(false);
                final Element modelTransformerElement = (Element) transformerNodeList.item(i);
                final String transformerId = modelTransformerElement.getAttribute(Constants.ID_ATTRIBUTE);
                documentElement.appendChild(modelTransformerElement.cloneNode(true));
                final InputSource inputSource = XmlUtils.nodeToInputSource(documentElement);
                inputSource.setSystemId(document.getDocumentURI());
                final Script script = context.compileScript(inputSource);
                String scriptElement = XmlUtils.getNodeDump(documentElement);
                final ScriptDecorator scriptDecorator = new ScriptDecorator(transformerId, script, document.getDocumentURI(), scriptElement);
                scripts.add(scriptDecorator);
                log.debug("Created transformer source for script with id: {} script: {}", transformerId, scriptElement);
            }
        } catch (Exception ex) {
            log.error("Error creating script input source", ex);
            throw new TransformerException("Error creating script input source", ex);
        }
        return scripts;
    }

    /**
     * Process the xml configuration file.
     *
     * @param uri
     *            the xml configuration file.
     * @return processed configuration as DOMSource.
     */
    public static Document process(final URI uri) {
        return process(uri.toString());
    }

    /**
     * Process the xml configuration file.
     *
     * @param uri
     *            the xml configuration file.
     * @return processed configuration as DOMSource.
     */
    private static Document process(final String uri){
        final DocumentBuilder db = XmlUtils.getDocumentBuilder();
        try {
            Document document = db.parse(uri);
            document = resolveXmlImports(document);
            document = resolveTransformerIncludes(document);
            return document;
        } catch (Exception e) {
            log.error("Error processing xml file: " + uri, e);
            throw new TransformerException("Error processing xml file: " + uri, e);
        }
    }

    private static Document resolveXmlImports(final Document document) throws XPathExpressionException, URISyntaxException {
        final NodeList importElements = document.getElementsByTagName(Constants.IMPORT_TRANSFORMER_CONFIG_TAG);
        for (int i = 0; i < importElements.getLength(); i++) {
            final Element importElement = (Element) importElements.item(i);
            final String uri = importElement.getAttribute(Constants.URI_ATTRIBUTE);
            if (uri != null && !uri.isEmpty()) {
                final Document importedDocument = XmlUtils.getDocument(new URI(uri));
                mergeElements(importedDocument, document);
            } else {
                final String file = importElement.getAttribute(Constants.FILE_ATTRIBUTE);
                if (file != null && !file.isEmpty()) {
                    final URI documentUri = new URI(document.getDocumentURI());
                    String schemeSpecificPart = documentUri.getSchemeSpecificPart();
                    final int index = schemeSpecificPart.lastIndexOf("/");
                    schemeSpecificPart = schemeSpecificPart.substring(0, index).concat("/").concat(file);
                    final URI srcURI = new URI(documentUri.getScheme(), schemeSpecificPart, documentUri.getFragment());
                    final Document importedDocument = XmlUtils.getDocument(srcURI);
                    mergeElements(importedDocument, document);
                } else {
                    log.error("Invalid import-transformer-config, attribute 'file' not specified.");
                    throw new TransformerException("Invalid import-transformer-config, attribute 'file' not specified.");
                }
            }
        }
        for (int i = 0; i < importElements.getLength(); i++) {
            final Element importElement = (Element) importElements.item(i);
            importElement.getParentNode().removeChild(importElement);
        }
        Node importsNode = (Node) XPathFactory.newInstance().newXPath()
                .evaluate(Constants.MODEL_TRANSFORMER_IMPORTS_ELEMENT_XPATH, document, XPathConstants.NODE);
        if (importsNode != null) {
            importsNode.getParentNode().removeChild(importsNode);
        }
        return document;
    }

    private static Document resolveTransformerIncludes(final Document document) throws XPathExpressionException {
        final NodeList definedTransformerNodeList = document.getElementsByTagName(Constants.DEFINE_MODEL_TRANSFORMER_TAG);
        final Map<String, Node> definedTransformerNodeMap = new HashMap<>();
        addTransformerNames(definedTransformerNodeList, definedTransformerNodeMap);
        final NodeList includeTransformerNodeList = document.getElementsByTagName(Constants.INCLUDE_MODEL_TRANSFORMER_TAG);
        for (int x = 0; x < includeTransformerNodeList.getLength(); x++) {
            final Element includeElement = (Element) includeTransformerNodeList.item(x);
            final String transformer = includeElement.getAttribute(Constants.TRANSFORMER_ATTRIBUTE);
            if (transformer == null || transformer.isEmpty()) {
                log.error("Invalid include-model-transformer usage. Attribute 'transformer' cannot be or empty");
                throw new TransformerException("Invalid include-model-transformer usage. "
                        + "Attribute 'transformer' cannot be or empty");
            }
            if (!definedTransformerNodeMap.containsKey(transformer)) {
                log.error("Invalid include-model-transformer usage. No such defined-transformer: " + transformer);
                throw new TransformerException("Invalid include-model-transformer usage. " + "No such defined-transformer: "
                        + transformer);
            }
            final Element definedTransformerElement = (Element) definedTransformerNodeMap.get(transformer);
            final Element transformerElement = (Element) includeElement.getParentNode();
            addChildNodeSet(definedTransformerElement, transformerElement);
        }
        XmlUtils.removeAll(definedTransformerNodeList);
        XmlUtils.removeAll(includeTransformerNodeList);
        return document;
    }

    private static void mergeElements(final Document src, final Document dest) throws XPathExpressionException {
        mergeElement(src, dest, Constants.TAG_LIBRARIES_ELEMENT_XPATH);
        mergeElement(src, dest, Constants.ENUMS_ELEMENT_XPATH);
        mergeElement(src, dest, Constants.MODEL_TRANSFORMERS_ELEMENT_XPATH);
    }

    private static void mergeElement(final Document src, final Document dest, final String path) throws XPathExpressionException {
        final XPath xpath = XPathFactory.newInstance().newXPath();
        final Node srcNode = (Node) xpath.evaluate(path, src, XPathConstants.NODE);
        if (srcNode != null) {
            final Node dstNode = (Node) xpath.evaluate(path, dest, XPathConstants.NODE);
            if (dstNode != null) {
                final NodeList srcChildrenElements = (NodeList) xpath.evaluate(ALL_ELEMENTS_XPATH, srcNode, XPathConstants.NODESET);
                for (int i = 0; i < srcChildrenElements.getLength(); i++) {
                    final Node importedNode = dest.importNode(srcChildrenElements.item(i), true);
                    dstNode.appendChild(importedNode);
                }
            } else {
                final Node importedNode = dest.importNode(srcNode, true);
                final Element documentElement = dest.getDocumentElement();
                documentElement.appendChild(importedNode);
            }
        }
    }

    private static void addChildNodeSet(final Element definedTransformerElement, final Element transformerElement) throws XPathExpressionException {
        final NodeList childNodes = (NodeList) XPathFactory.newInstance().newXPath()
                .evaluate(ALL_ELEMENTS_XPATH, definedTransformerElement, XPathConstants.NODESET);
        for (int y = 0; y < childNodes.getLength(); y++) {
            final Node append = childNodes.item(y);
            transformerElement.appendChild(append.cloneNode(true));
        }
    }

    private static void addTransformerNames(final NodeList definedTransformerNodeList, final Map<String, Node> definedTransformerNodeMap) {
        for (int i = 0; i < definedTransformerNodeList.getLength(); i++) {
            final Element definedTransformerElement = (Element) definedTransformerNodeList.item(i);
            final String name = definedTransformerElement.getAttribute(Constants.NAME_ATTRIBUTE);
            if (name == null || name.isEmpty()) {
                log.error("Invalid define-model-transformer usage. Attribute 'name' cannot be nul or empty");
                throw new TransformerException("Invalid define-model-transformer usage. Attribute 'name' cannot be nul or empty");
            }
            definedTransformerNodeMap.put(name, definedTransformerElement.cloneNode(true));
        }
    }
}
