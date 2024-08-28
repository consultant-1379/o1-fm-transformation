/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ericsson.oss.mediation.fm.o1.engine.transform.tag;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.jelly.JellyTagException;
import org.apache.commons.jelly.TagSupport;
import org.apache.commons.jelly.XMLOutput;

@Slf4j
public class ModelTransformerTag extends TagSupport {

    private String id;
    private String description;

    @Override
    public void doTag(final XMLOutput xmlo) throws JellyTagException {
        log.debug("Executing script with id: " + id + ". Description: " + description);
        invokeBody(xmlo);
        log.debug("Script successfully executed. Id: " + id + ", description: " + description);
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }
}
