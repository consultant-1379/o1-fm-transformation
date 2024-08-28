
package com.ericsson.oss.mediation.fm.o1.engine.service.config.xml;

import com.ericsson.oss.mediation.fm.o1.engine.service.config.MediationConfigurationException;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Document;

import javax.xml.bind.JAXB;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.dom.DOMSource;
import java.net.URL;

@Slf4j
public class MediationConfigurationXmlLoader {



    public O1MediationConfig load(final URL configFile) {
        log.debug("Loading configuration from: " + configFile);
        try {
            DocumentBuilderFactory df = DocumentBuilderFactory.newInstance(com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl.class.getName(), null);
            preventExternalAccess(df);
            DocumentBuilder builder = df.newDocumentBuilder();
            Document document = builder.parse(configFile.toURI().toString());
            DOMSource domSource = new DOMSource(document);
            return JAXB.unmarshal(domSource, O1MediationConfig.class);
        } catch (final Exception e) {
            throw new MediationConfigurationException("Unable to load configuration file: " + configFile, e);
        }
    }

    private void preventExternalAccess(final DocumentBuilderFactory df) {
        df.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        df.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
    }
}
