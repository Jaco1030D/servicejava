package com.magmatranslation.xliffconverter.core;

import java.util.ArrayList;
import java.util.List;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.ISegmenter;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.common.resource.TextContainer;

import com.magmatranslation.xliffconverter.config.FileProcessorConfig;
import com.magmatranslation.xliffconverter.io.DocxHandler;
import com.magmatranslation.xliffconverter.io.ExcelHandler;
import com.magmatranslation.xliffconverter.io.FilesHandlersUtils;
import com.magmatranslation.xliffconverter.io.PptxHandler;
import com.magmatranslation.xliffconverter.io.XliffHandler;


public class FileReaderWithOkapi {
    public ExtractionResult extractFileEvents(FileProcessorConfig config) {
        return extractFileEvents(config, true); // Default: redução ativada
    }

    private List<Event> readGenericMicrosoftOfficeFile(FileProcessorConfig config) {
        List<Event> eventList = new ArrayList<>();
        LocaleId srcLoc = LocaleId.fromString(config.langSource);
        LocaleId trgLoc = LocaleId.fromString(config.langTarget);

        try (
            RawDocument rawDocument = new RawDocument(config.file.toURI(), "UTF-8", srcLoc, trgLoc)
            ) {

            config.filter.open(rawDocument);

            if (config.param && config.filePathParams != null) {
                FilesHandlersUtils.configFilter(config.filter, config.filePathParams);
            }

            ISegmenter segmenter = FilesHandlersUtils.getSegmenter(config.filePathSegmentRules, srcLoc);
            
            while (config.filter.hasNext()) {
                Event event = config.filter.next();

                if (event.isTextUnit()) {

                    ITextUnit textUnit = event.getTextUnit();

                    TextContainer sourceContainer = textUnit.getSource();

                    if (config.filePathSegmentRules != null) {

                        if (segmenter != null) {
                            segmenter.computeSegments(sourceContainer);

                            sourceContainer.getSegments().create(segmenter.getRanges());

                            textUnit.setSource(sourceContainer);
                        }
                    }

                }

                eventList.add(event);
            }

            config.filter.close();
        } catch (Exception e) {
            System.err.println("Erro ao Ler o arquivo: " + e.getMessage());
        }

        return eventList;
    }
    
    public ExtractionResult extractFileEvents(FileProcessorConfig config, boolean reduceFont) {
        
        List<Event> contentFile = null;
        String jsonFileName = null;

        switch (config.typeFile) {

            case "DOCX"  -> {
                
                // Verifica se o arquivo é DOC e converte para DOCX se necessário
                if (DocxHandler.isDocFile(config.file)) {
                    System.out.println("Arquivo DOC detectado. Convertendo para DOCX...");
                    DocxHandler.convertDocToDocx(config);
                }
                
                contentFile = readGenericMicrosoftOfficeFile(config);  
                
            }
            case "PPTX"  -> {

                // Verifica se o arquivo é PPT e converte para PPTX se necessário
                if (PptxHandler.isPptFile(config.file)) {
                    System.out.println("Arquivo PPT detectado. Convertendo para PPTX...");
                    PptxHandler.convertPptToPptx(config);
                }
                
                contentFile = readGenericMicrosoftOfficeFile(config);  
                
            }
            case "EXCEL" -> {

                if (ExcelHandler.isCsvFile(config.file)) {
                    System.out.println("Arquivo CSV detectado. Convertendo para XLSX...");
                    ExcelHandler.convertCsvToXlsx(config);
                }
                
                // Verifica se o arquivo é XLS e converte para XLSX se necessário
                if (ExcelHandler.isXlsFile(config.file)) {
                    System.out.println("Arquivo XLS detectado. Convertendo para XLSX...");
                    ExcelHandler.convertXlsToXlsx(config);
                }
                
                contentFile = readGenericMicrosoftOfficeFile(config);  
            
            }
            
            case "XLIFF" -> {
                //usar o caso generico parece funcionar, tenho que testar mais
                List<Event> eventsFile = readGenericMicrosoftOfficeFile(config);  
                
                List<Event> eventXLIFF = XliffHandler.XliffReader(config);

                jsonFileName = XliffConverter.updateDocxWithXLIFF(eventXLIFF, eventsFile, config.langTarget, reduceFont);

                System.out.println("entrou em xliff");
                System.out.println("Arquivo JSON gerado: " + jsonFileName);
                
                contentFile = eventsFile;
            }

        }

        // Retorna o resultado com eventos e nome do arquivo JSON
        return new ExtractionResult(contentFile, jsonFileName);
    }
}
