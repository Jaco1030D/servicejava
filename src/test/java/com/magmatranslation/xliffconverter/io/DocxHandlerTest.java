package com.magmatranslation.xliffconverter.io;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertTrue;
import org.junit.Test;

import com.magmatranslation.xliffconverter.config.AppConfig;
import com.magmatranslation.xliffconverter.config.FileProcessorConfig;

import net.sf.okapi.common.Event;
import net.sf.okapi.filters.openxml.OpenXMLFilter;

public class DocxHandlerTest {
    

    @Test
    public void extractEventsDocxTest() {
        AppConfig config = new AppConfig(new String[]{
            null, null, null, null, null,
        });

        File file = new File(config.getFilePath());

        boolean param = true;

        FileProcessorConfig fileProcessorConfig = new FileProcessorConfig(config, new OpenXMLFilter(), file, param, null);

        DocxHandler docxHandler = new DocxHandler();

        List<Event> events = docxHandler.readDocxFile(fileProcessorConfig);

        boolean containsTextUnits = events.stream().anyMatch(Event::isTextUnit);
        assertTrue("Expected at least one TextUnit in the events", containsTextUnits);
        
    }
}
