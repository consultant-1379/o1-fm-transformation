package com.ericsson.oss.mediation.fm.o1.engine.transform


import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.mediation.fm.o1.engine.service.config.LocalConfigRepoRule
import com.ericsson.oss.mediation.fm.o1.engine.service.config.MediationConfiguration
import com.ericsson.oss.mediation.fm.o1.engine.service.config.NetworkElementConfiguration
import com.ericsson.oss.mediation.fm.o1.engine.service.config.NetworkElementId
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import org.junit.Rule

import javax.inject.Inject
import java.lang.reflect.Field

class TransformManagerSpec extends CdiSpecification {

    @ObjectUnderTest
    TransformManager transformManager

    @Inject
    MediationConfiguration mediationConfiguration

    @Rule
    LocalConfigRepoRule localConfigRepoRule

    def "Test when mapper key is not found in the registry"() {

        given: "the mapper registry does not contain mapper for 2.0.0 version"
            Map<NetworkElementId, ModelTransformerKey> mapperRegistry = new HashMap<>()
            mapperRegistry.put(new NetworkElementId("O1", "2.0.0"), null)
            setFieldValue(TransformManager, transformManager, "mapperRegistry", mapperRegistry)

        when: "transform is called"
            transformManager.transformAlarm(ImmutableMap.of("notificationType","notifyNewAlarm"))

        then: "an exception for not finding a matching mapper is thrown"
            def e = thrown(TransformerException.class)
            assert e.message.contains("ModelTransformerKey not found for type O1 and version 1.0.0")
    }

    def "Test adding transformer config when url is empty"() {

        given: "A network element configuration with empty transformer url"
            def networkElementConfiguration = new NetworkElementConfiguration(new NetworkElementId("O1", "2.0.0"))
            assert networkElementConfiguration.getTransformerFile() == null

        when: "adding the transformer config"
            transformManager.addTransformerConfigurations(ImmutableList.of(networkElementConfiguration))

        then: "an exception is thrown"
            def e = thrown(IllegalArgumentException.class)
            assert e.message.contains("transformerUrl cannot be null")
    }

    def "Test adding transformer config with invalid URL"() {

        given: "A network element configuration with invalid transformer url"
            def mockNetworkElementConfiguration = mock(NetworkElementConfiguration)
            1 * mockNetworkElementConfiguration.getTransformerFile() >> new URL("http://<>")

        when: "adding the transformer config"
            transformManager.addTransformerConfigurations(ImmutableList.of(mockNetworkElementConfiguration))

        then: "an exception is thrown"
            def e = thrown(IllegalArgumentException.class)
            assert e.message.contains("Invalid url")
    }



    private void setFieldValue(Class clazz, Object objectInstance, String fieldName, Object newValue) {
        Field field = clazz.getDeclaredField(fieldName)
        field.setAccessible(true)
        field.set(objectInstance, newValue)
    }
}
