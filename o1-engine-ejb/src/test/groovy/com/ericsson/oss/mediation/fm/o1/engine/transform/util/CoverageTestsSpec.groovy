package com.ericsson.oss.mediation.fm.o1.engine.transform.util

import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.mediation.fm.o1.engine.transform.MissingArgumentException
import com.ericsson.oss.mediation.fm.o1.engine.transform.ModelTransformerException
import com.ericsson.oss.mediation.fm.o1.engine.transform.ModelTransformerKey
import com.ericsson.oss.mediation.fm.o1.engine.transform.OidKey
import com.ericsson.oss.mediation.fm.o1.engine.transform.converter.ModelConverterException
import com.ericsson.oss.mediation.fm.o1.engine.transform.tag.ContextVariableNotFoundException

/**
 * Tests here are mainly for code coverage of hard to reach areas.
 * The main flow is tested from these test classes:
 * <pre>
 *  {@code com.ericsson.oss.mediation.fm.o1.engine.service.O1AlarmServiceImplSpec}
 *  {@code com.ericsson.oss.mediation.fm.o1.engine.transform.TransformManagerSpec}
 */
class CoverageTestsSpec extends CdiSpecification {

    def "Test ContextVariableNotFoundException"() {

        when: "ContextVariableNotFoundException is created with variable set"
            ContextVariableNotFoundException contextVariableNotFoundException = new ContextVariableNotFoundException("var1")

        then: "variable can be retrieved"
            assert contextVariableNotFoundException.getVariable() == "var1"
    }

    def "Test MissingArgumentException"() {

        when: "MissingArgumentException is created"
            MissingArgumentException missingArgumentException = new MissingArgumentException("arg1", String.class)

        then:
            assert missingArgumentException.getMessage().contains("You must define an argument called 'arg1' for this class: java.lang.String")
    }

    def "Test ModelConverterException"() {

        when: "ModelConverterException is created"
            def cause = new IllegalArgumentException()
            ModelConverterException modelConverterException = new ModelConverterException("foo", cause)

        then:
            assert modelConverterException.getMessage().contains("foo")
            assert modelConverterException.getCause() == cause
    }

    def "Test ModelTransformerException"() {

        when:"ModelTransformerException is created with just a message"
            ModelTransformerException modelTransformerException = new ModelTransformerException("foo")

        then:
            assert modelTransformerException.getMessage() == "foo"

        when:"ModelTransformerException is created with just a cause"
            def cause = new IllegalArgumentException()
            modelTransformerException = new ModelTransformerException(cause)

        then:
            assert modelTransformerException.getCause() == cause

        when: "ModelTransformerException is created with message and cause"
            cause = new IllegalArgumentException()
            modelTransformerException = new ModelTransformerException("foo", cause)

        then: ""
            assert modelTransformerException.getMessage().contains("foo")
            assert modelTransformerException.getCause() == cause
    }


    def "Test ModelTransformerKey"() {

        when: "ModelTransformerKey is created"
            ModelTransformerKey modelTransformerKey = new ModelTransformerKey("id","version")

        then:
            assert modelTransformerKey.getVersion() == "version"
            assert modelTransformerKey.getId() == "id"
    }

    def "Test oidKey"() {

        when: "OidKey is created"
            OidKey oidKey = new OidKey("oid")

        then:
            assert oidKey.getOid() == "oid"
            assert oidKey.toString() == "OidKey(oid=oid)"
    }

}
