package com.ericsson.oss.mediation.o1.yang.handlers.netconf.util

import com.ericsson.oss.itpf.modeling.modelservice.exception.ConstraintViolationException
import com.ericsson.oss.itpf.modeling.modelservice.typed.core.DataType
import com.ericsson.oss.mediation.o1.yang.handlers.netconf.parser.ParserConstants
import spock.lang.Specification
import spock.lang.Unroll


class ParserConstantsSpec extends Specification {


    @Unroll
    def "parseNumber should return the correct number type for #dataType and value #value"() {
        expect:
        result == ParserConstants.parseNumber(dataType, value.toString())

        where:
        dataType         | value || result
        DataType.BYTE    | 1     || (byte) 1
        DataType.SHORT   | 2     || (short) 2
        DataType.INTEGER | 3     || 3
        DataType.LONG    | 4L    || 4L
        DataType.DOUBLE  | 5.0   || 5.0
    }

    def "parseNumber should throw numberformateException for unsupported types"() {
        when:
        ParserConstants.parseNumber(DataType.INTEGER, "1x")

        then:
        thrown(ConstraintViolationException.class)
    }

    def "parseNumber should throw ConstraintViolationException for unsupported types"() {
        when:
        ParserConstants.parseNumber(DataType.IP_ADDRESS, "1")

        then:
        thrown(ConstraintViolationException.class)
    }

    def "appendDpsList should append list correctly"() {
        expect:
        result == ParserConstants.appendDpsList(input)

        where:
        input           || result
        []              || ""
        [1, 2, 3]       || "[1,2,3]"
        ["a", "b", "c"] || "[a,b,c]"
    }

    def "removeNodeAppInstanceName should remove instance name correctly"() {
        expect:
        result == ParserConstants.removeNodeAppInstanceName(input)

        where:
        input                   || result
        "node---instance---app" || "node-app"
        "node--- ---app"        || "node--- ---app"
        "node---instance--- "   || "node---instance--- "
    }

}