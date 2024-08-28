package com.ericsson.oss.mediation.o1.yang.handlers.netconf.util

import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.mediation.o1.yang.handlers.netconf.util.LoggingUtil

/**
 * This class has been added for test coverage. It should be removed when full clone of netconf code is done.
 */
class LoggingUtilSpec extends CdiSpecification {

    def 'test convertToRecordingFormat with null input' () {
        when: 'method is called with null input'
            LoggingUtil.convertToRecordingFormat(null)

        then: 'Exception is thrown'
        thrown IllegalArgumentException
    }

    def 'test logEventDetails with null input' () {
        when: 'method is called with null input'
        LoggingUtil.logEventDetails(null)

        then: 'Exception is thrown'
        thrown IllegalArgumentException
    }

    def 'test constructMOInstrumentationRecorder' () {
        when: 'method is called'
        String result = LoggingUtil.constructMOInstrumentationRecorder(
                123l, 'testOperation', 'MeContext=1', 'TestMo', 1234l)

        then: 'Result is returned.'
        result == 'Start Time=01:00:00.123,Operation Type=testOperation,Managed Object=MeContext=1,MO Name=TestMo,Finish Time=01:00:01.234'
    }

    def 'test constructErrorLogLine' () {
        when: 'method is called'
        String result = LoggingUtil.constructErrorLogLine('testHandler', 'MeContext=1', 'exceptionMessage')

        then: 'Result is returned.'
        result == 'Error in: testHandler (\'MeContext=1\'). Exception message: exceptionMessage'
    }

}
