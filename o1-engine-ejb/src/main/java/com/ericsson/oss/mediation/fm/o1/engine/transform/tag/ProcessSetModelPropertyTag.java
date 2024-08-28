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

package com.ericsson.oss.mediation.fm.o1.engine.transform.tag;

import java.util.Map;

import org.apache.commons.jelly.JellyTagException;
import org.apache.commons.jelly.MissingAttributeException;

import com.ericsson.oss.mediation.fm.o1.engine.transform.Constants;
import com.ericsson.oss.mediation.fm.o1.engine.transform.processor.AbstractModelProcessor;
import com.ericsson.oss.mediation.fm.o1.engine.transform.util.TransformerUtils;

public class ProcessSetModelPropertyTag extends AbstractArgumentableTag {

    private String processor;

    @SuppressWarnings("unchecked")
    @Override
    public void doTag() throws JellyTagException {
        if (oid == null && !(getParent() instanceof ValueHolder)) {
            logger.error("Attribute 'oid' not set");
            throw new MissingAttributeException("oid");
        }

        if (processor == null) {
            logger.error("Attribute 'processor' not set");
            throw new MissingAttributeException("processor");
        }

        final Map<String, String> registeredProcessors = (Map<String, String>) context.getVariable(Constants.REGISTERED_PROCESSORS);
        if (registeredProcessors.containsKey(processor)) {
            processor = registeredProcessors.get(processor);
        }

        try {
            final AbstractModelProcessor modelProcessor =
                    TransformerUtils.createInstance(processor, getArguments(), context.getClassLoader(), context);
            final Object alarmValue = getValue();
            modelProcessor.process(alarmObject, oid, alarmValue, eventNotification);

        } catch (final Exception ex) {
            throw new JellyTagException("Error executing ProcessSetModelPropertyTag. Processor is: "
                    + processor + " Oid is: " + oid + ", mappedBy is: " + mappedBy, ex);
        }
    }

    public String getProcessor() {
        return processor;
    }

    public void setProcessor(final String processor) {
        this.processor = processor;
    }
}
