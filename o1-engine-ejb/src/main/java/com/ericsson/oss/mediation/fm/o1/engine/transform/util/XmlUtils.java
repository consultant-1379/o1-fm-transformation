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
package com.ericsson.oss.mediation.fm.o1.engine.transform.util;

import com.ericsson.oss.mediation.fm.o1.engine.transform.TransformerException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.URI;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * This class consists exclusively of static methods that operate on xml documents.
 */
public class XmlUtils {

    public static final String DEFAULT_ENCODING = "UTF-8";
    private static String encoding = DEFAULT_ENCODING;

    private XmlUtils() {}

    /**
     * Returns a DocumentBuilder object.
     *
     * @return the DocumentBuilder.
     */
    public static DocumentBuilder getDocumentBuilder() {
        try {
            final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            return dbf.newDocumentBuilder();
        } catch (Exception ex) {
            throw new TransformerException("Error creating document builder", ex);
        }
    }

    /**
     * Parses the given xml file and returns the relative Document.
     *
     * @param uri the xml file uri.
     * @return the xml Document.
     */
    public static Document getDocument(final URI uri) {
        try {
            final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            final DocumentBuilder db = dbf.newDocumentBuilder();
            return db.parse(uri.toString());
        } catch (Exception ex) {
            throw new TransformerException("Error parsing document from uri: " + uri, ex);
        }
    }

    /**
     * Removes all nodes in the given node list
     *
     * @param nodeList the nodes to remove
     */
    public static void removeAll(final NodeList nodeList) {
        for (int i = 0; i < nodeList.getLength(); i++) {
            final Node node = nodeList.item(i);
            node.getParentNode().removeChild(node);
        }
    }

    /**
     * Transforms the specified org.w3c.dom.Node into an org.xml.sax.InputSource
     * object.
     *
     * @param node the org.w3c.dom.Node to transform.
     * @return the org.xml.sax.InputSource object.
     */
    public static final InputSource nodeToInputSource(final Node node) throws IOException {
        return sourceToInputSource(new DOMSource(node));
    }

    /**
     * Transforms the specified source into an org.xml.sax.InputSource object.
     *
     * @param source the source.
     * @return the org.xml.sax.InputSource object.
     */
    public static final InputSource sourceToInputSource(final Source source) throws IOException {
         if (source instanceof DOMSource) {
             ByteArrayOutputStream outputStream = null;
             try {
                 outputStream = new ByteArrayOutputStream();
                 Node node = ((DOMSource) source).getNode();
                 if (node instanceof Document) {
                     node = ((Document) node).getDocumentElement();
                 }
                 final Element domElement = (Element) node;
                 elementToStream(domElement, outputStream);
                 final InputSource inputSource = new InputSource(source.getSystemId());
                 inputSource.setByteStream(new ByteArrayInputStream(outputStream.toByteArray()));
                 return inputSource;
             }  finally {
                 if (outputStream != null) {
                     outputStream.close();
                 }
             }
        }
         throw new TransformerException("Expected source to be of type DOMSource");
    }

    /**
     * Transforms the specified org.w3c.dom.Element into arrays of bytes and
     * writes them to a stream.
     *
     * @param element the dom element to transform.
     * @param output the stream where to write.
     */
    public static final void elementToStream(final Element element, final OutputStream output) {
        try {
            final DOMSource source = new DOMSource(element);
            final StreamResult result = new StreamResult(output);
            final TransformerFactory transFactory = TransformerFactory.newInstance();
            final Transformer transformer = transFactory.newTransformer();
            transformer.transform(source, result);
        } catch (Exception ex) {
            throw new TransformerException("Unable to convert element to stream", ex);
        }
    }

    /**
     * Prints the given node as a string.
     *
     * @param node the node to print.
     * @return the node dump.
     */
    public static final String getNodeDump(final Node node) {
        try {
            final StringWriter stringWriter = new StringWriter();
            print(new DOMSource(node), new StreamResult(stringWriter));
            return stringWriter.toString();
        } catch (Exception ex) {
            throw new TransformerException("Unable to print node", ex);
        }
    }

    /**
     * Prints the given Source to the specified Result.
     *
     * @param source the source to print.
     * @param result the Result object.
     */
    public static final void print(final Source source, final Result result) {
        try {
            final TransformerFactory transformerFactory = TransformerFactory.newInstance();
            final Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, encoding);
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            transformer.transform(source, result);
        } catch (Exception ex) {
            throw new TransformerException("Unable to print dom source", ex);
        }
    }
}
