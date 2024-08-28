package com.ericsson.oss.mediation.fm.o1.engine.transform.util

import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.mediation.fm.o1.engine.transform.TransformerException
import org.apache.commons.beanutils.converters.ConverterFacade

import java.lang.reflect.Method

/**
 * Tests here are mainly for code coverage of hard to reach areas.
 * The main flow is tested from these test classes:
 * <pre>
 *  {@code com.ericsson.oss.mediation.fm.o1.engine.service.O1AlarmServiceImplSpec}
 *  {@code com.ericsson.oss.mediation.fm.o1.engine.transform.TransformManagerSpec}
 */
class TransformerUtilsSpec extends CdiSpecification {

    def "Test get class name"() {

        given: "On object URL"
            URL validUrl = new URL("http://example.com")

        when: "getClassName with valid URL"
            String result = TransformerUtils.getClassName(validUrl)

        then: "java.net.URL is returned"
            assert result == "java.net.URL"

        when: "getClassName with null URL"
            result = TransformerUtils.getClassName(null)

        then: "null is returned"
            assert result == null
    }

    def "test loading class not on classpath"() {

        given: "Unknown class on classpath"
            def className = "Unknown.class"
            def mockClassLoader = mock(ClassLoader)
            1 * mockClassLoader.loadClass(_) >> { throw new ClassNotFoundException()}

        when: "createInstance"
            TransformerUtils.createInstance(className, null, mockClassLoader, null)

        then: "error loading exception is thrown"
            def e = thrown(TransformerException.class)
            assert e.message.contains("Error loading instance of class: " + className.replaceFirst(".class", ""))
    }

    def "test convert when source type is null "() {

        given: "A null source object"
            def srcObj = null

        when: "convert called"
            def result = TransformerUtils.convert(srcObj, String.class)

        then: "null is converted to null"
            assert result == null
    }

    def "test convert when destination type is null"() {

        given: "A null dest object"
            def srcObj = "fooString"
            def destType = null

        when: "convert called"
            TransformerUtils.convert(srcObj, destType)

        then: "String is converted to null"
            def e = thrown(IllegalArgumentException.class)
            assert e.message.contains("dstType is " + destType)
    }

    def "test findConverter method"() {

        given: "getConverted is made accessible"
            def srcType = String.class
            def dstType = String[].class

        when: "getConverted method is called"
            Object converter = TransformerUtils.findConverter(srcType, dstType)

        then: "Converter is returned"
            assert converter instanceof ConverterFacade
    }

    def "test getConverted method"() {

        given: "getConverted is made accessible"
            Class<String> clazz = TransformerUtils.class
            Method getConverted = clazz.getDeclaredMethod("getConverted", Class.class, Object.class)
            getConverted.setAccessible(true)

            def srcObj = 1
            def destType = String.class

        when: "getConverted method is called"
            Object result = getConverted.invoke(null, destType, srcObj)

        then: "Integer object is converted to String value"
            assert result == "1"
    }
}
