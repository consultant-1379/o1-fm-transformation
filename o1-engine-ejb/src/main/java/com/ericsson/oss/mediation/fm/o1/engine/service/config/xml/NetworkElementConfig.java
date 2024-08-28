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

package com.ericsson.oss.mediation.fm.o1.engine.service.config.xml;

import java.io.Serializable;
import java.util.Objects;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Class that represents the 'network-element' XML tags in the o1-mediation-config.xml.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = { "type", "version", "transformerConfig" })
public class NetworkElementConfig implements Serializable {

    @XmlAttribute(name = "type")
    private String type;

    @XmlAttribute(name = "version")
    private String version;

    @XmlElement(name = "transformer-config")
    private String transformerConfig;

    /**
     * Returns the network element type.
     *
     * @return the network element type.
     */
    public String getType() {
        return type;
    }

    /**
     * Returns the network element version.
     *
     * @return the network element version.
     */
    public String getVersion() {
        return version;
    }

    /**
     * Returns the network element model transformer config jar.
     *
     * @return the network element model transformer jar.
     */
    public String getTransformerConfig() {
        return transformerConfig;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final NetworkElementConfig that = (NetworkElementConfig) o;
        return Objects.equals(type, that.type) && Objects.equals(version, that.version) && Objects.equals(transformerConfig, that.transformerConfig);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, version, transformerConfig);
    }

    @Override
    public String toString() {
        return "NetworkElementConfig{" +
                "type='" + type + '\'' +
                ", version='" + version + '\'' +
                ", transformerConfig='" + transformerConfig + '\'' +
                '}';
    }
}
