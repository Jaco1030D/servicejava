package com.magmatranslation.xliffconverter.io;

import java.util.ArrayList;
import java.util.List;

import com.magmatranslation.xliffconverter.config.FileProcessorConfig;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.ISegmenter;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.common.resource.TextContainer;

//Essa class so aceita a versão mais recente do excel, o xlsx, as outras versões devem ser convertidas para xlsx antes de ser processado.
public class ExcelHandler {

    public List<Event> readExcelFile(FileProcessorConfig config) {
        List<Event> eventList = new ArrayList<>();
        System.out.println("Lendo arquivo Excel: " + config.file.getName());
        LocaleId srcLoc = LocaleId.fromString(config.langSource);
        LocaleId trgLoc = LocaleId.fromString(config.langTarget);

        try (
            RawDocument rawDocument = new RawDocument(config.file.toURI(), "UTF-8", srcLoc, trgLoc)
            ) {

            System.out.println(rawDocument);

            config.filter.open(rawDocument);


            if (config.param && config.filePathParams != null) {
                FilesHandlersUtils.configFilter(config.filter, config.filePathParams);
            }

            ISegmenter segmenter = FilesHandlersUtils.getSegmenter(config.filePathSegmentRules, srcLoc);
            
            System.out.println("Passou do segmenter");
            while (config.filter.hasNext()) {
                System.out.println("Passou do hasNext");

                System.out.println(config.filter.getDisplayName());
                
                Event event = config.filter.next();

                System.out.println(event);
                
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
            
            System.err.println("Erro ao Ler o arquivo excel: " + e.getMessage());
        }

        return eventList;
    }
}
