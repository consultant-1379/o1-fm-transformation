package com.ericsson.oss.mediation.fm.o1.engine.transform.tag

import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.mediation.fm.o1.engine.transform.OidKey
import com.ericsson.oss.mediation.translator.model.EventNotification
import com.google.common.collect.ImmutableMap
import org.apache.commons.jelly.JellyContext
import org.apache.commons.jelly.JellyTagException
import org.apache.commons.jelly.MissingAttributeException
import org.apache.commons.jelly.TagSupport
import org.apache.commons.jelly.XMLOutput

import java.lang.reflect.Field

/**
 * Tests here are mainly for negative and alternate scenarios that are difficult or not worth testing e2e with the CDI test framework.
 * <p>
 * The main flow is tested from these test classes:
 * <pre>
 *  {@code com.ericsson.oss.mediation.fm.o1.engine.service.O1AlarmServiceImplSpec}
 *  {@code com.ericsson.oss.mediation.fm.o1.engine.transform.TransformManagerSpec}
 */
class SetModelPropertyTagSpec extends CdiSpecification {

    @ObjectUnderTest
    SetModelPropertyTag setModelPropertyTag

    def "Test not setting mappedBy arg causes exception" () {

        given: "mappedBy is empty"
            assert setModelPropertyTag.getMappedBy() == null

        when:
            setModelPropertyTag.doTag()

        then: "MissingAttributeException is thrown"
            def e = thrown(MissingAttributeException.class)
            assert e.message.contains("You must define an attribute called 'mappedBy' for this tag")
    }

    def "Test not setting oid attribute causes exception" () {

        given: "mappedBy is set"
            setModelPropertyTag.setMappedBy("map1")

        and: "oid is not set"
            assert setModelPropertyTag.getOid() == null

        when:
            setModelPropertyTag.doTag()

        then: "MissingAttributeException is thrown"
            def e = thrown(MissingAttributeException.class)
            assert e.message.contains("oid")
    }

    def "Test when getting object value causes exception" () {

        given: "mappedBy and oid is set"
            setModelPropertyTag.setMappedBy("map1")
            setModelPropertyTag.setOid("oid")

        when:
            setModelPropertyTag.doTag()

        then: "MissingAttributeException is thrown"
            def e = thrown(JellyTagException.class)
            assert e.message.contains("Error executing SetModelPropertyTag.")
    }

    def "Test doTag for XML output when srcObject is null"() {

        given:
            JellyContext jellyContext = new JellyContext()
            setFieldValue(TagSupport, setModelPropertyTag, "context", jellyContext)
            def mockXMLOutput = mock(XMLOutput)

        when: ""
            setModelPropertyTag.doTag(mockXMLOutput)

        then:
            def e = thrown(ContextVariableNotFoundException.class)
            assert e.message.contains("Context variable 'srcObject' not found")
    }

    def "Test doTag for XML output when dstObject is null"() {

        given:
            JellyContext jellyContext = new JellyContext()
            jellyContext.setVariable("srcObject", ImmutableMap.of("key", "val"))
            jellyContext.setVariable("dstObject",null)
            setFieldValue(TagSupport, setModelPropertyTag, "context", jellyContext)
            def mockXMLOutput = mock(XMLOutput)

        when: ""
            setModelPropertyTag.doTag(mockXMLOutput)

        then:
            def e = thrown(ContextVariableNotFoundException.class)
            assert e.message.contains("Context variable 'dstObject' not found")
    }

    def "Test doTag for XML output when srcValuesMap is null"() {

        given:
            JellyContext jellyContext = new JellyContext()
            jellyContext.setVariable("srcObject", ImmutableMap.of("key", "val"))
            jellyContext.setVariable("dstObject",new EventNotification())
            setFieldValue(TagSupport, setModelPropertyTag, "context", jellyContext)
            def mockXMLOutput = mock(XMLOutput)

        when: ""
            setModelPropertyTag.doTag(mockXMLOutput)

        then:
            def e = thrown(ContextVariableNotFoundException.class)
            assert e.message.contains("Context variable 'srcValuesMap' not found")
    }

    def "Test doTag for XML output successfull"() {

        given: "the input data is valid"
            setModelPropertyTag.setOid("oid")
            setModelPropertyTag.setMappedBy("probableCause")
            JellyContext jellyContext = new JellyContext()
            jellyContext.setVariable("srcObject", ImmutableMap.of("key", "val"))
            jellyContext.setVariable("dstObject", new EventNotification())
            jellyContext.setVariable("srcValuesMap", ImmutableMap.of("oid", "newVal"))
            setFieldValue(TagSupport, setModelPropertyTag, "context", jellyContext)
            def mockXMLOutput = mock(XMLOutput)

        when:
            setModelPropertyTag.doTag(mockXMLOutput)

        then:
            assert setModelPropertyTag.getAlarmObject() == ImmutableMap.of("key", "val")
            assert setModelPropertyTag.getOid() == "oid"
            assert setModelPropertyTag.getOidKey() == new OidKey("oid")
            assert setModelPropertyTag.getEventNotification() != null
            assert setModelPropertyTag.getAlarmData() == ImmutableMap.of("oid", "newVal")
            noExceptionThrown()
    }

    private void setFieldValue(Class clazz, Object objectInstance, String fieldName, Object newValue) {
        Field field = clazz.getDeclaredField(fieldName)
        field.setAccessible(true)
        field.set(objectInstance, newValue)
    }
}
