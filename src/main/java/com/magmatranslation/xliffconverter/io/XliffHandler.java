package com.magmatranslation.xliffconverter.io;
import java.util.ArrayList;
import java.util.List;

import com.magmatranslation.xliffconverter.config.FileProcessorConfig;
import com.magmatranslation.xliffconverter.core.Base64Handler;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.filterwriter.XLIFFWriter;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.filters.xliff.XLIFFFilter;

public class XliffHandler {

    public void createXLIFF(FileProcessorConfig config, List<Event> eventList) {
        LocaleId srcLoc = LocaleId.fromString(config.langSource);
        LocaleId trgLoc = LocaleId.fromString(config.langTarget);

        
        try (XLIFFWriter writer = new XLIFFWriter()) {
            writer.create(config.filePathOutput + "\\XLIFF\\" + config.file.getName() + ".xlf", null, srcLoc, trgLoc, null, "word/document.xml", null);
            
            String fileBase64 = Base64Handler.createBase64(config.file);
            writer.writeStartFile(config.file.getName(), "x-docx", null, "<reference><internal-file form=\"base64\"> " + fileBase64 + " </internal-file></reference>");

            writer.writeEndFile();
            
            for (Event event : eventList) {

                if (event.isTextUnit()) {

                    ITextUnit textUnit = event.getTextUnit();

                    writer.writeTextUnit(textUnit);

                }
            }

            writer.close();
        } catch (Exception e) {
            System.err.println("Erro ao criar o arquivo XLIFF: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public static List<Event> XliffReader(FileProcessorConfig config) {
        LocaleId srcLoc = LocaleId.fromString(config.langSource);

        LocaleId trgLoc = LocaleId.fromString(config.langTarget);
        
        List<Event> eventList = new ArrayList<>();

        try (   
            XLIFFFilter filter = new XLIFFFilter();
            RawDocument rawDocument = new RawDocument(config.fileXLIFF.toURI(), "UTF-8", srcLoc, trgLoc)
            ) {
    
            filter.open(rawDocument);
            

            while (filter.hasNext()) {

                Event event = filter.next();

                if (!event.isTextUnit()) continue;

                ITextUnit textUnit = event.getTextUnit();

                if (textUnit.getTarget(trgLoc) != null) {
                
                    eventList.add(event);
                
                }

            
            }

            filter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return eventList;
    }
}
