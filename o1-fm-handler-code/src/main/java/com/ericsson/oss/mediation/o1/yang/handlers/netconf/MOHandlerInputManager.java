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

package com.ericsson.oss.mediation.o1.yang.handlers.netconf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.mediation.netconf.session.api.handler.AbstractNetconfSessionHandlerInputManager;
import com.ericsson.oss.mediation.util.netconf.api.NetconManagerConstants;
import com.ericsson.oss.mediation.util.netconf.api.NetconfManager;

/**
 * This class is responsible for parsing the input headers for YANG handlers
 */
public class MOHandlerInputManager extends AbstractNetconfSessionHandlerInputManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(MOHandlerInputManager.class);

    private NetconfManager netconfManager;

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

    @Override
    protected void validateHandlerAttributes() throws IllegalArgumentException {
        netconfManager = getAttribute(NetconManagerConstants.NETCONF_MANAGER_ATTR, headers, true);
    }

    NetconfManager getNetconfManager() {
        return netconfManager;
    }
}
