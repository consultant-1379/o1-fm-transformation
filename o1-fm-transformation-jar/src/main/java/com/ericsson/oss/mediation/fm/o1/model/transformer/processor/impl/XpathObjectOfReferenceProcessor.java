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

package com.ericsson.oss.mediation.fm.o1.model.transformer.processor.impl;

import static com.ericsson.oss.mediation.fm.o1.model.transformer.processor.impl.Constants.COLON;
import static com.ericsson.oss.mediation.fm.o1.model.transformer.processor.impl.Constants.COMMA;
import static com.ericsson.oss.mediation.fm.o1.model.transformer.processor.impl.Constants.DOUBLE_DOLLAR_KEY;
import static com.ericsson.oss.mediation.fm.o1.model.transformer.processor.impl.Constants.EQUALS_ONE;
import static com.ericsson.oss.mediation.fm.o1.model.transformer.processor.impl.Constants.EQUALS_SIGN;
import static com.ericsson.oss.mediation.fm.o1.model.transformer.processor.impl.Constants.FORWARD_SLASH;
import static com.ericsson.oss.mediation.fm.o1.model.transformer.processor.impl.Constants.LEFT_SQUARE_BRACE;
import static com.ericsson.oss.mediation.fm.o1.model.transformer.processor.impl.Constants.RIGHT_SQAURE_BRACE;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.jelly.JellyContext;

import com.ericsson.oss.mediation.fm.snmp.model.transformer.processor.AbstractModelProcessor;
import com.ericsson.oss.mediation.fm.snmp.model.transformer.processor.ModelProcessorException;
import com.ericsson.oss.mediation.translator.model.EventNotification;

public class XpathObjectOfReferenceProcessor extends AbstractModelProcessor {

    /**
     * Creates a new XpathObjectOfReferenceProcessor for processing managed object instance.
     *
     * @param args
     *            the arguments passed to this processor.
     * @param context
     *            the model transformer context.
     */
    public XpathObjectOfReferenceProcessor(final String[] args, final JellyContext context) {
        super(args, context);
    }

    /**
     * Invoked by the framework when the associated OID is found in the SNMP data. Example of XPATH:- /epg:epg/sgw/function[name='SGW Session
     * Controller']/board[name='gc-0/3/1'] converted DN should be:- epg=1,sgw=1,function='SGW Session Controller',board='gc-0/3/1';
     *
     * @param <T>
     *            the SNMP object type
     * @param bean
     *            the SNMP object
     * @param snmpOid
     *            the SNMP OID
     * @param snmpValue
     *            the SNMP data value
     * @param eventNotification
     *            the EventNotification model object.
     */
    @Override
    public <T> void process(final T bean, final String snmpOid, final Object snmpValue, final EventNotification eventNotification) {

        if (snmpValue != null) {
            if (snmpValue instanceof String) {

                final String managedObjectValue = (String) snmpValue;
                final String formattedManagedObject = managedObjectValue.replace("][", "]/[");
                final String[] originalTokenValues = formattedManagedObject.split(FORWARD_SLASH);
                final List<String> processedTokenValues = extractTokenModules(originalTokenValues);
                eventNotification.setManagedObjectInstance(getManagedObjectValue(processedTokenValues));

            } else {
                throw new ModelProcessorException("Invalid Processor usage.Invalid data type. Snmp oid is: " + snmpOid);
            }
        }
    }

    private List<String> extractTokenModules(final String[] originalTokenValues) {

        final List<String> processedTokenValues = new ArrayList<>();
        StringBuilder tempTokenValue = new StringBuilder();
        boolean isTokenAppendNeeded = false;
        for (final String tokenValue : originalTokenValues) {
            if (tokenValue.trim().isEmpty()) {
                continue;
            }

            if (checkForPattern(tokenValue) && !isTokenAppendNeeded) {
                processedTokenValues.add(tokenValue);
            } else {
                isTokenAppendNeeded = true;
                if (!tempTokenValue.toString().isEmpty()) {
                    tempTokenValue.append(FORWARD_SLASH).append(tokenValue);
                } else {
                    tempTokenValue.append(tokenValue);
                }
                if (tokenValue.contains(RIGHT_SQAURE_BRACE)) {
                    processedTokenValues.add(tempTokenValue.toString());
                    isTokenAppendNeeded = false;
                    tempTokenValue = new StringBuilder();
                }
            }
        }

        return processedTokenValues;

    }

    private String extractTokenValues(final String moduleKeyValues, final String moduleKey) {
        final StringBuilder values = new StringBuilder();

        boolean isFirstAttribute = true;
        for (final String objectKeyVal : moduleKeyValues.split(COMMA)) {

            if (isNotNullAndNotEmpty(moduleKey) && objectKeyVal.split(EQUALS_SIGN).length > 1) {
                if (isFirstAttribute) {
                    values.append(objectKeyVal.split(EQUALS_SIGN)[1]);
                } else {
                    values.append(DOUBLE_DOLLAR_KEY).append(objectKeyVal.split(EQUALS_SIGN)[1]);

                }
                isFirstAttribute = false;
            } else {
                values.append(objectKeyVal);
                isFirstAttribute = false;
            }

        }
        return values.toString();
    }

    private String getManagedObjectValue(final List<String> processedTokenValues) {
        final StringBuilder objectOfReference = new StringBuilder();
        for (final String tokenValue : processedTokenValues) {
            if (!objectOfReference.toString().isEmpty()) {
                objectOfReference.append(COMMA);
            }

            if (tokenValue != null && !tokenValue.trim().isEmpty()) {
                final String module = extractNameAndNameSpace(tokenValue);
                if (!module.trim().isEmpty()) {
                    processExtractedModule(objectOfReference, module);
                }
            }
        }
        return objectOfReference.toString();
    }

    private void processExtractedModule(final StringBuilder objectOfReference, final String module) {

        if (module.contains(LEFT_SQUARE_BRACE) && module.contains(RIGHT_SQAURE_BRACE)) {

            final CharSequence moduleKey = module.substring(0, module.indexOf(LEFT_SQUARE_BRACE));
            final CharSequence moduleKeyValues = module.substring(module.indexOf(LEFT_SQUARE_BRACE) + 1, module.indexOf(RIGHT_SQAURE_BRACE));
            if (moduleKey.toString().isEmpty()) {
                objectOfReference.append(extractTokenValues(moduleKeyValues.toString(), moduleKey.toString()));
            } else {
                objectOfReference.append(moduleKey).append(EQUALS_SIGN).append(extractTokenValues(moduleKeyValues.toString(), moduleKey.toString()));
            }

        } else {
            if (module.contains(EQUALS_SIGN)) {
                objectOfReference.append(module);
            } else {
                objectOfReference.append(module).append(EQUALS_ONE);
            }
        }
    }

    private String extractNameAndNameSpace(final String tokenValue) {

        String module = "";

        if (tokenValue.contains(LEFT_SQUARE_BRACE) && tokenValue.contains(RIGHT_SQAURE_BRACE)) {
            if (tokenValue.contains(COLON) && tokenValue.indexOf(COLON) < tokenValue.indexOf(LEFT_SQUARE_BRACE)) {
                module = tokenValue.substring(tokenValue.indexOf(COLON) + 1);
            } else {
                module = tokenValue;
            }
        } else {
            if (tokenValue.contains(COLON)) {
                module = tokenValue.substring(tokenValue.indexOf(COLON) + 1);
            } else {
                module = tokenValue;
            }
        }
        return module;
    }

    private boolean checkForPattern(final String tokenValue) {
        return tokenValue.contains(LEFT_SQUARE_BRACE) && tokenValue.contains(RIGHT_SQAURE_BRACE)
                || !tokenValue.contains(LEFT_SQUARE_BRACE) && !tokenValue.contains(RIGHT_SQAURE_BRACE);
    }

    private boolean isNotNullAndNotEmpty(final String moduleKey) {
        return null != moduleKey && !moduleKey.isEmpty();
    }

}
