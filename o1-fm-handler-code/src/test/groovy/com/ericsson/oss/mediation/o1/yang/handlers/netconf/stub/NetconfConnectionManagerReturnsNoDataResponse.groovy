package com.ericsson.oss.mediation.o1.yang.handlers.netconf.stub;

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
import com.ericsson.oss.mediation.util.netconf.api.exception.NetconfManagerException;

import java.util.Collection;

public class NetconfConnectionManagerReturnsNoDataResponse implements NetconfManager {
    @Override
    public void setWaitForResponse(boolean b) {

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
        return new NetconfResponse();
    }

    @Override
    public NetconfResponse get(Filter filter, String s) throws NetconfManagerException {
        return null;
    }

    @Override
    public NetconfResponse get(NetconfResponseListener netconfResponseListener, Datastore datastore, Filter filter) throws NetconfManagerException {
        return null;
    }

    @Override
    public NetconfResponse get(NetconfResponseListener netconfResponseListener, Datastore datastore, Filter filter, String s) throws NetconfManagerException {
        return null;
    }

    @Override
    public NetconfResponse getConfig() throws NetconfManagerException {
        return null;
    }

    @Override
    public NetconfResponse getConfig(Datastore datastore) throws NetconfManagerException {
        return null;
    }

    @Override
    public NetconfResponse getConfig(Datastore datastore, Filter filter) throws NetconfManagerException {
        return null;
    }

    @Override
    public NetconfResponse getConfig(NetconfResponseListener netconfResponseListener, Datastore datastore, Filter filter) throws NetconfManagerException {
        return null;
    }

    @Override
    public NetconfResponse lock(Datastore datastore) throws NetconfManagerException {
        return null;
    }

    @Override
    public NetconfResponse unlock(Datastore datastore) throws NetconfManagerException {
        return null;
    }

    @Override
    public NetconfResponse validate(String s) throws NetconfManagerException {
        return null;
    }

    @Override
    public NetconfResponse validate(Datastore datastore) throws NetconfManagerException {
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
    public NetconfResponse editConfig(Datastore datastore, String s) throws NetconfManagerException {
        return null;
    }

    @Override
    public NetconfResponse editConfig(Datastore datastore, DefaultOperation defaultOperation, String s) throws NetconfManagerException {
        return null;
    }

    @Override
    public NetconfResponse editConfig(Datastore datastore, ErrorOption errorOption, String s) throws NetconfManagerException {
        return null;
    }

    @Override
    public NetconfResponse editConfig(Datastore datastore, TestOption testOption, String s) throws NetconfManagerException {
        return null;
    }

    @Override
    public NetconfResponse editConfig(Datastore datastore, ErrorOption errorOption, TestOption testOption, String s) throws NetconfManagerException {
        return null;
    }

    @Override
    public NetconfResponse editConfig(Datastore datastore, DefaultOperation defaultOperation, ErrorOption errorOption, TestOption testOption, String s) throws NetconfManagerException {
        return null;
    }

    @Override
    public NetconfResponse killSession(String s) throws NetconfManagerException {
        return null;
    }

    @Override
    public NetconfResponse createSubscription(String s, Filter filter, String s1, String s2) throws NetconfManagerException {
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
    public NetconfResponse action(String s) throws NetconfManagerException {
        return null;
    }

    @Override
    public NetconfResponse action(String s, String s1, String... strings) throws NetconfManagerException {
        return null;
    }

    @Override
    public NetconfResponse action(NetconfResponseListener netconfResponseListener, String s, String s1) throws NetconfManagerException {
        return null;
    }

    @Override
    public NetconfResponse copyConfig(String s, String s1) throws NetconfManagerException {
        return null;
    }

    @Override
    public NetconfResponse customOperation(String s) throws NetconfManagerException {
        return null;
    }

    @Override
    public NetconfResponse customOperation(String s, boolean b) throws NetconfManagerException {
        return null;
    }
}
