package com.ericsson.oss.mediation.fm.o1.engine.transform.util

import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.mediation.fm.o1.engine.transform.TransformerException
import org.w3c.dom.Element
import org.w3c.dom.Node

/**
 * Tests here are mainly for code coverage of hard to reach areas.
 * The main flow is tested from these test classes:
 * <pre>
 *  {@code com.ericsson.oss.mediation.fm.o1.engine.service.O1AlarmServiceImplSpec}
 *  {@code com.ericsson.oss.mediation.fm.o1.engine.transform.TransformManagerSpec}
 */
class XmlUtilsSpec extends CdiSpecification {

    def "Test getNodeDump when exception from node"() {

        given: "An xml node misbehaves"
            def mockNode = mock(Node)
            _ * mockNode._ >> { throw new IllegalArgumentException() }

        when: "getNodeDump is called"
            XmlUtils.getNodeDump(mockNode)

        then: "TransformerException is thrown"
            def e = thrown(TransformerException.class)
            assert e.message.contains("Unable to print node")
    }

    def "Test elementToStream when exception from element"() {

        given: "An xml node misbehaves"
            def mockElement = mock(Element)
            _ * mockElement._ >> { throw new IllegalArgumentException() }

        when: "elementToStream is called"
            XmlUtils.elementToStream(mockElement, null)

        then: "TransformerException is thrown"
            def e = thrown(TransformerException.class)
            assert e.message.contains("Unable to convert element to stream")
    }



}
