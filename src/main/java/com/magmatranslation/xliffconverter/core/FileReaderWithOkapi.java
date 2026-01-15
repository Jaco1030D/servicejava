package com.magmatranslation.xliffconverter.core;

import java.util.List;

import com.magmatranslation.xliffconverter.config.FileProcessorConfig;
import com.magmatranslation.xliffconverter.io.DocxHandler;
import com.magmatranslation.xliffconverter.io.XliffHandler;

import net.sf.okapi.common.Event;

public class FileReaderWithOkapi {
    public ExtractionResult extractFileEvents(FileProcessorConfig config) {
        return extractFileEvents(config, true); // Default: redução ativada
    }
    
    public ExtractionResult extractFileEvents(FileProcessorConfig config, boolean reduceFont) {
        System.out.println("O metodo extractFileEvents foi chamado");
        System.out.println(config.typeFile);
        System.out.println("Redução de fonte: " + reduceFont);
        
        List<Event> contentDocx = null;
        String jsonFileName = null;

        switch (config.typeFile) {

            case "DOCX"  -> {

                DocxHandler docxHandler = new DocxHandler();
                
                contentDocx = docxHandler.readDocxFile(config);  

                System.out.println("entrou em docx");
                
            }
            case "XLIFF" -> {
                
                DocxHandler docxHandler = new DocxHandler();

                List<Event> eventsDocx = docxHandler.readDocxFile(config);  
                
                List<Event> eventXLIFF = XliffHandler.XliffReader(config);

                // updateDocxWithXLIFF agora recebe o parâmetro reduceFont
                jsonFileName = XliffConverter.updateDocxWithXLIFF(eventXLIFF, eventsDocx, config.langTarget, reduceFont);

                System.out.println("entrou em xliff");
                System.out.println("Arquivo JSON gerado: " + jsonFileName);
                
                contentDocx = eventsDocx;
            }

        }

        // Retorna o resultado com eventos e nome do arquivo JSON
        return new ExtractionResult(contentDocx, jsonFileName);
    }
}
