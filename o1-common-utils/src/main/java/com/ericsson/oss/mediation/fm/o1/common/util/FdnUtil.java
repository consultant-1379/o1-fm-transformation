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

package com.ericsson.oss.mediation.fm.o1.common.util;

import static com.ericsson.oss.mediation.fm.o1.common.Constants.ME_CONTEXT;
import static com.ericsson.oss.mediation.fm.o1.common.Constants.NETWORK_ELEMENT;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;

import lombok.experimental.UtilityClass;

@UtilityClass
public class FdnUtil {

    public static String getParentFdn(final String fdn) {
        final int lastIndex = fdn.lastIndexOf(',');
        return fdn.substring(0, lastIndex);
    }

    /**
     * Gets the NetworkElement FDN from a mirror FDN (left hand side FDN).
     *
     * @param fdn
     *            The mirror MO FDN.
     *            e.g. SubNetwork=A,SubNetwork=B,MeContext=O1,GNBFunction=1
     * @return The NetworkElement FDN.
     *         e.g. NetworkElement=O1
     */
    public static String getNetworkElementFdn(final String fdn) {
        final String networkElementFdn = Arrays.stream(fdn.split(","))
                .filter(e -> e.contains(ME_CONTEXT))
                .collect(Collectors.joining());
        return networkElementFdn.replace(ME_CONTEXT, NETWORK_ELEMENT);
    }

    /**
     * Gets the FDN of the MeContext from the node FDN provided.
     *
     * @param fdn
     *            Any node MO FDN.
     *            e.g. SubNetwork=A,SubNetwork=B,MeContext=O1,ManagedElement=1,AlarmList=1
     * @return The MeContext FDN.
     *         e.g. SubNetwork=A,SubNetwork=B,MeContext=O1,
     */
    public static String getMeContextFdn(final String fdn) {
        return fdn.substring(0, fdn.indexOf("ManagedElement"));
    }

    public static String createFdn(String parentFdn, String childLdn) {

        return StringUtils.isBlank(childLdn) ? parentFdn : String.join(",", parentFdn, childLdn);
    }

    /**
     * Gets the id of the MeContext from the dnPrefix provided.
     *
     * @param dnPrefix
     *            Any dnPrefix.
     *            e.g. MeContext=O1
     * @return The MeContext ID.
     *         e.g. O1
     */
    public static String getMeContextId(String dnPrefix) {
        String key = "MeContext=";
        int indexOfMeContext = dnPrefix.indexOf(key);
        return dnPrefix.substring(indexOfMeContext + key.length()).trim();
    }
}
