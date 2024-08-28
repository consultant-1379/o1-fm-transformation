
package com.ericsson.oss.mediation.fm.o1.engine.service.config.xml;

import java.util.List;
import java.util.Objects;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import lombok.extern.slf4j.Slf4j;

/**
 * Class that represents the 'o1-mediation-config' XML tags in the o1-mediation-config.xml.
 */
@XmlRootElement(name = "o1-mediation-config")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = { "networkElementConfigurations" })
@Slf4j
public class O1MediationConfig {

    @XmlElement(name = "network-element")
    private List<NetworkElementConfig> networkElementConfigurations;

    public List<NetworkElementConfig> getNetworkElementConfigurations() {
        return networkElementConfigurations;
    }

    @Override
    public String toString() {
        return "O1MediationConfig{" +
                "networkElementConfigurations=" + networkElementConfigurations +
                '}';
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final O1MediationConfig that = (O1MediationConfig) o;
        return Objects.equals(networkElementConfigurations, that.networkElementConfigurations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(networkElementConfigurations);
    }
}
