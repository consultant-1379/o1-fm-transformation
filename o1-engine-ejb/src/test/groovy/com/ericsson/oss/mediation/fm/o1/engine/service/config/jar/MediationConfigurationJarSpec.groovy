package com.ericsson.oss.mediation.fm.o1.engine.service.config.jar

import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.mediation.fm.o1.engine.service.config.LocalConfigRepoRule
import com.ericsson.oss.mediation.fm.o1.engine.service.config.MediationConfigurationException
import com.ericsson.oss.mediation.fm.o1.engine.service.config.xml.MediationConfigurationXmlLoader
import com.ericsson.oss.mediation.fm.o1.engine.service.config.xml.NetworkElementConfig
import com.ericsson.oss.mediation.fm.o1.engine.service.config.xml.O1MediationConfig
import org.junit.Rule
import org.mockito.internal.util.reflection.Whitebox

/**
 * Test class to execute test cases that could not be directly tested through the {@class MediationConfigurationBean}
 * (meaning its cleaner code to test them through {@class MediationConfigurationJar} directly rather than having to
 * use reflection to set mocks or some other way to achieve the desired result).
 */
class MediationConfigurationJarSpec extends CdiSpecification {

    @ObjectUnderTest
    MediationConfigurationJar mediationConfigurationJar

    @Rule
    LocalConfigRepoRule localConfigRepoRule

    List<NetworkElementConfig> listNeConfig = new ArrayList<>()

    def "Test reading configuration when no network element config tags exist" () {

        given: "the mediation config does not contain any network element configuration tags"
            MediationConfigurationXmlLoader medConfigXmlLoaderMock = setMockXmlLoader()

            def o1MedConfigMock = mock(O1MediationConfig)
            1 * medConfigXmlLoaderMock.load(*_) >> o1MedConfigMock
            1 * o1MedConfigMock.getNetworkElementConfigurations() >> null

        when: "creating the network element configuration"
            mediationConfigurationJar.createNetworkElementConfigurations(localConfigRepoRule.localRepoPath, localConfigRepoRule.getTransformationJarPath())

        then: "an exception occurs"
            def e = thrown(MediationConfigurationException.class)
            assert e.message.contains("Error creating NetworkElementConfiguration from jarPath")
    }

    def "Test reading configuration when no type exists" () {

        given: "the mediation config does not contain any network element configuration tags"
            MediationConfigurationXmlLoader medConfigXmlLoaderMock = setMockXmlLoader()
            def o1MedConfigMock = mock(O1MediationConfig)
            def networkElementConfigMock = mock(NetworkElementConfig)

            1 * medConfigXmlLoaderMock.load(*_) >> o1MedConfigMock
            1 * o1MedConfigMock.getNetworkElementConfigurations() >> listNeConfig
            listNeConfig.add(networkElementConfigMock)
            1 * networkElementConfigMock.getType() >> null

        when: "getting the network element configuration"
            mediationConfigurationJar.createNetworkElementConfigurations(localConfigRepoRule.localRepoPath, localConfigRepoRule.getTransformationJarPath())

        then: "an exception occurs"
            def e = thrown(MediationConfigurationException.class)
            assert e.getCause().message.contains("type is null processing jarPath")
    }

    def "Test reading config when no version exists" () {

        given: "the mediation config does not contain any version tags"
            MediationConfigurationXmlLoader medConfigXmlLoaderMock = setMockXmlLoader()
            def o1MedConfigMock = mock(O1MediationConfig)
            def networkElementConfigMock = mock(NetworkElementConfig)

            1 * medConfigXmlLoaderMock.load(*_) >> o1MedConfigMock
            1 * o1MedConfigMock.getNetworkElementConfigurations() >> listNeConfig
            listNeConfig.add(networkElementConfigMock)
            1 * networkElementConfigMock.getType() >> "O1"
            1 * networkElementConfigMock.getVersion() >> null

        when: "getting the network element configuration"
            mediationConfigurationJar.createNetworkElementConfigurations(localConfigRepoRule.localRepoPath, localConfigRepoRule.getTransformationJarPath())

        then: "an exception occurs"
            def e = thrown(MediationConfigurationException.class)
            assert e.getCause().message.contains("version is null processing jarPath")
    }

    def "Test reading config when transformer config tag is specified" () {

        given: "the mediation config contains a populated transformation config path tag"
            MediationConfigurationXmlLoader medConfigXmlLoaderMock = setMockXmlLoader()
            def o1MedConfigMock = mock(O1MediationConfig)
            def networkElementConfigMock = mock(NetworkElementConfig)

            1 * medConfigXmlLoaderMock.load(*_) >> o1MedConfigMock
            1 * o1MedConfigMock.getNetworkElementConfigurations() >> listNeConfig
            listNeConfig.add(networkElementConfigMock)
            _ * networkElementConfigMock.getType() >> "O1"
            _ * networkElementConfigMock.getVersion() >> "1.0.0"
            _ * networkElementConfigMock.getTransformerConfig() >> localConfigRepoRule.getTransformationJarPath()

        when: "getting the network element configuration"
            def neConfig = mediationConfigurationJar.createNetworkElementConfigurations(localConfigRepoRule.localRepoPath, localConfigRepoRule.getTransformationJarPath())

        then: "the transformer config is available"
            assert neConfig != null
            assert neConfig.size() == 1
            assert neConfig.get(0).getTransformerFile().toString().contains("/transformer/transformer-config.xml")
    }

    def "Test for non O1 mediation configuration jar"() {

        when: "jar does not contain o1-mediation-config.xml"
            mediationConfigurationJar.isO1MediationConfigurationJar(localConfigRepoRule.getLocalRepoPath())

        then: "not a valid jar"
            def e = thrown(MediationConfigurationException.class)
            assert e.message.contains("Error verifying jar")
    }

    def "Test hashcode,toString for O1MediationConfig,NetworkElementConfig"() {
        given:
            O1MediationConfig mediationConfig = new O1MediationConfig()
            O1MediationConfig mediationConfig2 = new O1MediationConfig()
            NetworkElementConfig networkElementConfig = new NetworkElementConfig()
            NetworkElementConfig networkElementConfig2 = new NetworkElementConfig()

        when:
            mediationConfig.hashCode()
            mediationConfig.toString()
            networkElementConfig.hashCode()
            networkElementConfig.toString()

        then:
            mediationConfig.equals(mediationConfig2)
            networkElementConfig.equals(networkElementConfig2)
    }

    private MediationConfigurationXmlLoader setMockXmlLoader() {
        def medConfigXmlLoaderMock = mock(MediationConfigurationXmlLoader)
        Whitebox.setInternalState(mediationConfigurationJar, "mediationConfigurationXmlLoader", medConfigXmlLoaderMock)
        medConfigXmlLoaderMock
    }
}
