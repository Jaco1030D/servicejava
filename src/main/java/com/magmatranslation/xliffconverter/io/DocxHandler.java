package com.magmatranslation.xliffconverter.io;

import java.util.ArrayList;
import java.util.List;

import com.magmatranslation.xliffconverter.config.FileProcessorConfig;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.ISegmenter;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.filterwriter.IFilterWriter;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.common.resource.TextContainer;

public class DocxHandler {

    public List<Event> readDocxFile(FileProcessorConfig config) {
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
            System.err.println("Erro ao Ler o arquivo docx: " + e.getMessage());
        }

        return eventList;
    }

    public static void saveDocx(List<Event> eventList, FileProcessorConfig config) {
        LocaleId srcLoc = LocaleId.fromString(config.langSource);
        LocaleId trgLoc = LocaleId.fromString(config.langTarget);
        
        if (config.file == null) {
            System.err.println("Erro: Arquivo DOCX original não foi definido para salvar");
            return;
        }

        try (
            RawDocument rawDocument = new RawDocument(config.file.toURI(), "UTF-8", srcLoc, trgLoc)
        ) {
            // Abre o filtro com o documento original para preservar a estrutura
            config.filter.open(rawDocument);
            
            // Aplica os parâmetros do filtro se necessário
            if (config.param && config.filePathParams != null) {
                
                FilesHandlersUtils.configFilter(config.filter, config.filePathParams);
            
            }
            
            // Cria o writer após o filtro estar aberto e configurado
            try (IFilterWriter writer = config.filter.createFilterWriter()) {
                
                writer.setOutput(config.filePathOutput);
                
                writer.setOptions(trgLoc, "UTF-8");

                // Processa todos os eventos na ordem correta
                for (Event event : eventList) {

                    writer.handleEvent(event);
                    
                }

                writer.close();
            }
            
            config.filter.close();

        } catch (Exception e) {
            System.err.println("Erro ao salvar o arquivo: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
