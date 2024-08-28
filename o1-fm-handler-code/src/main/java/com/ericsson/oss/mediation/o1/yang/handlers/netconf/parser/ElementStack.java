/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2018
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.oss.mediation.o1.yang.handlers.netconf.parser;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * ElementStack datastructure object.
 *
 * @param <T> Element
 */
public class ElementStack<T> {
    private final ArrayDeque<T> elementCollection;

    public ElementStack() {
        super();
        elementCollection = new ArrayDeque<T>();
    }

    public void push(final T element) {
        elementCollection.addLast(element);
    }

    public T peek() {
        return elementCollection.getLast();
    }

    public T pop() {
        return elementCollection.removeLast();
    }

    public Deque<T> getElementCollection() {
        return elementCollection;
    }

    public boolean isEmpty() {
        return elementCollection.isEmpty();
    }
}
