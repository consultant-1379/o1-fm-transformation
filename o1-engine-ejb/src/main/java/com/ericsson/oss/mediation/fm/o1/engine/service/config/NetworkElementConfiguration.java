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
import java.net.URL;

public class NetworkElementConfiguration implements Serializable {

    private static final long serialVersionUID = 1L;

    private final NetworkElementId networkElementId;
    private URL transformerFile;

    public NetworkElementConfiguration(final NetworkElementId networkElementId) {
        this.networkElementId = networkElementId;
    }

    public NetworkElementId getNetworkElementId() {
        return networkElementId;
    }

    public URL getTransformerFile() {
        return transformerFile;
    }

    public void setTransformerFile(final URL transformerFile) {
        this.transformerFile = transformerFile;
    }

}
