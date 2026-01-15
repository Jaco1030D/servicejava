package com.magmatranslation.xliffconverter.core;

import java.util.List;

import net.sf.okapi.common.Event;

/**
 * Classe wrapper para retornar os eventos extraídos e o nome do arquivo JSON gerado.
 */
public class ExtractionResult {
    
    private List<Event> events;
    private String jsonFileName;
    
    /**
     * Construtor.
     * 
     * @param events Lista de eventos extraídos
     * @param jsonFileName Nome do arquivo JSON gerado
     */
    public ExtractionResult(List<Event> events, String jsonFileName) {
        this.events = events;
        this.jsonFileName = jsonFileName;
    }
    
    /**
     * Retorna a lista de eventos.
     * 
     * @return Lista de eventos
     */
    public List<Event> getEvents() {
        return events;
    }
    
    /**
     * Retorna o nome do arquivo JSON.
     * 
     * @return Nome do arquivo JSON
     */
    public String getJsonFileName() {
        return jsonFileName;
    }
}

