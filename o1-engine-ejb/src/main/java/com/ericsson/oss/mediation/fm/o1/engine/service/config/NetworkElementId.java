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

package com.ericsson.oss.mediation.fm.o1.engine.service.config;

import java.io.Serializable;
import java.util.Objects;

/**
 * This class represents a network element identifier. The identifier is
 * composed of two fields: type and version. Two identifiers are equal if and
 * only if they are equal their types and versions.
 */
public class NetworkElementId implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String type;
    private final String version;

    /**
     * Creates a new network element identifier with the given type and version.
     *
     * @param type
     *            the network element type.
     * @param version
     *            the network element version.
     */
    public NetworkElementId(final String type, final String version) {
        this.type = type;
        this.version = version;
    }

    /**
     * Returns the network element type.
     *
     * @return network element type.
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
     * Returns a hash code value for the network element identifier object.
     *
     * @return the hash code value.
     */
    @Override
    public int hashCode() {
        return Objects.hash(type, version);
    }

    /**
     * Compares this network element identifier to the specified object. The
     * result is true if and only if the argument is not null, is a
     * NetworkElementId object and the type and version are equal.
     *
     * @param o
     *            the object to compare this network element identifier
     * @return true if and only if the argument is not null, is a
     *         NetworkElementId object and the type and version are equal, false
     *         otherwise.
     */
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final NetworkElementId that = (NetworkElementId) o;
        return Objects.equals(type, that.type) && Objects.equals(version, that.version);
    }

    /**
     * Returns a string representation of this network element identifier.
     *
     * @return the string representation of this network element identifier.
     */
    @Override
    public String toString() {
        return "NetworkElementId{" +
                "type='" + type + '\'' +
                ", version='" + version + '\'' +
                '}';
    }
}
