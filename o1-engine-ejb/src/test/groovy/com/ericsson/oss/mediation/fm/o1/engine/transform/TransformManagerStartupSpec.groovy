package com.ericsson.oss.mediation.fm.o1.engine.transform


import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.mediation.fm.o1.engine.service.config.LocalConfigRepoRule
import org.junit.Rule

class TransformManagerStartupSpec extends CdiSpecification {

    @ObjectUnderTest
    TransformManagerStartup transformManagerStartup

    @Rule
    LocalConfigRepoRule localConfigRepoRule

    def "Test loading of TransformManager" () {

        when: "TransformManagerStartup has been initialized"
            transformManagerStartup != null

        then: "The TransformManager has been triggered to load"
            assert transformManagerStartup.isInitialized() == true
    }
}
