package com.magmatranslation.xliffconverter.core;

import java.util.List;

import net.sf.okapi.common.Event;

public class ExtractionResult {
    
    private List<Event> events;
    private String jsonFileName;

    public ExtractionResult(List<Event> events, String jsonFileName) {
        this.events = events;
        this.jsonFileName = jsonFileName;
    }

    public List<Event> getEvents() {
        return events;
    }

    public String getJsonFileName() {
        return jsonFileName;
    }
}

