package com.ericsson.oss.mediation.o1.yang.handlers.netconf.stub;

import com.ericsson.oss.mediation.adapter.netconf.jca.api.operation.NetconfOperationConnection;
import com.ericsson.oss.mediation.adapter.netconf.jca.api.operation.NetconfOperationRequest;
import com.ericsson.oss.mediation.adapter.netconf.jca.api.operation.NetconfOperationResult;
import com.ericsson.oss.mediation.o1.yang.handlers.netconf.exception.MOHandlerException;
import com.ericsson.oss.mediation.util.netconf.api.Capability;
import com.ericsson.oss.mediation.util.netconf.api.Datastore;
import com.ericsson.oss.mediation.util.netconf.api.Filter;
import com.ericsson.oss.mediation.util.netconf.api.NetconfConnectionStatus;
import com.ericsson.oss.mediation.util.netconf.api.NetconfManager;
import com.ericsson.oss.mediation.util.netconf.api.NetconfResponse;
import com.ericsson.oss.mediation.util.netconf.api.NetconfResponseListener;
import com.ericsson.oss.mediation.util.netconf.api.editconfig.DefaultOperation;
import com.ericsson.oss.mediation.util.netconf.api.editconfig.ErrorOption;
import com.ericsson.oss.mediation.util.netconf.api.editconfig.TestOption;
import com.ericsson.oss.mediation.util.netconf.api.exception.NetconfManagerException

public class NetconfConnectionManagerForTestsThrowsException implements NetconfOperationConnection, NetconfManager {
    @Override
    public NetconfOperationResult executeXAResourceOperation(final NetconfOperationRequest netconfOperationRequest) {
        throw new MOHandlerException("Test Exception");
    }

    @Override
    public void setWaitForResponse(final boolean b) {

    }

    @Override
    public NetconfResponse connect() throws NetconfManagerException {
        return null;
    }

    @Override
    public NetconfResponse disconnect() throws NetconfManagerException {
        return null;
    }

    @Override
    public NetconfConnectionStatus getStatus() {
        return null;
    }

    @Override
    public NetconfResponse get() throws NetconfManagerException {
        return null;
    }

    @Override
    public NetconfResponse get(final Filter filter) throws NetconfManagerException {
        String testString = "<ManagedElement\n" +
                "\txmlns=\"urn:3gpp:sa5:_3gpp-common-managed-element\">\n" +
                "\t<ExternalDomain\n" +
                "\t\txmlns=\"urn:rdns:com:ericsson:oammodel:ericsson-external-domain-cr\">\n" +
                "\t\t<id>1</id>\n" +
                "\t</ExternalDomain>\n" +
                "</ManagedElement>\n";
        NetconfResponse response = new NetconfResponse();
        response.setData(testString);
        return response;
    }

    @Override
    public NetconfResponse get(final Filter filter, final String s) throws NetconfManagerException {
        return null;
    }

    @Override
    public NetconfResponse get(final NetconfResponseListener netconfResponseListener, final Datastore datastore, final Filter filter)
            throws NetconfManagerException {
        return null;
    }

    @Override
    public NetconfResponse get(final NetconfResponseListener netconfResponseListener, final Datastore datastore, final Filter filter, final String s)
            throws NetconfManagerException {
        return null;
    }

    @Override
    public NetconfResponse getConfig() throws NetconfManagerException {
        return null;
    }

    @Override
    public NetconfResponse getConfig(final Datastore datastore) throws NetconfManagerException {
        return null;
    }

    @Override
    public NetconfResponse getConfig(final Datastore datastore, final Filter filter) throws NetconfManagerException {
        return null;
    }

    @Override
    public NetconfResponse getConfig(final NetconfResponseListener netconfResponseListener, final Datastore datastore, final Filter filter)
            throws NetconfManagerException {
        return null;
    }

    @Override
    public NetconfResponse lock(final Datastore datastore) throws NetconfManagerException {
        return null;
    }

    @Override
    public NetconfResponse unlock(final Datastore datastore) throws NetconfManagerException {
        return null;
    }

    @Override
    public NetconfResponse validate(final String s) throws NetconfManagerException {
        return null;
    }

    @Override
    public NetconfResponse validate(final Datastore datastore) throws NetconfManagerException {
        return null;
    }

    @Override
    public NetconfResponse commit() throws NetconfManagerException {
        return null;
    }

    @Override
    public NetconfResponse discardChanges() throws NetconfManagerException {
        return null;
    }

    @Override
    public NetconfResponse editConfig(final Datastore datastore, final String s) throws NetconfManagerException {
        return null;
    }

    @Override
    public NetconfResponse editConfig(final Datastore datastore, final DefaultOperation defaultOperation, final String s)
            throws NetconfManagerException {
        return null;
    }

    @Override
    public NetconfResponse editConfig(final Datastore datastore, final ErrorOption errorOption, final String s) throws NetconfManagerException {
        return null;
    }

    @Override
    public NetconfResponse editConfig(final Datastore datastore, final TestOption testOption, final String s) throws NetconfManagerException {
        return null;
    }

    @Override
    public NetconfResponse editConfig(final Datastore datastore, final ErrorOption errorOption, final TestOption testOption, final String s)
            throws NetconfManagerException {
        return null;
    }

    @Override
    public NetconfResponse editConfig(final Datastore datastore, final DefaultOperation defaultOperation, final ErrorOption errorOption,
                                      final TestOption testOption,
                                      final String s) throws NetconfManagerException {
        return null;
    }

    @Override
    public NetconfResponse killSession(final String s) throws NetconfManagerException {
        return null;
    }

    @Override
    public NetconfResponse createSubscription(final String s, final Filter filter, final String s1, final String s2) throws NetconfManagerException {
        return null;
    }

    @Override
    public Collection<Capability> getAllActiveCapabilities() {
        return null;
    }

    @Override
    public Collection<Capability> getAllNodeCapabilities() {
        return null;
    }

    @Override
    public String getSessionId() {
        return null;
    }

    @Override
    public NetconfResponse action(final String s) throws NetconfManagerException {
        return null;
    }

    @Override
    public NetconfResponse action(final String s, final String s1, final String... strings) throws NetconfManagerException {
        return null;
    }

    @Override
    public NetconfResponse action(final NetconfResponseListener netconfResponseListener, final String s, final String s1)
            throws NetconfManagerException {
        return null;
    }

    @Override
    public NetconfResponse copyConfig(final String s, final String s1) throws NetconfManagerException {
        return null;
    }

    @Override
    public NetconfResponse customOperation(final String s) throws NetconfManagerException {
        return null;
    }

    @Override
    public NetconfResponse customOperation(final String s, final boolean b) throws NetconfManagerException {
        return null;
    }
}
