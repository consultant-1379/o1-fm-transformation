package com.ericsson.oss.mediation.fm.o1.engine.transform.tag

import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import org.apache.commons.jelly.MissingAttributeException

/**
 * Tests here are mainly for code coverage of hard to reach areas.
 * The main flow is tested from these test classes:
 * <pre>
 *  {@code com.ericsson.oss.mediation.fm.o1.engine.service.O1AlarmServiceImplSpec}
 *  {@code com.ericsson.oss.mediation.fm.o1.engine.transform.TransformManagerSpec}
 */
class ConvertSetModelPropertyTagSpec extends CdiSpecification {

    @ObjectUnderTest
    ConvertSetModelPropertyTag convertSetModelPropertyTag

    def "Test not setting mappedBy arg causes exception" () {

        given: "mappedBy is empty"
            assert convertSetModelPropertyTag.getMappedBy() == null

        when:
            convertSetModelPropertyTag.doTag()

        then: "MissingAttributeException is thrown"
            def e = thrown(MissingAttributeException.class)
            assert e.message.contains("You must define an attribute called 'mappedBy' for this tag")
    }

    def "Test not setting oid attribute causes exception" () {

        given: "mappedBy is set"
            convertSetModelPropertyTag.setMappedBy("map1")

        and: "oid is not set"
            assert convertSetModelPropertyTag.getOid() == null

        when:
            convertSetModelPropertyTag.doTag()

        then: "MissingAttributeException is thrown"
            def e = thrown(MissingAttributeException.class)
            assert e.message.contains("oid")
    }

    def "Test not setting converter attribute causes exception" () {

        given: "mappedBy and oid is set"
            convertSetModelPropertyTag.setMappedBy("map1")
            convertSetModelPropertyTag.setOid("oid")

        and: "converter is not set"
            assert convertSetModelPropertyTag.getConverter() == null

        when:
            convertSetModelPropertyTag.doTag()

        then: "MissingAttributeException is thrown"
            def e = thrown(MissingAttributeException.class)
            assert e.message.contains("converter")
    }
}
