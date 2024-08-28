package com.ericsson.oss.mediation.fm.o1.engine.service.config

import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import org.junit.Rule

import javax.inject.Inject
import java.nio.file.Path

/**
 * The main entry test class for testing mediation configuration.
 */
class MediationConfigurationBeanSpec extends CdiSpecification {

    private static final FILE_SEP = System.getProperty("file.separator")

    @ObjectUnderTest
    MediationConfigurationBean mediationConfigurationBean

    @Inject
    MediationConfiguration o1MediationConfiguration

    @Rule
    LocalConfigRepoRule localConfigRepoRule

    def "Test initializing the network element configuration"() {
        given: "Network element id for type O1 and version 1.0.0"
            NetworkElementId networkElementId = new NetworkElementId("O1", "1.0.0")

        and: "the mediationConfigurationBean is initialized (postcontruct has been called)"
            mediationConfigurationBean != null

        when: "get configuration for the network element id"
            def networkElementConfiguration = mediationConfigurationBean.getConfiguration(networkElementId)

        then: "the network configuration is returned"
            assert networkElementConfiguration.getNetworkElementId() == networkElementId
            assert networkElementConfiguration.getTransformerFile().toString().contains("transformer/transformer-config.xml")

        when: "get jar path is retrieved"
            List<Path> actualJarPaths = o1MediationConfiguration.getJarPaths()

        then: "the path of the jar is returned"
            def jarPath = actualJarPaths.get(0).toString()
            assert jarPath.contains("o1-engine-ejb" + FILE_SEP + "target" + FILE_SEP + "med_config")
            assert jarPath.contains(".jar")
    }
}
