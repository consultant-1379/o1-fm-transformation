package com.ericsson.oss.mediation.fm.o1.engine.transform.converter.impl

import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.mediation.fm.o1.engine.transform.converter.ModelConverterException
import org.apache.commons.jelly.JellyContext

import java.lang.reflect.Field

/**
 * Tests here are mainly for negative and alternate scenarios that are difficult or not worth testing e2e with the CDI test framework.
 * <p>
 * The main flow is tested from these test classes:
 * <pre>
 * {@code com.ericsson.oss.mediation.fm.o1.engine.service.O1AlarmServiceImplSpec}
 * {@code com.ericsson.oss.mediation.fm.o1.engine.transform.TransformManagerSpec}
 */
class O1DateModelConverterSpec extends CdiSpecification {

    private static String[] args = ["format=yyyyMMddHHmmss.SSS"]
    private static Field field = TestClass.class.getDeclaredField("testField")

    def "Test O1DateModelConverter with null value"() {

        given: "O1DateModelConverter is created"
            final O1DateModelConverter o1DateModelConverter = new O1DateModelConverter(args, new JellyContext())

        when: ""
            String result = o1DateModelConverter.convert(null, field)

        then: ""
            assert result == null
    }

    def "Test O1DateModelConverter with invalid value"() {

        given: "O1DateModelConverter is created"
            final O1DateModelConverter o1DateModelConverter = new O1DateModelConverter(args, new JellyContext())

        when: "the date to convert is missing the seconds"
            String result = o1DateModelConverter.convert("2023-09-06T11:32", field)

        then: ""
            def e = thrown(ModelConverterException.class)
            assert e.message.contains("Invalid input value")
    }

    // Note that test for more than three milliseconds is in e2e test case in O1AlarmServiceImplSpec
    def "Test O1DateModelConverter with valid value with less than three millisecond digits"() {

        given: "O1DateModelConverter is created"
            final O1DateModelConverter o1DateModelConverter = new O1DateModelConverter(args, new JellyContext())

        when: "the date to convert has one millisecond digit"
            String result = o1DateModelConverter.convert("2023-09-06T11:32:01.7Z", field)

        then: "resulting date contains default milisecond value with zeros padded"
            assert result == "20230906113201.007"
    }

    def "Test O1DateModelConverter with valid value with no milliseconds"() {

        given: "O1DateModelConverter is created"
            final O1DateModelConverter o1DateModelConverter = new O1DateModelConverter(args, new JellyContext())

        when: "the date to convert has no milliseconds"
            String result = o1DateModelConverter.convert("2023-09-06T11:32:01Z", field)

        then: "resulting date contains default 000 for miliseconds"
            assert result == "20230906113201.000"
    }
}

class TestClass {
    String testField
}
