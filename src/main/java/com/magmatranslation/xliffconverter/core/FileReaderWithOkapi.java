package com.magmatranslation.xliffconverter.core;

import java.util.List;

import com.magmatranslation.xliffconverter.config.FileProcessorConfig;
import com.magmatranslation.xliffconverter.io.DocxHandler;
import com.magmatranslation.xliffconverter.io.XliffHandler;

import net.sf.okapi.common.Event;

public class FileReaderWithOkapi {
    public List<Event> extractFileEvents(FileProcessorConfig config) {
        System.out.println("O metodo extractFileEvents foi chamado");
        System.out.println(config.typeFile);
        List<Event> contentDocx = null;

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

                XliffConverter.updateDocxWithXLIFF(eventXLIFF, eventsDocx, config.langTarget);

                System.out.println("entrou em xliff");
                
                contentDocx = eventsDocx;
            }

        }


        return contentDocx;
    }
}
