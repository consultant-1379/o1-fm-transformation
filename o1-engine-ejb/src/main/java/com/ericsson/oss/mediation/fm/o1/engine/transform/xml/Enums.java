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
package com.ericsson.oss.mediation.fm.o1.engine.transform.xml;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Enums class represents a collection of enums defined in a xml file.
 *
 */
@XmlType(propOrder = "enumConfigs")
@XmlAccessorType(XmlAccessType.FIELD)
public class Enums implements Serializable {

    @XmlElement(name = "t:enum")
    private List<EnumConfig> enumConfigs;

    /**
     * Returns the EnumConfig list.
     *
     * @return the EnumConfig list.
     */
    public List<EnumConfig> getEnumConfigs() {
        return enumConfigs;
    }

    /**
     * EnumConfig class represents an enum defined in a xml file.
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(propOrder = {"name", "entries"})
    public static final class EnumConfig implements Serializable{

        private static final long serialVersionUID = 5586796541620741142L;

        @XmlAttribute
        private String name;
        @XmlElement(name = "t:enum-entries")
        @XmlJavaTypeAdapter(value = EnumEntryConfigXmlAdapter.class)
        private HashMap<Integer, String> entries;

        /**
         * Returns the enum name.
         *
         * @return the enum name.
         */
        public String getName() {
            return name;
        }

        /**
         * Sets the enum name.
         *
         * @param name the enum name.
         */
        public void setName(final String name) {
            this.name = name;
        }

        /**
         * Returns all enum entries as a map
         *
         * @return the enum entries.
         */
        public Map<Integer, String> getEntries() {
            return entries;
        }
    }

    /**
     *
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(propOrder = {"ordinal", "value"})
    public static final class EnumEntryConfig implements Serializable {

        @XmlAttribute(name = "ordinal")
        private Integer ordinal;
        @XmlValue
        private String value;

        /**
         * Returns the enum entry ordinal value.
         *
         * @return the enum entry ordinal value.
         */
        public Integer getOrdinal() {
            return ordinal;
        }

        /**
         * Returns the enum value.
         *
         * @return the enum value.
         */
        public String getValue() {
            return value;
        }
    }

    /**
     * Enum entry config adapter class
     */
    public static final class EnumEntryConfigXmlAdapter extends XmlAdapter<EnumEntryConfigXmlAdapter.EnumEntryConfigs, HashMap<Integer, String>> {

        /**
         * Unmarshal callback method.
         *
         * @param v the enum entry config.
         * @return the map of enum entry config.
         * @throws Exception if there's an error during the conversion.
         */
        @Override
        public HashMap<Integer, String> unmarshal(final EnumEntryConfigs v) {
            final HashMap<Integer, String> enumEntryMap = new HashMap<>();
            for (EnumEntryConfig enumEntry : v.entries) {
                enumEntryMap.put(enumEntry.getOrdinal(), enumEntry.getValue());
            }
            return enumEntryMap;
        }

        /**
         * Marshal callback method.
         *
         * @param v the map of enum entry config.
         * @return the enum entry config.
         * @throws Exception if there's an error during the conversion.
         */
        @Override
        public EnumEntryConfigs marshal(final HashMap<Integer, String> v) {
            // Not used
            return null;
        }

        /**
         * Enum entry config class.
         */
        public static final class EnumEntryConfigs {
            @XmlElement(name = "t:enum-entry")
            List<EnumEntryConfig> entries;
        }
    }
}
