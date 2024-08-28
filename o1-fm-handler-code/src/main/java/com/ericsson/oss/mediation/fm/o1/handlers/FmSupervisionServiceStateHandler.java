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

package com.ericsson.oss.mediation.fm.o1.handlers;

import static com.ericsson.oss.mediation.fm.o1.common.Constants.AUTOMATIC_SYNCHRONIZATION;
import static com.ericsson.oss.mediation.fm.o1.common.Constants.COMMA_FM_ALARM_SUPERVISION_RDN;
import static com.ericsson.oss.mediation.fm.o1.common.Constants.COMMA_FM_FUNCTION_RDN;
import static com.ericsson.oss.mediation.fm.o1.common.util.AlarmSupervisionResponseHelper.createAlarmSupervisionResponseSuccess;
import static com.ericsson.oss.mediation.fm.o1.handlers.util.Attributes.CURRENT_SERVICE_STATE;
import static com.ericsson.oss.mediation.fm.o1.handlers.util.Attributes.FDN;
import static com.ericsson.oss.mediation.fm.o1.handlers.util.HandlerMessages.FAILED_TO_SET_CURRENT_SERVICE_STATE;
import static com.ericsson.oss.services.models.ned.fm.function.FmSyncStatus100.IDLE;
import static com.ericsson.oss.services.models.ned.fm.function.FmSyncStatus100.IN_SERVICE;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;
import com.ericsson.oss.mediation.fm.o1.engine.api.O1AlarmService;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

import com.ericsson.oss.itpf.common.event.handler.annotation.EventHandler;
import com.ericsson.oss.itpf.common.event.handler.exception.EventHandlerException;
import com.ericsson.oss.itpf.sdk.core.retry.RetryManager;
import com.ericsson.oss.itpf.sdk.core.retry.RetryPolicy;
import com.ericsson.oss.itpf.sdk.eventbus.model.EventSender;
import com.ericsson.oss.itpf.sdk.eventbus.model.annotation.Modeled;
import com.ericsson.oss.mediation.flow.events.MediationComponentEvent;
import com.ericsson.oss.mediation.fm.o1.common.util.FdnUtil;
import com.ericsson.oss.mediation.fm.o1.dps.DpsRead;
import com.ericsson.oss.mediation.fm.o1.dps.DpsWrite;
import com.ericsson.oss.mediation.fm.o1.handlers.util.Attributes;
import com.ericsson.oss.mediation.fm.o1.handlers.util.HandlerMessages;
import com.ericsson.oss.mediation.fm.o1.handlers.util.MediationTaskRequestUtil;
import com.ericsson.oss.mediation.fm.supervision.response.AlarmSupervisionResponse;
import com.ericsson.oss.mediation.sdk.event.MediationTaskRequest;
import com.ericsson.oss.services.fm.service.model.FmMediationAlarmSyncRequest;
import com.ericsson.oss.services.models.ned.fm.function.FmSyncStatus100;
import lombok.extern.slf4j.Slf4j;

/**
 * Handler responsible for performing post supervision actions required when supervision is successfully enabled / disabled.
 * <p>
 * The header must contain the 'FmAlarmSupervision' FDN and its 'active' attribute which indicates whether supervision is being turned on or off.
 * <p>
 * The following actions are performed if supervision is turned on:
 * <ul>
 * <li>Set the 'FmFunction.currentServiceState' to 'IN_SERVICE'.</li>
 * <li>Send AlarmSupervisionResponse event to indicate that the alarm supervision request is complete.<li>
 * <li>If the 'FmSupervision.automaticSynchronization is set to true, send an FmMediationAlarmSyncRequest event to trigger an alarm synch.</li>
 * <ul>
 * The following actions are performed if supervision is turned off:
 * <ul>
 * <li>Set the 'FmFunction.currentServiceState' to 'IDLE'.</li>
 * <li>Send AlarmSupervisionResponse event to indicate that the alarm supervision request is complete.<li>
 * <ul>
 */
@Slf4j
@EventHandler
public class FmSupervisionServiceStateHandler extends AbstractFmMediationHandler {

    private static final String HANDLER = FmSupervisionServiceStateHandler.class.getSimpleName();

    @Inject
    @Modeled
    private EventSender<AlarmSupervisionResponse> alarmSupervisionResponseSender;

    @Inject
    @Modeled
    private EventSender<MediationTaskRequest> fmMediationAlarmSyncRequestSender;

    @Inject
    private RetryManager retryManager;

    @Inject
    private DpsRead dpsRead;

    @Inject
    private DpsWrite dpsWrite;

    @EServiceRef
    private O1AlarmService alarmService;
    /**
     * @param inputEvent
     *            the parameter, received by mediation framework, is the event triggering the present handler
     * @return the return code is for passing the event through next handlers
     */
    @Override
    public Object onEventWithResult(final Object inputEvent) {
        super.onEventWithResult(inputEvent);

        final boolean isSupervisionActive = isSupervisionActive();
        final String fmAlarmSupervisionFdn = getHeader(FDN);

        if (fmAlarmSupervisionFdn == null) {
            throw new EventHandlerException(HandlerMessages.SUPERVISION_FAILED_MSG + ": FmAlarmSupervision FDN not available");
        }

        final String networkElementFdn = FdnUtil.getParentFdn(fmAlarmSupervisionFdn);
        final String fmFunctionFdn = networkElementFdn.concat(COMMA_FM_FUNCTION_RDN);
        final String currentServiceState = dpsRead.getAttributeValue(fmFunctionFdn, CURRENT_SERVICE_STATE);

        log.info("FDN [{}] : supervision active [{}] : FmFunction.currentServiceState [{}]", fmAlarmSupervisionFdn, isSupervisionActive,
                currentServiceState);

        processSupervision(networkElementFdn, isSupervisionActive, currentServiceState);

        return new MediationComponentEvent(headers, StringUtils.EMPTY);
    }

    @Override
    protected Logger getLogger() {
        return log;
    }

    private void processSupervision(final String networkElementFdn, final boolean isSupervisionActive, final String currentServiceState) {
        final FmSyncStatus100 wantedCurrentServiceState = isSupervisionActive ? IN_SERVICE : IDLE;

        if (wantedCurrentServiceState.toString().equals(currentServiceState)) {
            log.info("FmFunction.currentServiceState is already [{}] for [{}], skipping update.", currentServiceState, networkElementFdn);
            return;
        }

        updateCurrentServiceState(networkElementFdn, wantedCurrentServiceState);
        updateNodeSuspenderCache(networkElementFdn, isSupervisionActive);
        sendAlarmSupervisionResponse(networkElementFdn, isSupervisionActive);

        if (isSupervisionActive && isAutomaticSyncSet(networkElementFdn)) {
            sendAlarmSyncRequest(networkElementFdn);
        }
    }

    private void updateCurrentServiceState(final String networkElementFdn, final FmSyncStatus100 wantedCurrentServiceState) {
        final RetryPolicy policy = RetryPolicy.builder()
                .attempts(5)
                .waitInterval(1, SECONDS)
                .retryOn(Exception.class)
                .build();

        final String fmFunctionFdn = networkElementFdn.concat(COMMA_FM_FUNCTION_RDN);

        try {
            retryManager.executeCommand(policy, retryContext -> {
                log.info("Setting FmFunction.currentServiceState to [{}] for [{}]", wantedCurrentServiceState, networkElementFdn);
                final Map<String, Object> attributes = getFmFunctionAttributesToUpdate(wantedCurrentServiceState);
                dpsWrite.setAttributeValues(fmFunctionFdn, attributes);
                return null;
            });

        } catch (final Exception e) {
            final String errorMessage = String.format(FAILED_TO_SET_CURRENT_SERVICE_STATE, wantedCurrentServiceState, networkElementFdn);
            getRecorder().recordError(HANDLER, getHeader(FDN), fmFunctionFdn, errorMessage);
            throw new EventHandlerException(errorMessage, e);
        }
    }

    private Map<String, Object> getFmFunctionAttributesToUpdate(final FmSyncStatus100 wantedCurrentServiceState) {
        final Map<String, Object> attributes = new HashMap<>();
        final Date lastUpdatedTimeStamp = new Date();
        attributes.put(Attributes.LAST_UPDATED_TIME_STAMP, lastUpdatedTimeStamp);
        attributes.put(Attributes.LAST_UPDATED, String.valueOf(lastUpdatedTimeStamp.getTime()));
        attributes.put(Attributes.CURRENT_SERVICE_STATE, wantedCurrentServiceState.toString());
        return attributes;
    }

    private void updateNodeSuspenderCache(final String networkElementFdn, final boolean isSupervisionActive) {
        final String meContextFdn = dpsRead.getMeContextFdn(networkElementFdn);
        final String meContextId = FdnUtil.getMeContextId(meContextFdn);
        if (isSupervisionActive) {
            alarmService.addToNodeSuspenderCache(meContextId);
        } else {
            alarmService.removeFromNodeSuspenderCache(meContextId);
        }
    }

    private void sendAlarmSupervisionResponse(final String networkElementFdn, final boolean isSupervisionActive) {
        try {
            final AlarmSupervisionResponse response = createAlarmSupervisionResponseSuccess(networkElementFdn, isSupervisionActive);
            alarmSupervisionResponseSender.send(response);
            log.info("Sent alarm supervision response event: {}", response);
        } catch (final Exception exception) {
            log.error("Exception when sending AlarmSupervisionResponse event {}", exception.getMessage());
        }
    }

    private boolean isAutomaticSyncSet(final String networkElementFdn) {
        return dpsRead.getAttributeValue(networkElementFdn.concat(COMMA_FM_ALARM_SUPERVISION_RDN), AUTOMATIC_SYNCHRONIZATION);
    }

    private void sendAlarmSyncRequest(final String networkElementFdn) {
        try {
            FmMediationAlarmSyncRequest event = MediationTaskRequestUtil.createFmMediationAlarmSyncRequest(networkElementFdn);
            getLogger().info("Sent FmMediationAlarmSyncRequest event: {} ", event);
            fmMediationAlarmSyncRequestSender.send(event);
        }
        catch (final Exception exception) {
            getRecorder().recordError(HANDLER, networkElementFdn, "", "Exception when sending FmMediationAlarmSyncRequest event " + exception.getMessage());
        }
    }
}
