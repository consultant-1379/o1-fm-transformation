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

package com.ericsson.oss.mediation.o1.yang.handlers.netconf.util;

import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.Constants.ALARM_LIST_RDN;
import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.Constants.HEARTBEAT_CONTROL_RDN;
import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.Constants.MANAGED_ELEMENT;
import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.Constants.ME_CONTEXT;
import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.Constants.NETWORK_ELEMENT;
import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.Constants.NTF_SUBSCRIPTION_CONTROL_RDN;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.ericsson.oss.mediation.o1.yang.handlers.netconf.exception.MOHandlerException;

import lombok.experimental.UtilityClass;

/**
 * Utility methods for FDN Handling.
 */
@UtilityClass
public class FdnUtil {

    /**
     * A regular expression that matches the NetworkElement RDN
     * <p>
     * The ([^,]+) captures the value other than comma and our required value should be
     * in matcher.group(1)
     */

    private static final Pattern NETWORK_ELEMENT_RDN = Pattern.compile("NetworkElement=([^,]+)");

    /**
     * Generates the Managed Element FDN using the provided Normalised FDN and Managed Element ID.
     *
     * @param normalisedFdn
     *            The Normalised FDN of the element.
     * @param managedElementId
     *            The ID of the Managed Element.
     * @return A String representing the fully constructed Managed Element FDN.
     */
    public static String getManagedElementFdn(final String normalisedFdn, final String managedElementId) {
        return String.join(",", getMeContextRdn(normalisedFdn), getManangedElementRdn(managedElementId));
    }

    /**
     * Generates the Heartbeat Control FDN using the provided Normalised FDN and Managed Element ID.
     *
     * @param normalisedFdn
     *            The Normalised FDN of the element.
     * @param managedElementId
     *            The ID of the Managed Element.
     * @return The Heartbeat Control FDN.
     */
    public static String getHeartbeatControlFdn(final String normalisedFdn, final String managedElementId) {
        return String.join(",", getMeContextRdn(normalisedFdn), getManangedElementRdn(managedElementId), NTF_SUBSCRIPTION_CONTROL_RDN,
                HEARTBEAT_CONTROL_RDN);
    }

    /**
     * Generates the NTF Subscription Control FDN using the provided Normalised FDN and Managed Element ID.
     *
     * @param normalisedFdn
     *            The Normalised FDN of the element.
     * @param managedElementId
     *            The ID of the Managed Element.
     * @return The NTF Subscription Control FDN.
     */
    public static String getNtfSubscriptionControlFdn(final String normalisedFdn, final String managedElementId) {
        return String.join(",", getMeContextRdn(normalisedFdn), getManangedElementRdn(managedElementId), NTF_SUBSCRIPTION_CONTROL_RDN);
    }

    /**
     * Generates the Alarm List FDN using the provided Normalised FDN and Managed Element ID.
     *
     * @param normalisedFdn
     *            The Normalised FDN of the element.
     * @param managedElementId
     *            The ID of the Managed Element.
     * @return The Alarm List FDN.
     */
    public static String getAlarmListFdn(final String normalisedFdn, final String managedElementId) {
        return String.join(",", getMeContextRdn(normalisedFdn), getManangedElementRdn(managedElementId), ALARM_LIST_RDN);
    }

    /**
     * Retrieves the Managed Element FDN from the provided normalised FDN.
     *
     * @param normalisedFdn
     *            The Normalised FDN of the element.
     * @return The Managed Element FDN if found within the provided FDN.
     * @throws MOHandlerException
     *             If the Managed Element FDN is not found in the provided FDN.
     */
    public String getManagedElementFdn(final String normalisedFdn) {
        String[] values = normalisedFdn.split(",");

        for (int i = 0; i < values.length; i++) {
            if (values[i].contains(MANAGED_ELEMENT)) {
                return String.join(",", Arrays.copyOfRange(values, 0, i + 1));
            }
        }

        throw new MOHandlerException("Failed to find ManagedElement FDN in the provided FDN: " + normalisedFdn);
    }

    /**
     * Extracts the MeContext FDN from the provided string containing the MeContext information.
     *
     * @param fdnContainingMeContext
     *            The string containing MeContext information.
     * @return The extracted MeContext FDN if found; otherwise, an empty string.
     */
    public static String extractMeContextFdn(final String fdnContainingMeContext) {
        final int mecontextStartIndex = fdnContainingMeContext.indexOf(ME_CONTEXT);
        final int mecontextEndIndex = fdnContainingMeContext.indexOf(',', mecontextStartIndex);
        return fdnContainingMeContext.substring(0, mecontextEndIndex);
    }

    /**
     * Generates a NetworkElement FDN given a mirror FDN. Does not call DPS.
     * 
     * @param fdn
     *            The FDN of an MO on the network node. E.g. 'Subnetwork=A,MeContext=B,ENodeBFunction=C'
     * @return The FDN of the NetworkElement. E.g. 'NetworkElement=B'
     */
    public static String getNetworkElementFdn(final String fdn) {
        final String networkElementFdn = Arrays.stream(fdn.split(","))
                .filter(e -> e.contains(ME_CONTEXT))
                .collect(Collectors.joining());
        return networkElementFdn.replace(ME_CONTEXT, NETWORK_ELEMENT);
    }

    private static String getManangedElementRdn(final String managedElementId) {
        return "ManagedElement=" + managedElementId;
    }

    private static String getMeContextRdn(final String normalisedFdn) {
        final Matcher matcher = NETWORK_ELEMENT_RDN.matcher(normalisedFdn);
        matcher.find();
        return "MeContext=" + matcher.group(1);
    }

}
