package com.ericsson.oss.mediation.fm.o1.engine.transform.tag

import org.apache.commons.jelly.JellyContext
import org.apache.commons.jelly.JellyTagException
import org.apache.commons.jelly.MissingAttributeException

import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.mediation.fm.o1.engine.transform.Constants

/**
 * Tests here are mainly for negative and alternate scenarios that are difficult or not worth testing e2e with the CDI test framework.
 * <p>
 * The main flow is tested from these test classes:
 * <pre>
 *  {@code com.ericsson.oss.mediation.fm.o1.engine.service.O1AlarmServiceImplSpec}
 *  {@code com.ericsson.oss.mediation.fm.o1.engine.transform.TransformManagerSpec}
 */
class EvaluateSetModelPropertyTagSpec extends CdiSpecification {

    @ObjectUnderTest
    EvaluateSetModelPropertyTag evaluateSetModelPropertyTag

    def "Test not setting mappedBy arg causes exception" () {

        given: "mappedBy is empty"
            assert evaluateSetModelPropertyTag.getMappedBy() == null

        when:
            evaluateSetModelPropertyTag.doTag()

        then: "MissingAttributeException is thrown"
            def e = thrown(MissingAttributeException.class)
            assert e.message.contains("You must define an attribute called 'mappedBy' for this tag")
    }

    def "Test not setting evaluator arg causes exception" () {

        given: "mappedBy is set"
            evaluateSetModelPropertyTag.setMappedBy("map1")

        when:
            evaluateSetModelPropertyTag.doTag()

        then: "MissingAttributeException is thrown"
            def e = thrown(MissingAttributeException.class)
            assert e.message.contains("You must define an attribute called 'evaluator' for this tag")
    }

    def "Test when getting object value causes exception" () {

        given: "required parameters are set"
            evaluateSetModelPropertyTag.setMappedBy("map1")
            evaluateSetModelPropertyTag.setEvaluator("evaluatorName")

        and: "the Jelly context exists with a registered evaluator"
            def mockJellyContext = mock(JellyContext)
            mockJellyContext.getVariable(Constants.REGISTERED_EVALUATORS) >> ['evaluatorName' : 'evaluatorClass']
            evaluateSetModelPropertyTag.setContext(mockJellyContext)

        when:
            evaluateSetModelPropertyTag.doTag()

        then: "JellyTagException is thrown"
            def e = thrown(JellyTagException.class)
            assert e.message.contains("Error executing EvaluateSetModelPropertyTag.")
    }
}
