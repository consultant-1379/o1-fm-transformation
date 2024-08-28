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

package com.ericsson.oss.mediation.fm.o1.engine.transform.converter.impl;

import java.lang.reflect.Field;

import com.ericsson.oss.mediation.fm.o1.common.util.FdnUtil;
import org.apache.commons.jelly.JellyContext;

import com.ericsson.oss.mediation.fm.o1.engine.transform.converter.AbstractModelConverter;
import com.ericsson.oss.mediation.fm.o1.common.util.HrefUtil;

/**
 * Converts the {@code href} alarm property value to a MO FDN.
 * <p>
 * Here is an example of how the converter should be used:
 * <p>
 * {@code
 *   <t:convert-set-model-property oid="href" mappedBy="managedObjectInstance" converter="ManagedObjectInstanceConverter"/>
 * }
 * <p>
 * The converter would convert a href value such as the following:
 * <p>
 * "https://cucp.MeContext.skylight.SubNetwork/ManagedElement=1/GNBCUCPFunction=1"
 * <p>
 * to:
 * <p>
 * "SubNetwork=skylight,MeContext=cucp,ManagedElement=1,GNBCUCPFunction=1"
 */
public class ManagedObjectInstanceConverter extends AbstractModelConverter<String, String> {

    public ManagedObjectInstanceConverter(final String[] args, final JellyContext context) {
        super(args, context);
    }

    @Override
    public String convert(final String hrefValue, final Field dstField) {
        final String dnPrefix = HrefUtil.extractDnPrefix(hrefValue);
        final String ldn = HrefUtil.extractLdn(hrefValue);

        return FdnUtil.createFdn(dnPrefix, ldn);
    }
}
