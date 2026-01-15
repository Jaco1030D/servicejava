package com.magmatranslation.xliffconverter.io;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.magmatranslation.xliffconverter.config.FileProcessorConfig;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.ISegmenter;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.filters.IFilter;
import net.sf.okapi.common.filterwriter.IFilterWriter;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.common.resource.TextContainer;
import net.sf.okapi.filters.openxml.ConditionalParameters;
import net.sf.okapi.lib.segmentation.SRXDocument;

public class DocxHandler {

    public List<Event> readDocxFile(FileProcessorConfig config) {
        List<Event> eventList = new ArrayList<>();
        LocaleId srcLoc = LocaleId.fromString(config.langSource);
        LocaleId trgLoc = LocaleId.fromString(config.langTarget);

        System.out.println("Tentando ler arquivo: " + config.file.getAbsolutePath());
        System.out.println("Arquivo existe: " + config.file.exists());
        System.out.println("Tamanho do arquivo: " + config.file.length() + " bytes");

        try (
            RawDocument rawDocument = new RawDocument(config.file.toURI(), "UTF-8", srcLoc, trgLoc)
            ) {
            config.filter.open(rawDocument);

            if (config.param && config.filePathParams != null) {
                configFilter(config.filter, config.filePathParams);
            }

            while (config.filter.hasNext()) {
                Event event = config.filter.next();

                if (event.isTextUnit()) {

                    ITextUnit textUnit = event.getTextUnit();

                    TextContainer sourceContainer = textUnit.getSource();

                    if (config.filePathSegmentRules != null) {
                        ISegmenter segmenter = getSegmenter(config.filePathSegmentRules, srcLoc);

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

    private static void configFilter(IFilter filter, String filePath) {
        File paramFile = new File(filePath);
        System.out.println("Caminho do arquivo de parâmetros: " + paramFile.getPath());
        if (!paramFile.exists()) {
            System.out.println("Erro: O arquivo de parâmetros não foi encontrado! Verifique o caminho: " + paramFile.getAbsolutePath());
            return;
        }

        try {
            ConditionalParameters params = new ConditionalParameters();
            
            params.load(paramFile.toURI().toURL(), false);

            filter.setParameters(params);

        } catch (java.net.MalformedURLException e) {
            System.out.println("Erro: URL malformada ao carregar o arquivo de parâmetros: " + e.getMessage());
            
        } catch (Exception e) {
            System.out.println("Erro inesperado ao carregar o arquivo de parâmetros: " + e.getMessage());
            
        }
    }

    private ISegmenter getSegmenter(String filePath, LocaleId locale) {
        
        try {
            File srxFile = new File(filePath);
            if (!srxFile.exists()) {
                System.out.println("Arquivo SRX não encontrado: " + srxFile.getAbsolutePath());
                return null;
            }
            
            SRXDocument doc = new SRXDocument();
        
            doc.loadRules(filePath);

            if (doc.getLanguageRules("main") == null || doc.getLanguageRules("main").isEmpty()) {
                System.out.println("Nenhuma regra de segmentação foi carregada.");
                return null;
            }

            ISegmenter segmenter = doc.compileLanguageRules(locale, null);

            return segmenter;
        } catch (Exception e) {
            System.err.println("Erro ao carregar regras de segmentação: " + e.getMessage());
            return null;
        }

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
                configFilter(config.filter, config.filePathParams);
            }
            
            // Cria o writer após o filtro estar aberto e configurado
            try (IFilterWriter writer = config.filter.createFilterWriter()) {
                
                writer.setOutput(config.filePathOutput);
                
                writer.setOptions(trgLoc, "UTF-8");
                
                System.out.println("Salvando arquivo DOCX em: " + config.filePathOutput);

                // Processa todos os eventos na ordem correta
                for (Event event : eventList) {

                    writer.handleEvent(event);
                    
                }

                writer.close();
            }
            
            config.filter.close();

        } catch (Exception e) {
            System.err.println("Erro ao salvar o arquivo DOCX: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
