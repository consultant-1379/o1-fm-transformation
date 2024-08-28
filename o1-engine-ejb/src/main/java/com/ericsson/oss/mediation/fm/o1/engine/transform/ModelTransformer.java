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

package com.ericsson.oss.mediation.fm.o1.engine.transform;

import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.xml.bind.JAXB;
import javax.xml.transform.dom.DOMSource;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import com.ericsson.oss.mediation.fm.o1.engine.instrumentation.O1EngineStatistics;
import org.apache.commons.jelly.JellyContext;
import org.apache.commons.jelly.XMLOutput;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.ericsson.oss.itpf.sdk.recording.ErrorSeverity;
import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.mediation.fm.o1.engine.transform.converter.AbstractModelConverter;
import com.ericsson.oss.mediation.fm.o1.engine.transform.converter.impl.ManagedObjectInstanceConverter;
import com.ericsson.oss.mediation.fm.o1.engine.transform.converter.impl.O1DateModelConverter;
import com.ericsson.oss.mediation.fm.o1.engine.transform.converter.impl.XmlDefinedEnumModelConverter;
import com.ericsson.oss.mediation.fm.o1.engine.transform.evaluator.AbstractModelEvaluator;
import com.ericsson.oss.mediation.fm.o1.engine.transform.evaluator.impl.SimpleModelEvaluator;
import com.ericsson.oss.mediation.fm.o1.engine.transform.processor.AbstractModelProcessor;
import com.ericsson.oss.mediation.fm.o1.engine.transform.processor.impl.AdditionalInfoProcessor;
import com.ericsson.oss.mediation.fm.o1.engine.transform.processor.impl.HrefProcessor;
import com.ericsson.oss.mediation.fm.o1.engine.transform.processor.impl.TimeZoneModelProcessor;
import com.ericsson.oss.mediation.fm.o1.engine.transform.tag.ModelTransformerTagLibrary;
import com.ericsson.oss.mediation.fm.o1.engine.transform.xml.Enums;
import com.ericsson.oss.mediation.fm.o1.engine.transform.xml.Enums.EnumConfig;
import com.ericsson.oss.mediation.translator.model.EventNotification;

import lombok.extern.slf4j.Slf4j;

/**
 * Responsible for transforming alarm data received from the node into {@code EventNotification} objects which can be sent to FM applications.
 * <p>
 * Clients must provide an XML configuration file that defines the model transformers and their properties.
 * <p>
 * The XML configuration file for O1 is called {@code transformer-config.xml}.
 * <p>
 * Alarm properties are transformed using the converter, evaluators, and processors registered with this class.
 * <ul>
 * <li>Evaluator - updates the value of an alarm property to a static value provided as an argument in the transformation XML.</li>
 * <li>Converter - converts the value of an alarm property to some other value e.g. converts integer value to ENUM string.</li>
 * <li>Processor - has access to the {@code EventNotification} and can set properties on it e.g. can set the time zone property on the
 * {@code EventNotification} if the alarms 'eventTime' property is set.</li>
 * </ul>
 * <p>
 * Each converter, evaluator, and processor must have an XML tag / class registered with the {@code ModelTransformerTagLibrary}.
 */
@Slf4j
public class ModelTransformer {

    private static final Map<String, String> CONVERTERS = new HashMap<>();
    private static final Map<String, String> EVALUATORS = new HashMap<>();
    private static final Map<String, String> PROCESSORS = new HashMap<>();
    private static final XMLOutput NO_OUTPUT = XMLOutput.createDummyXMLOutput();

    static {
        registerConverter(O1DateModelConverter.class);
        registerConverter(ManagedObjectInstanceConverter.class);
        registerConverter(XmlDefinedEnumModelConverter.class);
        registerEvaluator(SimpleModelEvaluator.class);
        registerProcessor(HrefProcessor.class);
        registerProcessor(AdditionalInfoProcessor.class);
        registerProcessor(TimeZoneModelProcessor.class);
    }

    @Inject
    DefaultModelTransformerKeyFactory defaultModelTransformerKeyFactory;

    @Inject
    SystemRecorder systemRecorder;

    @Inject
    O1EngineStatistics o1EngineStatistics;

    private final Map<ModelTransformerKey, ModelTransformerScripts> scripts = new HashMap<>();

    /**
     * Generates a transformer script that can be used perform the transformation based on the XML configuration file provided.
     *
     * @param transformerUrl
     *            URL for the XML configuration file that defines the model transformers and their properties
     * @return a key that identifies the transformer script
     */
    public ModelTransformerKey addModelTransformerConfig(final URL transformerUrl) {
        if (transformerUrl == null) {
            log.error("transformerUrl cannot be null");
            throw new IllegalArgumentException("transformerUrl cannot be null");
        }
        try {
            final Document document = ModelTransformerXmlProcessor.process(transformerUrl.toURI());
            return addModelTransformerConfigScripts(document);
        } catch (final URISyntaxException ex) {
            log.error("Invalid url " + transformerUrl, ex);
            throw new IllegalArgumentException("Invalid url " + transformerUrl, ex);
        }
    }

    /**
     * Transforms the properties of the alarm provided into an {@code EventNotification}.
     *
     * @param alarm
     *            A map containing the properties of the alarm to be transformed.
     * @param modelTransformerKey
     *            a key that identifies the transformer script to use.
     * @return An {@Code EventNotification} representing the transformed alarm.
     */
    public EventNotification transformAlarm(final Map<String, Object> alarm, final ModelTransformerKey modelTransformerKey) {

        final String id = getNotificationType(alarm);

        return doTransform(alarm, modelTransformerKey, id);
    }

    /**
     * Transforms the properties of the alarm records provided into a list of {@code EventNotification}.
     *
     * @param alarms
     *            A list of maps containing the properties of the alarms to be transformed.
     * @param modelTransformerKey
     *            a key that identifies the transformer script to use.
     * @return A {@code List<EventNotification>} representing the transformed alarms.
     */
    public List<EventNotification> transformAlarms(final List<Map<String, Object>> alarms, final ModelTransformerKey modelTransformerKey) {

        final List<EventNotification> eventNotifications = new ArrayList<>();

        for (final Map<String, Object> map : alarms) {
            try {
                final EventNotification eventNotification = doTransform(map, modelTransformerKey, "alarmRecords");
                eventNotifications.add(eventNotification);
            } catch (final Exception e) {
                systemRecorder.recordError("Issue transforming alarms", ErrorSeverity.WARNING, "ModelTransformer", "",
                        "Transformation failed for the alarm with the following ID: " + map.get("alarmId"));
            }
        }
        return eventNotifications;
    }

    /**
     * Transforms the alarm data provided using the transformer script that is identified by the {@code ModelTransformerKey} provided.
     *
     * @param alarmMap
     *            A map containing the properties of the alarm to be transformed.
     * @param modelTransformerKey
     *            a key that identifies the transformer script to use.
     * @return An {@Code EventNotification} representing the transformed alarm.
     */
    private EventNotification doTransform(final Map<String, Object> alarmMap, final ModelTransformerKey modelTransformerKey, final String id) {
        final EventNotification eventNotification = new EventNotification();
        final ModelTransformerScripts modelTransformerScripts = scripts.get(modelTransformerKey);
        final ScriptDecorator script = modelTransformerScripts.getScript(id);

        if (script == null) {
            throw new ModelTransformerException("No model transformer scripts found with key " + modelTransformerKey + "for id " + id);
        }

        log.info("Executing transformer script with id: {} ", modelTransformerScripts);
        final JellyContext context = modelTransformerScripts.getRootContext().newJellyContext();

        context.setCurrentURL(script.getScriptJarURL());
        context.setVariable(Constants.TRANSFORMER_CONFIGURATION_FILE_URL, script.getScriptJarURL());
        context.setVariable(Constants.SRC_OBJECT, alarmMap);
        context.setVariable(Constants.SRC_VALUES_MAP, getOidData(alarmMap));
        context.setVariable(Constants.DST_OBJECT, eventNotification);

        final URL[] jarURLs = script.getJarURLs();
        try (URLClassLoader classLoader = new URLClassLoader(jarURLs, Thread.currentThread().getContextClassLoader())) {
            context.setClassLoader(classLoader);
            script.run(context, NO_OUTPUT);
            log.info(Constants.DST_OBJECT + " after: " + eventNotification);
            o1EngineStatistics.incrementTotalNoOfSuccessfulTransformations();
        } catch (final Exception ex) {
            throw new ModelTransformerException("Error executing transformer script with id: " + id
                    + ". The error may be related to this transformer script or to one imported. Transformer tag source is: \n"
                    + script.getScriptElement(), ex);
        }
        return eventNotification;
    }

    private static void registerConverter(final Class<? extends AbstractModelConverter> converter) {
        CONVERTERS.put(converter.getSimpleName(), converter.getName());
    }

    private static void registerEvaluator(final Class<? extends AbstractModelEvaluator> evaluator) {
        EVALUATORS.put(evaluator.getSimpleName(), evaluator.getName());
    }

    private static void registerProcessor(final Class<? extends AbstractModelProcessor> processor) {
        PROCESSORS.put(processor.getSimpleName(), processor.getName());
    }

    private ModelTransformerKey addModelTransformerConfigScripts(final Document document) {
        final ModelTransformerKey modelTransformerKey = defaultModelTransformerKeyFactory.createKey(document);
        final JellyContext rootContext = createRootContext(document);
        final ModelTransformerScripts modelTransformerScripts = new ModelTransformerScripts(rootContext);

        final List<ScriptDecorator> compiledTransformerScripts =
                ModelTransformerXmlProcessor.createAndCompileTransformerScripts(rootContext, document);

        for (final ScriptDecorator script : compiledTransformerScripts) {
            modelTransformerScripts.addScript(script.getId(), script);
        }

        scripts.put(modelTransformerKey, modelTransformerScripts);

        return modelTransformerKey;
    }

    private JellyContext createRootContext(final Document document) {
        try {
            final JellyContext rootContext = new JellyContext();
            // Register converters, evaluators, and processors
            rootContext.setVariable(Constants.REGISTERED_CONVERTERS, CONVERTERS);
            rootContext.setVariable(Constants.REGISTERED_EVALUATORS, EVALUATORS);
            rootContext.setVariable(Constants.REGISTERED_PROCESSORS, PROCESSORS);

            // Register XML tags to be used with convertors, evaluators, and processors
            rootContext.registerTagLibrary(ModelTransformerTagLibrary.NAMESPACE_URI, new ModelTransformerTagLibrary());

            loadEnums(document, rootContext);
            return rootContext;
        } catch (final Exception ex) {
            throw new TransformerException("Error creating root context", ex);
        }
    }

    /*
     * Loads the enums and saves them in the root context
     */
    private static void loadEnums(final Document document, final JellyContext rootContext) throws XPathExpressionException {
        final Element enumsElement = (Element) XPathFactory.newInstance().newXPath().evaluate(Constants.ENUMS_ELEMENT_XPATH, document,
                XPathConstants.NODE);
        final Map<String, Map<Integer, String>> enumsMap = new HashMap<>();
        if (enumsElement != null) {
            final Enums enums = JAXB.unmarshal(new DOMSource(enumsElement), Enums.class);
            final List<EnumConfig> enumList = enums.getEnumConfigs();
            if (enumList != null) {
                for (final EnumConfig enumConfig : enumList) {
                    enumsMap.put(enumConfig.getName(), enumConfig.getEntries());
                }
            }

        }
        rootContext.setVariable(Constants.ENUMS_MAP, enumsMap);
    }

    private Map<OidKey, Object> getOidData(final Map<String, Object> alarmMap) {
        final HashMap<OidKey, Object> oidMap = new HashMap<>();
        for (final Map.Entry<String, Object> entry : alarmMap.entrySet()) {
            oidMap.put(new OidKey(entry.getKey()), entry.getValue());
        }
        return oidMap;
    }

    private String getNotificationType(final Map<String, Object> alarmMap) {
        final Object notificationType = alarmMap.get("notificationType");
        if (notificationType == null) {
            throw new TransformerException("notificationType from alarm map was null - cannot transform alarm");
        }
        if (notificationType instanceof String) {
            return (String) notificationType;
        }
        throw new TransformerException("notificationType was not a string type - cannot transform alarm");
    }
}
