package com.ericsson.oss.mediation.fm.o1.engine.nodesuspender.cluster

import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.mediation.fm.o1.engine.nodesuspender.ClusterMembershipObserver
import com.ericsson.oss.mediation.fm.o1.engine.nodesuspender.MasterInterceptor

import javax.interceptor.InvocationContext

class MasterInterceptorSpec extends CdiSpecification {

    @MockedImplementation
    ClusterMembershipObserver cluster

    @ObjectUnderTest
    MasterInterceptor interceptor

    InvocationContext ctx = Mock(InvocationContext)

    def "Test context can proceed when the cluster member is a master"() {
        given: "The cluster member is a master"
            cluster.isMaster() >> true

        when: "Interceptor receives the context"
            def obj = interceptor.membership(ctx)

        then: "Context is allowed to proceed"
            1 * ctx.proceed() >> "RESULT"
            obj == "RESULT"
    }

    def "Test context cannot proceed when the cluster member is not a master"() {
        given: "The cluster member is standby"
            cluster.isMaster() >> false

        when: "Interceptor receives the context"
            def obj = interceptor.membership(ctx)

        then: "Context is not proceeded"
            0 * ctx.proceed()
            !obj
    }
}