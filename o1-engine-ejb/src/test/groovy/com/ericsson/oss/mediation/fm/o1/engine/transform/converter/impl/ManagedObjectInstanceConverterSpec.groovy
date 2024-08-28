/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2023
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.mediation.fm.o1.engine.transform.converter.impl

import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.mediation.fm.o1.engine.transform.ModelTransformerException
import org.apache.commons.jelly.JellyContext

import java.lang.reflect.Field

/**
 * Tests here are mainly for negative and alternate scenarios that are difficult or not worth testing e2e with the CDI test framework.
 * <p>
 * The main flow is tested from these test classes:
 * <pre>
 *  {@code com.ericsson.oss.mediation.fm.o1.engine.service.O1AlarmServiceImplSpec}
 *  {@code com.ericsson.oss.mediation.fm.o1.engine.transform.TransformManagerSpec}
 */
class ManagedObjectInstanceConverterSpec extends CdiSpecification {


    def "Test ManagedObjectInstanceConverter with null value"() {

        given: "ManagedObjectInstanceConverter is created"
            String [] args = ["href=http://customerA.com/SubNetwork=SUB_A"]
            ManagedObjectInstanceConverter managedObjectInstanceConverter = new ManagedObjectInstanceConverter(args, new JellyContext())

        when: ""
            Field field = TestClass.class.getDeclaredField("testField")
            managedObjectInstanceConverter.convert(null, field)

        then: ""
            def e = thrown(IllegalArgumentException.class)
            assert e.message.contains("Alarm is missing mandatory 'href' field.")
    }

    def "Test ManagedObjectInstanceConverter with http"() {

        given: "ManagedObjectInstanceConverter is created"
            String [] args = ["http://cucp.MeContext.skylight.SubNetwork/ManagedElement=1/GNBCUCPFunction=1"]
            ManagedObjectInstanceConverter managedObjectInstanceConverter = new ManagedObjectInstanceConverter(args, new JellyContext())

        when: ""
            Field field = TestClass.class.getDeclaredField("testField")
            String result = managedObjectInstanceConverter.convert("http://cucp.MeContext.skylight.SubNetwork/ManagedElement=1/GNBCUCPFunction=1", field)

        then: ""
            assert result == "SubNetwork=skylight,MeContext=cucp,ManagedElement=1,GNBCUCPFunction=1"
    }

    def "Test ManagedObjectInstanceConverter with https"() {

        given: "ManagedObjectInstanceConverter is created"
            String [] args = ["https://cucp.MeContext.skylight.SubNetwork/ManagedElement=1/GNBCUCPFunction=1"]
            ManagedObjectInstanceConverter managedObjectInstanceConverter = new ManagedObjectInstanceConverter(args, new JellyContext())

        when: ""
            Field field = TestClass.class.getDeclaredField("testField")
            String result = managedObjectInstanceConverter.convert("https://cucp.MeContext.skylight.SubNetwork/ManagedElement=1/GNBCUCPFunction=1", field)

        then: ""
            assert result == "SubNetwork=skylight,MeContext=cucp,ManagedElement=1,GNBCUCPFunction=1"
    }

    def "Test ManagedObjectInstanceConverter when dnPrefix is just an MeContext"() {

         given: "ManagedObjectInstanceConverter is created"
            String [] args = ["http://cucp.MeContext/ManagedElement=1/GNBCUCPFunction=1"]
            ManagedObjectInstanceConverter managedObjectInstanceConverter = new ManagedObjectInstanceConverter(args, new JellyContext())

         when: ""
            Field field = TestClass.class.getDeclaredField("testField")
            String result = managedObjectInstanceConverter.convert("http://cucp.MeContext/ManagedElement=1/GNBCUCPFunction=1", field)

         then: ""
            assert result == "MeContext=cucp,ManagedElement=1,GNBCUCPFunction=1"
    }

    def "Test ManagedObjectInstanceConverter when dnPrefix has multiple SubNetworks"() {

         given: "ManagedObjectInstanceConverter is created"
            String [] args = ["http://cucp.MeContext.athlone.SubNetwork.ireland.SubNetwork/ManagedElement=1/GNBCUCPFunction=1"]
            ManagedObjectInstanceConverter managedObjectInstanceConverter = new ManagedObjectInstanceConverter(args, new JellyContext())

         when: ""
            Field field = TestClass.class.getDeclaredField("testField")
            String result = managedObjectInstanceConverter.convert("http://cucp.MeContext.athlone.SubNetwork.ireland.SubNetwork/ManagedElement=1/GNBCUCPFunction=1", field)

         then: ""
            assert result == "SubNetwork=ireland,SubNetwork=athlone,MeContext=cucp,ManagedElement=1,GNBCUCPFunction=1"
    }

    def "Test ManagedObjectInstanceConverter when href does not contain ldn"() {
 
         given: "ManagedObjectInstanceConverter is created"
            String [] args = ["href=http://cucp.MeContext.athlone.SubNetwork.ireland.SubNetwork"]
            ManagedObjectInstanceConverter managedObjectInstanceConverter = new ManagedObjectInstanceConverter(args, new JellyContext())

         when: ""
            Field field = TestClass.class.getDeclaredField("testField")
            String result = managedObjectInstanceConverter.convert("http://cucp.MeContext.athlone.SubNetwork.ireland.SubNetwork", field)

         then: ""
            assert result == "SubNetwork=ireland,SubNetwork=athlone,MeContext=cucp"
    }

    class TestClass {
        String testField
    }
}
