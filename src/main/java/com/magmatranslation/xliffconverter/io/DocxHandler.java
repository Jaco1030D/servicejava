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

        try (
            RawDocument rawDocument = new RawDocument(config.file.toURI(), "UTF-8", srcLoc, trgLoc)
            ) {
            config.filter.open(rawDocument);

            if (config.param) {
                configFilter(config.filter, config.filePathParams);
            }

            while (config.filter.hasNext()) {
                Event event = config.filter.next();

                if (event.isTextUnit()) {

                    ITextUnit textUnit = event.getTextUnit();

                    TextContainer sourceContainer = textUnit.getSource();

                    ISegmenter segmenter = getSegmenter(config.filePathSegmentRules);

                    segmenter.computeSegments(sourceContainer);

                    sourceContainer.getSegments().create(segmenter.getRanges());

                    textUnit.setSource(sourceContainer);

                }

                eventList.add(event);
            }

            config.filter.close();
        } catch (Exception e) {
            System.err.println("Erro ao Ler o arquivo docx: " + e.getMessage());
        }

        return eventList;
    }

    private void configFilter(IFilter filter, String filePath) {
        File paramFile = new File(filePath);//mudei isso, caso de algum problema foi aqui

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

    private ISegmenter getSegmenter(String filePath) {
        
        try {
            SRXDocument doc = new SRXDocument();
        
            doc.loadRules(filePath);

            if (doc.getLanguageRules("main") == null || doc.getLanguageRules("main").isEmpty()) {
                System.out.println("Nenhuma regra de segmentação foi carregada.");
                return null;
            }

            ISegmenter segmenter = doc.compileLanguageRules(LocaleId.fromString("pt-BR"), null);


            return segmenter;
        } catch (Exception e) {

            return null;
        }

    }

    public static void saveDocx(List<Event> eventList, FileProcessorConfig config) {
        LocaleId trgLoc = LocaleId.fromString(config.langTarget);
        try (IFilterWriter writer = config.filter.createFilterWriter();) {
            
            writer.setOutput(config.filePathOutput);
            
            writer.setOptions(trgLoc, "UTF-8");
            
            System.out.println("Pelo menos aqui chega");

            for (Event event : eventList) {
                // Recria o evento no filtro
                writer.handleEvent(event);
            }

            writer.close();

        } catch (Exception e) {
            System.err.println("Erro ao salvar o arquivo XLIFF: " + e.getMessage());
        }
    }
}
