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

import org.apache.commons.jelly.JellyContext;
import org.apache.commons.jelly.JellyException;
import org.apache.commons.jelly.JellyTagException;
import org.apache.commons.jelly.Script;
import org.apache.commons.jelly.XMLOutput;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * This class acts as a decorator over a org.apache.commons.jelly.Script.
 */
public class ScriptDecorator implements Script, Serializable {

    private static final String JAR_PROTOCOL_PREFIX = "jar:";
    private static final long serialVersionUID = 5193384485429768429L;
    private final String id;
    private final Script delegate;
    private final String scriptURI;
    private final String scriptElement;
    private List<URL> tagLibraryURLs;

    /**
     * Creates a new ScriptDecorator with the given Script delegate and the
     * given Script uri.
     *
     * @param id the id associated with this script.
     * @param delegate the Script delegate.
     * @param scriptURI the Script uri.
     * @param scriptElement the script dom element that represents this script.
     */
    ScriptDecorator(final String id, final Script delegate, final String scriptURI, final String scriptElement) {
        this.id = id;
        this.delegate = delegate;
        this.scriptURI = scriptURI;
        this.scriptElement = scriptElement;
        this.tagLibraryURLs = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    /**
     * Returns the URI (as string) associated with the delegate script.
     *
     * @return the URI (as string) associated with the delegate script.
     */
    public String getScriptURI() {
        return scriptURI;
    }

    /**
     * Returns the DOM element related to this script as a string.
     *
     * @return the DOM element related to this script as a string.
     */
    public String getScriptElement() {
        return scriptElement;
    }

    /**
     * Returns a list of all tag-libraries used by this script.
     *
     * @return a list of all tag-libraries used by this script.
     */
    public List<URL> getTagLibraryURLs() {
        return tagLibraryURLs;
    }

    public void setTagLibraryURLs(List<URL> tagLibraryURLs) {
        this.tagLibraryURLs = tagLibraryURLs;
    }

    /**
     * Returns the URL the jar that contains this script.
     *
     * @return the URL the jar that contains this script.
     */
    URL getScriptJarURL() {
        String value = scriptURI;
        final int index = value.indexOf("!/");
        if (index != -1) {
            value = value.substring(0, index);
        }
        if (value.startsWith(JAR_PROTOCOL_PREFIX)) {
            value = value.substring(JAR_PROTOCOL_PREFIX.length());
        }
        try {
            return new URL(value);
        } catch (MalformedURLException ex) {
            throw new TransformerException(ex);
        }
    }

    /**
     * Returns all jar URLs. The list includes the the jar that contains the
     * scripts and all jars of tag-libraries used.
     *
     * @return all jar URLs.
     */
    URL[] getJarURLs() {
        List<URL> urlList = new ArrayList<>();
        urlList.addAll(tagLibraryURLs);
        urlList.add(getScriptJarURL());
        return urlList.toArray(new URL[urlList.size()]);
    }

    /**
     * Compile the delegate Script.
     *
     * @return the compiled script.
     * @throws JellyException if an exception occur during compilation
     */
    @Override
    public Script compile() throws JellyException {
        return delegate.compile();
    }

    /**
     * Runs the delegate script.
     *
     * @param context the context.
     * @param output the xml output.
     * @throws JellyTagException if an exception occur during script execution.
     */
    @Override
    public void run(final JellyContext context, final XMLOutput output) throws JellyTagException {
        delegate.run(context, output);
    }

}
