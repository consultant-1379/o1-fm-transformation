/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2024
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.oss.mediation.fm.o1.flows;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Rule;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

import com.ericsson.cds.cdi.support.providers.custom.model.ModelPattern;
import com.ericsson.oss.mediation.adapter.netconf.jca.xa.yang.provider.NetconfAttribute;
import com.ericsson.oss.mediation.adapter.netconf.jca.xa.yang.provider.YangDataNodeId;
import com.ericsson.oss.mediation.adapter.netconf.jca.xa.yang.provider.YangEditConfigOperation;
import com.ericsson.oss.mediation.adapter.netconf.jca.xa.yang_read.provider.YangGetConfigOperation;
import com.ericsson.oss.mediation.test.BaseFlowTest;

/**
 * This is the base test class for O1 mediation flows. It implements flow unit tests as described at:
 * https://confluence-oss.seli.wh.rnd.internal.ericsson.com/pages/viewpage.action?spaceKey=EPT&title=Unit+Testing+Mediation+Flows
 * This abstract base class should be used to keep verification methods common to multiple tests.
 */
public abstract class O1BaseFlowTest extends BaseFlowTest {
    // Prevents strictness errors from JUnit. These occur when unnecessary mocks are created
    // by the Mediation Flow Test Framework
    @Rule
    public MockitoRule mockito = MockitoJUnit.rule().strictness(Strictness.LENIENT);

    // Load non-flow models
    @Override
    protected Collection<ModelPattern> getAdditionalModelPatterns() {
        final Collection<ModelPattern> modelPatterns = new ArrayList<>();
        modelPatterns.addAll(super.getAdditionalModelPatterns());
        modelPatterns.add(new ModelPattern(".*", "NODE", "O1Node", ".*"));
        modelPatterns.add(new ModelPattern(".*", "urn%3a3gpp%3asa5%3a_3gpp-common-managed-element", ".*", ".*"));
        modelPatterns.add(new ModelPattern(".*", "OSS_TOP", ".*", ".*"));
        return modelPatterns;
    }

    public void verifyHeaderValueExists(Map<String, Object> headers, String headerName, Object expectedValue) {
        if (headerName == null) {
            throw new IllegalArgumentException("Method verifyHeaderValueExists was called with null headerName");
        }
        if (expectedValue == null) {
            throw new IllegalArgumentException("Method verifyHeaderValueExists was called with null expectedValue");
        }
        assertNotNull("Headers are null.", headers);
        Object actualValue = headers.get(headerName);
        assertNotNull(String.format("Headers does not contain entry '%s'", headerName), actualValue);
        assertTrue(String.format("Incorrect header value for '%s'. Expected '%s' but got '%s'", headerName, expectedValue, actualValue),
                actualValue.equals(expectedValue));
    }

    public void verifyHeaderEntryDoesNotExist(Map<String, Object> headers, String headerName) {
        if (headerName == null) {
            throw new IllegalArgumentException("Method verifyHeaderEntryDoesNotExist was called with null headerName");
        }
        assertNotNull("Headers are null.", headers);
        assertFalse(String.format("Header should not contain entry with key '%s'.", headerName), headers.containsKey(headerName));
    }

    public void verifyHeaderWasCreatedByHandler(Map<String, Object> headers, String handlerName, String headerName, Object expectedValue) {
        if (headerName == null) {
            throw new IllegalArgumentException("Method verifyHeaderWasCreatedByHandler was called with null headerName");
        }
        if (expectedValue == null) {
            throw new IllegalArgumentException("Method verifyHeaderWasCreatedByHandler was called with null expectedValue");
        }
        assertNotNull("Headers are null.", headers);
        Object actualValue = headers.get(headerName);
        assertNotNull(String.format("Headers '%s' was not created by handler '%s'.", headerName, handlerName), actualValue);
        assertTrue(String.format("Incorrect header value for '%s' created by handler '%s'. Expected '%s' but got '%s'", headerName, handlerName,
                expectedValue, actualValue), actualValue.equals(expectedValue));
    }

    public void verifySupervisionAttributeExists(Map<String, Object> headers, String attributeName, Object expectedValue) {
        if (attributeName == null) {
            throw new IllegalArgumentException("Method verifySupervisionAttributeExists was called with null attributeName");
        }
        if (expectedValue == null) {
            throw new IllegalArgumentException("Method verifySupervisionAttributeExists was called with null expectedValue");
        }
        Map<String, Object> supervisionAttributes = (Map<String, Object>) headers.get("supervisionAttributes");
        assertNotNull("Header value 'supervisionAttributes' does not exist.", supervisionAttributes);
        assertTrue(String.format("Header 'supervisionAttributes' does not contain attribute '%s'", attributeName),
                supervisionAttributes.containsKey(attributeName));
        Object actualValue = supervisionAttributes.get(attributeName);
        assertTrue(String.format("Attribute '%s' has not been set correctly in header 'supervisionAttributes'. Expected '%s' but got '%s'.",
                attributeName, expectedValue, actualValue), expectedValue.equals(actualValue));
    }

    public void verifyNetconfOperationAttribute(String handlerName, String attributeName, String expectedValue, String actualValue) {
        assertTrue(String.format("Incorrect value set for attribute '%s' in netconf operation for handler '%s'. Expected '%s' but found '%s'.",
                attributeName, handlerName, expectedValue, actualValue),
                expectedValue.equals(actualValue));
    }

    public void verifyMoAttributeExistsInNetconfOperation(String handlerName, List<NetconfAttribute> attributes, String attributeName,
            String attributeValue) {
        Optional<NetconfAttribute> netconfAttribute = attributes.stream()
                .filter(attribute -> attributeName.equals(attribute.getName()))
                .filter(attribute -> attributeValue.equals(attribute.getAttributeInfo().getAttributeValue()))
                .findFirst();

        assertTrue(
                String.format("Did not find MO attribute '%s' with value '%s' in Netconf EDIT operation for handler %s", attributeName,
                        attributeValue,
                        handlerName),
                netconfAttribute.isPresent());
    }

    public void verifyMoAttributeExistsInGetOperation(String handlerName, YangGetConfigOperation operation, String attributeName, String namespace) {
        Optional<YangDataNodeId> yangDataNodeId = operation.getChildMOs().stream()
                .filter(child -> child.getName().equals(attributeName))
                .filter(child -> child.getNamespace().equals(namespace))
                .findFirst();

        assertTrue(
                String.format("Did not find MO attribute '%s' with namespace '%s' in Netconf GET operation for handler %s", attributeName, namespace,
                        handlerName),
                yangDataNodeId.isPresent());
    }

    public List<NetconfAttribute> getEditNetconfAttributes(YangEditConfigOperation yangEditConfigOperation) {
        Optional<List<NetconfAttribute>> attributes = yangEditConfigOperation.getChildMOs().stream()
                .filter(yangDataNodeId -> yangDataNodeId.getName().equals("attributes"))
                .map(yangDataNodeId -> yangDataNodeId.getAttributes())
                .findFirst();
        return attributes.orElse(Collections.emptyList());
    }

    public List<NetconfAttribute> getGetNetconfAttributes(YangGetConfigOperation yangGetConfigOperation) {
        Optional<List<NetconfAttribute>> attributes = yangGetConfigOperation.getChildMOs().stream()
                .filter(yangDataNodeId -> yangDataNodeId.getName().equals("attributes"))
                .map(yangDataNodeId -> yangDataNodeId.getAttributes())
                .findFirst();
        return attributes.orElse(Collections.emptyList());
    }
}
