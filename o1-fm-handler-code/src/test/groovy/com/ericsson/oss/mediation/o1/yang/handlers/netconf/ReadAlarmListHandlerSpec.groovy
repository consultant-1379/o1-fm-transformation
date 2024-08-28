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
package com.ericsson.oss.mediation.o1.yang.handlers.netconf

import com.ericsson.cds.cdi.support.configuration.InjectionProperties
import com.ericsson.cds.cdi.support.providers.custom.model.ModelPattern
import com.ericsson.cds.cdi.support.providers.custom.model.local.LocalModelServiceProvider
import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.rule.SpyImplementation
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.itpf.common.config.Configuration
import com.ericsson.oss.itpf.common.event.handler.EventHandlerContext
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject
import com.ericsson.oss.itpf.datalayer.dps.stub.RuntimeConfigurableDps
import com.ericsson.oss.mediation.adapter.netconf.jca.xa.yang_read.provider.YangGetConfigOperation
import com.ericsson.oss.mediation.flow.events.MediationComponentEvent
import com.ericsson.oss.mediation.fm.o1.engine.api.O1AlarmService
import com.ericsson.oss.mediation.netconf.session.api.operation.NetconfSessionOperationStatus
import com.ericsson.oss.mediation.netconf.session.api.operation.NetconfSessionOperationsStatus
import com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.Constants
import com.ericsson.oss.mediation.o1.yang.handlers.netconf.parser.YangNetconfGetXmlParser
import com.ericsson.oss.mediation.o1.yang.handlers.netconf.stub.NetconfConnectionManagerForTestsOk
import com.ericsson.oss.mediation.o1.yang.handlers.netconf.stub.NetconfConnectionManagerForTestsThrowsException
import com.ericsson.oss.mediation.o1.yang.handlers.netconf.util.ReadSystemRecorder
import com.ericsson.oss.mediation.util.netconf.api.NetconfResponse

import javax.xml.parsers.ParserConfigurationException

import static com.ericsson.oss.mediation.adapter.netconf.jca.xa.yang.provider.YangNetconfOperationResult.YangNetconfOperationResultCode.ERROR
import static com.ericsson.oss.mediation.adapter.netconf.jca.xa.yang.provider.YangNetconfOperationResult.YangNetconfOperationResultCode.OK
import static com.ericsson.oss.mediation.netconf.session.api.operation.NetconfSessionOperation.GET
import static com.ericsson.oss.mediation.netconf.session.api.operation.NetconfSessionOperationErrorCode.NONE
import static com.ericsson.oss.mediation.netconf.session.api.operation.NetconfSessionOperationErrorCode.OPERATION_ABORTED
import static com.ericsson.oss.mediation.netconf.session.api.operation.NetconfSessionOperationErrorCode.OPERATION_FAILED
import static com.ericsson.oss.mediation.netconf.session.api.operation.NetconfSessionOperationErrorCode.SKIP_ALL

class ReadAlarmListHandlerSpec extends CdiSpecification {

    static final String NETWORK_ELEMENT_FDN = "NetworkElement=5G141O1001"
    static final String MECONTEXT_FDN = "MeContext=5G141O1001"
    static final String FM_ALARM_SUPERVISION_FDN = "NetworkElement=5G141O1001,FmAlarmSupervision=1"
    static final String ALARM_LIST_FDN = "MeContext=5G141O1001,ManagedElement=1,AlarmList=1"

    @ObjectUnderTest
    private ReadAlarmListHandler readAlarmListHandler

    private final Map<String, Object> headers = new HashMap<>()

    @MockedImplementation
    protected EventHandlerContext mockEventHandlerContext
    @MockedImplementation
    protected Configuration mockConfiguration
    @MockedImplementation
    protected ReadSystemRecorder readSystemRecorder
    @MockedImplementation
    protected O1AlarmService alarmService

    @SpyImplementation
    protected YangNetconfGetXmlParser yangNetconfGetXmlParser
    @SpyImplementation
    protected NetconfConnectionManagerForTestsOk netconfConnectionManagerForTestsOk
    @SpyImplementation
    protected NetconfConnectionManagerForTestsThrowsException netconfConnectionManagerForTestsThrowsException

    static filteredModels = [
            new ModelPattern('.*', '.*', '.*', '.*')
    ]

    static LocalModelServiceProvider localModelServiceProvider = new LocalModelServiceProvider(filteredModels)

    @Override
    def addAdditionalInjectionProperties(InjectionProperties injectionProperties) {
        super.addAdditionalInjectionProperties(injectionProperties)
        injectionProperties.addInjectionProvider(localModelServiceProvider)
    }

    def setup() {
        mockEventHandlerContext.getEventHandlerConfiguration() >> mockConfiguration
        mockConfiguration.getAllProperties() >> headers

        addO1NodeToDps()
    }

    def "Test read failure as the response from the node is malformed"() {
        given: "headers for an event with all required attributes"
            prepareHeaders()

        and: "connection manager stub is configured to return a malformed netconf response"
            configureNetconfConnectionManagerStubWithResponse('<malformed netconf response>', OK)

        and: "the ReadAlarmListHandler is initialized"
            readAlarmListHandler.init(mockEventHandlerContext)

        when: "the handler is triggered"
            def inputEvent = new MediationComponentEvent(headers, null)
            def response = readAlarmListHandler.onEvent(inputEvent)

        then: "an exception is not thrown"
            noExceptionThrown()

        and: "the event header is updated to indicate the request failed"
            validateNetConfSessionOperationStatus(OPERATION_FAILED)

        and: "there was one call to the connection manager with expected read request"
            1 * netconfConnectionManagerForTestsOk.executeXAResourceOperation({ operation -> return isValidReadOperation(operation) })

        and: "and a parsing error was recorded"
            1 * readSystemRecorder.recordError(Constants.EVENT_ID_FM_SUPERVISION_PARSE_ERROR,
                    "com.ericsson.oss.mediation.o1.yang.handlers.netconf.ReadAlarmListHandler",
                    "MeContext=5G141O1001,ManagedElement=5G141O1001,AlarmList=1",
                    "Unable to parse response <<malformed netconf response>>")

        and: "'SYNCHRONIZATION_ABORTED' EventNotification sent"
            1 * alarmService.sendAlarm({ event -> return event.eventType == 'SYNCHRONIZATION_ABORTED' })
    }

    def "Test read failure as the AlarmList element is missing from the response"() {
        given: "headers for an event with all required attributes"
            prepareHeaders()

        and: "connection manager stub is configured to return a response with AlarmList missing and result code ERROR"
            configureNetconfConnectionManagerStubWithResponse('<ManagedElement xmlns="urn:3gpp:sa5:_3gpp-common-managed-element"></ManagedElement>', ERROR)

        and: "the ReadAlarmListHandler is initialized"
            readAlarmListHandler.init(mockEventHandlerContext)

        when: "the handler is triggered"
            def inputEvent = new MediationComponentEvent(headers, null)
            def response = readAlarmListHandler.onEvent(inputEvent)

        then: "an exception is not thrown"
            noExceptionThrown()

        and: "the event header is updated to indicate the Netconf request failed"
            validateNetConfSessionOperationStatus(OPERATION_FAILED)

        and: "there was one call to the connection manager with expected read request"
            1 * netconfConnectionManagerForTestsOk.executeXAResourceOperation({ operation -> return isValidReadOperation(operation) })

        and: "and missing data error was recorded"
            1 * readSystemRecorder.recordError(Constants.EVENT_ID_FM_SUPERVISION_MISSING_DATA,
                    'com.ericsson.oss.mediation.o1.yang.handlers.netconf.ReadAlarmListHandler',
                    'MeContext=5G141O1001,ManagedElement=5G141O1001,AlarmList=1',
                    Constants.MSG_ALARM_LIST_TYPE_MISSING)

        and: "'SYNCHRONIZATION_ABORTED' EventNotification sent"
            1 * alarmService.sendAlarm({ event -> return event.eventType == 'SYNCHRONIZATION_ABORTED' })
    }

    def "Test read failure as the XML parser throws exception parsing the response"() {
        given: "headers for an event with all required attributes"
            prepareHeaders()

        and: "connection manager stub is configured to return a successful response"
            configureNetconfConnectionManagerStubWithResponse(getSuccessfulReadAlarmListResponse(), OK)

        and: "the netconf response parser is configured to throw an exception"
            yangNetconfGetXmlParser.parseXmlString(_) >> new ParserConfigurationException()

        and: "the ReadAlarmListHandler is initialized"
            readAlarmListHandler.init(mockEventHandlerContext)

        when: "the handler is triggered"
            def inputEvent = new MediationComponentEvent(headers, null)
            def response = readAlarmListHandler.onEvent(inputEvent)

        then: "an exception is not thrown"
            noExceptionThrown()

        and: "the event header is updated to indicate the request failed"
            validateNetConfSessionOperationStatus(OPERATION_FAILED)

        and: "there was one call to the connection manager with expected read request"
            1 * netconfConnectionManagerForTestsOk.executeXAResourceOperation({ operation -> return isValidReadOperation(operation) })

        and: "and a parsing error was recorded"
            1 * readSystemRecorder.recordError(Constants.EVENT_ID_FM_SUPERVISION_PARSE_ERROR,
                    'com.ericsson.oss.mediation.o1.yang.handlers.netconf.ReadAlarmListHandler',
                    'MeContext=5G141O1001,ManagedElement=5G141O1001,AlarmList=1',
                    "Unable to parse response <${getSuccessfulReadAlarmListResponse()}>")

        and: "'SYNCHRONIZATION_ABORTED' EventNotification sent"
            1 * alarmService.sendAlarm({ event -> return event.eventType == 'SYNCHRONIZATION_ABORTED' })
    }

    def "Test read failure if netconf connection setup fails"() {
        given: "headers for an event with all required attributes"
            prepareHeaders()

        and: "a netconf manager is provided in the headers"
            headers.put('netconfManager', netconfConnectionManagerForTestsOk)

        and: "the headers indicate that there was a connection failure and the operation should be skipped"
            NetconfSessionOperationsStatus operationsStatus = new NetconfSessionOperationsStatus()
            operationsStatus.addOperationStatus(new NetconfSessionOperationStatus(GET, SKIP_ALL));
            headers.put('netconfSessionOperationsStatus', operationsStatus)

        and: "the ReadAlarmListHandler is initialized"
            readAlarmListHandler.init(mockEventHandlerContext)

        when: "the handler is triggered"
            def inputEvent = new MediationComponentEvent(headers, null)
            def response = readAlarmListHandler.onEvent(inputEvent)

        then: "an exception is not thrown"
            noExceptionThrown()

        and: "the event header is updated to indicate the request has been aborted"
            validateNetConfSessionOperationStatus(OPERATION_ABORTED)

        and: "there were no calls to the connection manager"
            0 * netconfConnectionManagerForTestsOk.executeXAResourceOperation(_)

        and: "and active false error was recorded"
            1 * readSystemRecorder.recordError(Constants.EVENT_ID_FM_SUPERVISION_CONNECTION_ERROR,
                    'com.ericsson.oss.mediation.o1.yang.handlers.netconf.ReadAlarmListHandler',
                    'MeContext=5G141O1001,ManagedElement=5G141O1001,AlarmList=1',
                    Constants.MSG_NETCONF_SETUP_FAILED)

        and: "'SYNCHRONIZATION_ABORTED' EventNotification sent"
            1 * alarmService.sendAlarm({ event -> return event.eventType == 'SYNCHRONIZATION_ABORTED' })
    }

    def "Test failed read of the AlarmList due to exception during call to netconf connection manager"() {
        given: "headers for an event with all required attributes"
            prepareHeaders()

        and: "connection manager stub is configured to throw an exception"
            headers.put('netconfManager', netconfConnectionManagerForTestsThrowsException)

        and: "the ReadAlarmListHandler is initialized"
            readAlarmListHandler.init(mockEventHandlerContext)

        when: "the handler is triggered"
            def inputEvent = new MediationComponentEvent(headers, null)
            def response = readAlarmListHandler.onEvent(inputEvent)

        then: "an exception is not thrown"
            noExceptionThrown()

        and: "the event header is updated to indicate the request failed"
            validateNetConfSessionOperationStatus(OPERATION_FAILED)

        and: "there was one call to the connection manager"
            1 * netconfConnectionManagerForTestsThrowsException.executeXAResourceOperation(_)

        and: "and netconf connection error was recorded"
            1 * readSystemRecorder.recordError(Constants.EVENT_ID_FM_SUPERVISION_CONNECTION_ERROR,
                    'com.ericsson.oss.mediation.o1.yang.handlers.netconf.ReadAlarmListHandler',
                    'MeContext=5G141O1001,ManagedElement=5G141O1001,AlarmList=1',
                    'CRUD config enqueue failed: Test Exception')

        and: "'SYNCHRONIZATION_ABORTED' EventNotification sent"
            1 * alarmService.sendAlarm({ event -> return event.eventType == 'SYNCHRONIZATION_ABORTED' })
    }

    def "Test failed read due to null netconf operation result and result code ERROR from netconf connection manager"() {
        given: "headers for an event with all required attributes"
            prepareHeaders()

        and: "connection manager stub is configured to return a null response"
            netconfConnectionManagerForTestsOk.setNetconfOperationResult(null, ERROR)
            headers.put('netconfManager', netconfConnectionManagerForTestsOk)

        and: "the ReadAlarmListHandler is initialized"
            readAlarmListHandler.init(mockEventHandlerContext)

        when: "the handler is triggered"
            def inputEvent = new MediationComponentEvent(headers, null)
            def response = readAlarmListHandler.onEvent(inputEvent)

        then: "an exception is not thrown"
            noExceptionThrown()

        and: "the event header is updated to indicate the request failed"
            validateNetConfSessionOperationStatus(OPERATION_FAILED)

        and: "there was one call to the connection manager with expected read request"
            1 * netconfConnectionManagerForTestsOk.executeXAResourceOperation({ operation -> return isValidReadOperation(operation) })

        and: "and netconf connection error was recorded"
            1 * readSystemRecorder.recordError(Constants.EVENT_ID_FM_SUPERVISION_MISSING_DATA,
                    'com.ericsson.oss.mediation.o1.yang.handlers.netconf.ReadAlarmListHandler',
                    'MeContext=5G141O1001,ManagedElement=5G141O1001,AlarmList=1',
                    Constants.MSG_ALARM_LIST_TYPE_MISSING)

        and: "'SYNCHRONIZATION_ABORTED' EventNotification sent"
            1 * alarmService.sendAlarm({ event -> return event.eventType == 'SYNCHRONIZATION_ABORTED' })
    }

    def "Test failed read due to null YangNetconfOperationResult returned from the executeOperation() method"() {
        given: "headers for an event with all required attributes"
            prepareHeaders()

        and: "connection manager stub is configured to return a null result"
            netconfConnectionManagerForTestsOk.executeXAResourceOperation(_) >> null
            headers.put('netconfManager', netconfConnectionManagerForTestsOk)

        and: "the AlarmListHandler is initialized"
            readAlarmListHandler.init(mockEventHandlerContext)

        when: "the handler is triggered"
            def inputEvent = new MediationComponentEvent(headers, null)
            def response = readAlarmListHandler.onEvent(inputEvent)

        then: "an exception is not thrown"
            noExceptionThrown()

        and: "the event header is updated to indicate the request failed"
            validateNetConfSessionOperationStatus(OPERATION_FAILED)

        and: "and netconf connection error was recorded"
            1 * readSystemRecorder.recordError(Constants.EVENT_ID_FM_SUPERVISION_NULL_RESPONSE,
                    'com.ericsson.oss.mediation.o1.yang.handlers.netconf.ReadAlarmListHandler',
                    'MeContext=5G141O1001,ManagedElement=5G141O1001,AlarmList=1',
                    Constants.MSG_NETCONF_NULL_RESULT)

        and: "'SYNCHRONIZATION_ABORTED' EventNotification sent"
            1 * alarmService.sendAlarm({ event -> return event.eventType == 'SYNCHRONIZATION_ABORTED' })
    }

    def "Test successful read of the AlarmList when there are no alarmRecords"() {
        given: "headers for an event with all required attributes"
            prepareHeaders()

        and: "connection manager stub is configured to return a response with AlarmList.alarmRecords missing"
            configureNetconfConnectionManagerStubWithResponse('<ManagedElement xmlns="urn:3gpp:sa5:_3gpp-common-managed-element"><id>1</id>' +
                    '<AlarmList><id>1</id></AlarmList></ManagedElement>', OK)

        and: "the ReadAlarmListHandler is initialized"
            readAlarmListHandler.init(mockEventHandlerContext)

        when: "the handler is triggered"
            def inputEvent = new MediationComponentEvent(headers, null)
            def response = readAlarmListHandler.onEvent(inputEvent)

        then: "the resulting payLoad contains no alarmRecords"
            ((List) response.getPayload()).size() == 0

        and: "there was one call to the connection manager with expected read request"
            1 * netconfConnectionManagerForTestsOk.executeXAResourceOperation({ operation -> return isValidReadOperation(operation) })

        and: "and netconf connection error was recorded"
            1 * readSystemRecorder.recordError(Constants.EVENT_ID_FM_SUPERVISION_MISSING_DATA,
                    'com.ericsson.oss.mediation.o1.yang.handlers.netconf.ReadAlarmListHandler',
                    'MeContext=5G141O1001,ManagedElement=5G141O1001,AlarmList=1',
                    Constants.MSG_ALARM_RECORDS_CANNOT_BE_READ)

        and: "'SYNCHRONIZATION_ABORTED' EventNotification sent"
            1 * alarmService.sendAlarm({ event -> return event.eventType == 'SYNCHRONIZATION_ABORTED' })
    }

    def "Test successful read of the AlarmList.alarmRecords"() {
        given: "headers for an event with all required attributes"
            prepareHeaders()

        and: "connection manager stub is configured to return a successful response"
            configureNetconfConnectionManagerStubWithResponse(getSuccessfulReadAlarmListResponse(), OK)

        and: "the ReadAlarmListHandler is initialized"
            readAlarmListHandler.init(mockEventHandlerContext)

        when: "the handler is triggered"
            def inputEvent = new MediationComponentEvent(headers, null)
            def response = readAlarmListHandler.onEvent(inputEvent)

        then: "the resulting payLoad contains one alarmRecord"
            ((List) response.getPayload()).size() == 1

        and: "the alarmRecord has the expected fields"
            ((Map) ((List) response.getPayload()).get(0)).size() == 9
            ((Map) ((List) response.getPayload()).get(0)).get('additionalText') == 'The trusted certificate has expired and should be renewed'
            ((Map) ((List) response.getPayload()).get(0)).get('perceivedSeverity') == 'CRITICAL'
            ((Map) ((List) response.getPayload()).get(0)).get('specificProblem') == 'Certificate Management, Trusted Certificate is about to Expire'
            ((Map) ((List) response.getPayload()).get(0)).get('probableCause') == '351'
            ((Map) ((List) response.getPayload()).get(0)).get('alarmType') == 'PROCESSING_ERROR_ALARM'
            ((Map) ((List) response.getPayload()).get(0)).get('alarmChangedTime') == '2023-04-06T08:31:41.244466+00:00'
            ((Map) ((List) response.getPayload()).get(0)).get('notificationId') == 590443372
            ((Map) ((List) response.getPayload()).get(0)).get('objectInstance') == "/ts:truststore/ts:certificates[ts:name='Test CAs']/ts:certificate[ts:name='SelfSignedCert1']"
            ((Map) ((List) response.getPayload()).get(0)).get('alarmId') == '6c46f46b-ee28-4d71-b998-d575d87efa99'

        and: "the headers has the correct fdn"
            ((Map) response.getHeaders()).get("fdn") == "MeContext=5G141O1001,ManagedElement=5G141O1001,AlarmList=1"

        and: "there was one call to the connection manager with expected read request"
            1 * netconfConnectionManagerForTestsOk.executeXAResourceOperation({ operation -> return isValidReadOperation(operation) })


    }

    def prepareHeaders() {
        headers.put('fdn', FM_ALARM_SUPERVISION_FDN)
        headers.put('setIncludeNsPrefix', true)
        headers.put("active", true)

        NetconfSessionOperationsStatus operationsStatus = new NetconfSessionOperationsStatus()
        operationsStatus.addOperationStatus(new NetconfSessionOperationStatus(GET, NONE))
        headers.put('netconfSessionOperationsStatus', operationsStatus)
        headers.put('ManagedElementId', '5G141O1001')
    }

    def addO1NodeToDps() {
        RuntimeConfigurableDps configurableDps = cdiInjectorRule.getService(RuntimeConfigurableDps)
        configurableDps.withTransactionBoundaries()

        ManagedObject meContext = configurableDps.addManagedObject()
                .withFdn(MECONTEXT_FDN)
                .build();

        ManagedObject networkElement = configurableDps.addManagedObject()
                .withFdn(NETWORK_ELEMENT_FDN)
                .addAttributes([neType: "O1Node"])
                .withAssociation("nodeRootRef", meContext)
                .build()

        meContext.addAssociation('networkElementRef', networkElement)
    }

    def configureNetconfConnectionManagerStubWithResponse(response, yangNetconfOperationResultCode) {
        def netconfResponse = new NetconfResponse();
        netconfResponse.setData(response);
        netconfConnectionManagerForTestsOk.setNetconfOperationResult(netconfResponse, yangNetconfOperationResultCode)
        headers.put('netconfManager', netconfConnectionManagerForTestsOk)
    }

    def validateNetConfSessionOperationStatus(netconfSessionOperationErrorCode) {
        ((NetconfSessionOperationsStatus) headers.get('netconfSessionOperationsStatus'))
                .getOperationStatus(GET).getNetconfSessionOperationErrorCode() == netconfSessionOperationErrorCode
    }

    boolean isValidReadOperation(YangGetConfigOperation operation) {
        return operation instanceof YangGetConfigOperation && operation.getFdn().contains(ALARM_LIST_FDN)
    }

    String getSuccessfulReadAlarmListResponse() {
        return "<ManagedElement xmlns=\"urn:3gpp:sa5:_3gpp-common-managed-element\">" +
                "<id>5G141O1001</id>" +
                "<AlarmList>" +
                "<id>1</id>" +
                "<attributes>" +
                "<operationalState>ENABLED</operationalState>" +
                "<administrativeState>UNLOCKED</administrativeState>" +
                "<triggerHeartbeatNtf>true</triggerHeartbeatNtf>" +
                "<lastModification>2023-03-31T14:45:03.545512+00:00</lastModification>" +
                "<numOfAlarmRecords>1</numOfAlarmRecords>" +
                "<alarmRecords>" +
                "<additionalText>The trusted certificate has expired and should be renewed</additionalText>" +
                "<perceivedSeverity>CRITICAL</perceivedSeverity>" +
                "<specificProblem>Certificate Management, Trusted Certificate is about to Expire</specificProblem>" +
                "<probableCause>351</probableCause>" +
                "<alarmType>PROCESSING_ERROR_ALARM</alarmType>" +
                "<alarmChangedTime>2023-04-06T08:31:41.244466+00:00</alarmChangedTime>" +
                "<notificationId>590443372</notificationId>" +
                "<objectInstance>/ts:truststore/ts:certificates[ts:name='Test CAs']/ts:certificate[ts:name='SelfSignedCert1']</objectInstance>" +
                "<alarmId>6c46f46b-ee28-4d71-b998-d575d87efa99</alarmId>" +
                "</alarmRecords>" +
                "</attributes>" +
                "</AlarmList>" +
                "</ManagedElement>"
    }
}
